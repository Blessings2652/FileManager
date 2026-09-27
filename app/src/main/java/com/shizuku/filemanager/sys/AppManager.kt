package com.shizuku.filemanager.sys

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import android.content.pm.PermissionInfo
import android.content.pm.Signature
import android.os.Build
import android.os.storage.StorageManager
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class AppInfo(
    val label: String,
    val packageName: String,
    val dataDir: String,
    val sourceDir: String,
    val isSystemApp: Boolean,
    val version: String,
    val installTime: Long,
    val category: String = "Unknown"
)

data class CertificateInfo(
    val subject: String,
    val issuer: String,
    val validFrom: String,
    val validUntil: String,
    val serialNumber: String,
    val fingerprintSha256: String
)

data class AppDetails(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val uid: Int,
    val processName: String,
    val dataDir: String,
    val sourceDir: String,
    val nativeLibraryDir: String,
    val installTime: Long,
    val updateTime: Long,
    val certificates: List<CertificateInfo>,
    val usedPermissions: List<String>,
    val dangerousPermissions: List<String>,
    val definedPermissions: List<String>,
    val activities: List<String>,
    val services: List<String>,
    val providers: List<String>,
    val receivers: List<String>,
    val features: List<String>,
    val isDebuggable: Boolean,
    val allowBackup: Boolean,
    val category: String
)

data class UnusedApp(
    val appInfo: AppInfo,
    val lastUsed: Long,
    val daysSinceLastUse: Int
)

data class AppStorageItem(
    val appInfo: AppInfo,
    val codeBytes: Long,
    val dataBytes: Long,
    val cacheBytes: Long,
    val totalBytes: Long,
    val totalSizeFormatted: String
)

object AppManager {
    suspend fun getAppsWithStorageStats(context: Context): List<AppStorageItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = try { pm.getInstalledApplications(PackageManager.GET_META_DATA) } catch (_: Exception) { emptyList() }
        val storageStatsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        } else null
        val defaultUuid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) StorageManager.UUID_DEFAULT else null

        apps.map { app ->
            val label = try { app.loadLabel(pm).toString() } catch (_: Exception) { app.packageName }
            val category = getCategoryName(app)
            val pkgInfo = try { pm.getPackageInfo(app.packageName, 0) } catch (_: Exception) { null }

            val appInfo = AppInfo(
                label = label,
                packageName = app.packageName,
                dataDir = app.dataDir ?: "",
                sourceDir = app.sourceDir ?: "",
                isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                version = pkgInfo?.versionName ?: "1.0",
                installTime = pkgInfo?.firstInstallTime ?: 0L,
                category = category
            )

            var codeSize = 0L
            var dataSize = 0L
            var cacheSize = 0L

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && storageStatsManager != null && defaultUuid != null) {
                try {
                    val stats = storageStatsManager.queryStatsForUid(defaultUuid, app.uid)
                    codeSize = stats.appBytes
                    dataSize = stats.dataBytes
                    cacheSize = stats.cacheBytes
                } catch (_: Exception) {
                    codeSize = File(app.sourceDir ?: "").length().coerceAtLeast(0L)
                    dataSize = File(app.dataDir ?: "").length().coerceAtLeast(0L)
                }
            } else {
                codeSize = File(app.sourceDir ?: "").length().coerceAtLeast(0L)
                dataSize = File(app.dataDir ?: "").length().coerceAtLeast(0L)
            }

            val total = codeSize + dataSize + cacheSize
            AppStorageItem(
                appInfo = appInfo,
                codeBytes = codeSize,
                dataBytes = dataSize,
                cacheBytes = cacheSize,
                totalBytes = total,
                totalSizeFormatted = StorageScanner.formatSize(total)
            )
        }.sortedByDescending { it.totalBytes }
    }
    suspend fun getInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = try { pm.getInstalledApplications(PackageManager.GET_META_DATA) } catch (_: Exception) { emptyList() }
        apps.map { app ->
            val pkgInfo = try { pm.getPackageInfo(app.packageName, 0) } catch (e: Exception) { null }
            val label = try { app.loadLabel(pm).toString() } catch (e: Exception) { app.packageName }
            val category = getCategoryName(app)
            AppInfo(
                label = label,
                packageName = app.packageName,
                dataDir = app.dataDir ?: "",
                sourceDir = app.sourceDir ?: "",
                isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                version = pkgInfo?.versionName ?: "Unknown",
                installTime = pkgInfo?.firstInstallTime ?: 0L,
                category = category
            )
        }.sortedBy { it.label.lowercase() }
    }

    private fun getCategoryName(app: ApplicationInfo): String {
        return if (Build.VERSION.SDK_INT >= 26) {
            when (app.category) {
                ApplicationInfo.CATEGORY_GAME -> "Games"
                ApplicationInfo.CATEGORY_AUDIO -> "Audio"
                ApplicationInfo.CATEGORY_VIDEO -> "Video"
                ApplicationInfo.CATEGORY_IMAGE -> "Image"
                ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                ApplicationInfo.CATEGORY_NEWS -> "News"
                ApplicationInfo.CATEGORY_MAPS -> "Maps"
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Tools"
                ApplicationInfo.CATEGORY_ACCESSIBILITY -> "Tools"
                else -> "Tools" // Defaulting to Tools as requested
            }
        } else {
            "Tools"
        }
    }

    suspend fun getAppDetails(context: Context, packageName: String): AppDetails = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val flags = PackageManager.GET_PERMISSIONS or 
                    PackageManager.GET_SERVICES or 
                    PackageManager.GET_RECEIVERS or 
                    PackageManager.GET_ACTIVITIES or 
                    PackageManager.GET_PROVIDERS or
                    PackageManager.GET_CONFIGURATIONS or
                    if (Build.VERSION.SDK_INT >= 28) {
                        PackageManager.GET_SIGNING_CERTIFICATES
                    } else {
                        @Suppress("DEPRECATION")
                        PackageManager.GET_SIGNATURES
                    }
        
        val pkgInfo = pm.getPackageInfo(packageName, flags)
        val appInfo = pkgInfo.applicationInfo ?: throw Exception("ApplicationInfo not found")
        
        val allPerms = pkgInfo.requestedPermissions?.toList() ?: emptyList()
        val dangerous = allPerms.filter { perm ->
            try {
                val info = pm.getPermissionInfo(perm, 0)
                val protection = if (android.os.Build.VERSION.SDK_INT >= 28) {
                    info.protection
                } else {
                    @Suppress("DEPRECATION")
                    info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
                }
                protection == PermissionInfo.PROTECTION_DANGEROUS
            } catch (e: Exception) { false }
        }

        val definedPermissions = pkgInfo.permissions?.map { it.name } ?: emptyList()
        val activities = pkgInfo.activities?.map { it.name } ?: emptyList()
        val services = pkgInfo.services?.map { it.name } ?: emptyList()
        val receivers = pkgInfo.receivers?.map { it.name } ?: emptyList()
        val providers = pkgInfo.providers?.map { it.name } ?: emptyList()
        val features = pkgInfo.reqFeatures?.mapNotNull { it.name ?: "GlEsVersion: ${it.glEsVersion}" } ?: emptyList()

        val certificates = mutableListOf<CertificateInfo>()
        val cf = java.security.cert.CertificateFactory.getInstance("X.509")
        
        fun parseSignature(sig: Signature) {
            try {
                val cert = cf.generateCertificate(java.io.ByteArrayInputStream(sig.toByteArray())) as java.security.cert.X509Certificate
                val sha256 = java.security.MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
                    .joinToString(":") { "%02X".format(it) }
                
                certificates.add(CertificateInfo(
                    subject = cert.subjectDN.name,
                    issuer = cert.issuerDN.name,
                    validFrom = cert.notBefore.toString(),
                    validUntil = cert.notAfter.toString(),
                    serialNumber = cert.serialNumber.toString(16).uppercase(),
                    fingerprintSha256 = sha256
                ))
            } catch (e: Exception) {
                // Skip invalid certs
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= 28) {
            val signingInfo = pkgInfo.signingInfo
            if (signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners.forEach { parseSignature(it) }
                } else {
                    signingInfo.signingCertificateHistory.forEach { parseSignature(it) }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.signatures?.forEach { parseSignature(it) }
        }

        AppDetails(
            packageName = packageName,
            versionName = pkgInfo.versionName ?: "Unknown",
            versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo),
            minSdk = if (android.os.Build.VERSION.SDK_INT >= 24) appInfo.minSdkVersion else 0,
            targetSdk = appInfo.targetSdkVersion,
            uid = appInfo.uid,
            processName = appInfo.processName,
            dataDir = appInfo.dataDir,
            sourceDir = appInfo.sourceDir,
            nativeLibraryDir = appInfo.nativeLibraryDir,
            installTime = pkgInfo.firstInstallTime,
            updateTime = pkgInfo.lastUpdateTime,
            certificates = certificates,
            usedPermissions = allPerms,
            dangerousPermissions = dangerous,
            definedPermissions = definedPermissions,
            activities = activities,
            services = services,
            providers = providers,
            receivers = receivers,
            features = features,
            isDebuggable = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
            allowBackup = (appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0,
            category = getCategoryName(appInfo)
        )
    }

    fun getInternalDataPath(packageName: String): String {
        return "/data/data/$packageName"
    }

    fun getExternalDataPath(context: Context, packageName: String): String {
        val dir = context.getExternalFilesDir(null)
        return if (dir != null) {
            dir.parentFile?.parentFile?.absolutePath + "/$packageName"
        } else {
            android.os.Environment.getExternalStorageDirectory().absolutePath + "/Android/data/$packageName"
        }
    }

    suspend fun extractApk(context: Context, app: AppInfo, destDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!destDir.exists()) destDir.mkdirs()
            val destFile = File(destDir, "${app.label}_${app.version}.apk")
            FileInputStream(File(app.sourceDir)).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Result.success(destFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAppIcon(context: Context, packageName: String): Drawable? = withContext(Dispatchers.IO) {
        try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    suspend fun getApkIcon(context: Context, apkPath: String): Drawable? = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(apkPath, 0)
            info?.applicationInfo?.let { appInfo ->
                appInfo.sourceDir = apkPath
                appInfo.publicSourceDir = apkPath
                appInfo.loadIcon(pm)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUnusedApps(context: Context, days: Int = 30): List<UnusedApp> = withContext(Dispatchers.IO) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val endTime = System.currentTimeMillis()
        
        // Query for a longer range to get actual last used times.
        // Even if we filter for 'days', we want the accurate count for those that are unused.
        val queryStartTime = endTime - (365L * 24 * 60 * 60 * 1000) // 1 year lookback

        val statsMap = try {
            usageStatsManager.queryAndAggregateUsageStats(queryStartTime, endTime)
        } catch (e: Exception) {
            emptyMap()
        }

        val apps = getInstalledApps(context)
        apps.filter { !it.isSystemApp }.map { app ->
            val stats = statsMap[app.packageName]
            val lastUsed = stats?.lastTimeUsed ?: 0L
            
            // If never used in recorded history, use install time as baseline
            val effectiveLastUsed = if (lastUsed > 0) lastUsed else app.installTime
            val daysUnused = if (effectiveLastUsed > 0) {
                (maxOf(0L, endTime - effectiveLastUsed) / (1000 * 60 * 60 * 24)).toInt()
            } else {
                0
            }
            
            UnusedApp(app, lastUsed, daysUnused)
        }.filter { it.daysSinceLastUse >= days }
        .sortedByDescending { it.daysSinceLastUse }
    }

    fun uninstallApp(context: Context, packageName: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_DELETE).apply {
            data = android.net.Uri.fromParts("package", packageName, null)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun getUninstallIntent(packageName: String): android.content.Intent {
        return android.content.Intent(android.content.Intent.ACTION_DELETE).apply {
            data = android.net.Uri.fromParts("package", packageName, null)
        }
    }
}
