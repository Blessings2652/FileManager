package com.shizuku.filemanager.sys

import android.content.Context
import android.os.Build
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.Locale
import kotlin.io.OnErrorAction

data class LargeFile(
    val file: File,
    val sizeFormatted: String,
    val sizeBytes: Long
)

data class DuplicateGroup(
    val hash: String,
    val files: List<File>,
    val totalSizeFormatted: String,
    val totalSizeBytes: Long
)

data class StorageCategory(
    val name: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val fileCount: Int,
    val icon: String // Icon name for UI
)

data class StorageStats(
    val usedSize: Long,
    val totalSize: Long,
    val usedSizeFormatted: String,
    val totalSizeFormatted: String,
    val categories: List<StorageCategory>
)

data class SystemPartitionInfo(
    val name: String,
    val path: String,
    val totalBytes: Long,
    val usedBytes: Long,
    val totalFormatted: String,
    val usedFormatted: String,
    val description: String,
    val exists: Boolean
)

data class SystemOsDetails(
    val androidVersion: String,
    val sdkInt: Int,
    val buildId: String,
    val deviceModel: String,
    val securityPatch: String,
    val partitions: List<SystemPartitionInfo>
)

object StorageScanner {

    private val imageExts = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "svg", "raw", "cr2", "nef")
    private val videoExts = setOf("mp4", "mkv", "webm", "avi", "3gp", "mov", "flv", "wmv", "m4v", "mpg", "mpeg")
    private val audioExts = setOf("mp3", "wav", "flac", "m4a", "ogg", "aac", "opus", "wma", "mid", "midi", "amr", "awb", "aiff", "aif", "ape", "alac", "mka", "ac3", "dts", "caf", "3ga", "mp2", "mp1", "m4b")
    private val docExts   = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "epub", "rtf", "json", "xml", "html", "md", "log")
    private val apkExts   = setOf("apk", "xapk", "apks")
    private val archiveExts = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "iso", "img", "xz")

    fun getRealSystemOsDetails(): SystemOsDetails {
        val androidVersion = Build.VERSION.RELEASE ?: "Unknown"
        val sdkInt = Build.VERSION.SDK_INT
        val buildId = Build.DISPLAY ?: Build.ID ?: "Unknown"
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        val deviceModel = "$manufacturer ${Build.MODEL}"
        val securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A"

        val candidatePartitions = listOf(
            Triple("/system", "Core OS & Framework", "Android System Runtime & Framework"),
            Triple("/vendor", "Hardware Drivers & Vendor", "HALs & Hardware Device Firmware"),
            Triple("/apex", "APEX System Modules", "Updatable Core System Modules"),
            Triple("/product", "Product Overlays", "OEM & Product Framework Overlays"),
            Triple("/system_ext", "System Extensions", "Extended System Services & Libraries")
        )

        val partitions = mutableListOf<SystemPartitionInfo>()

        candidatePartitions.forEach { (pathStr, name, desc) ->
            val file = File(pathStr)
            if (file.exists()) {
                try {
                    val stat = StatFs(pathStr)
                    val total = stat.totalBytes
                    val free = stat.availableBytes
                    val used = (total - free).coerceAtLeast(0L)
                    if (total > 0) {
                        partitions.add(
                            SystemPartitionInfo(
                                name = name,
                                path = pathStr,
                                totalBytes = total,
                                usedBytes = used,
                                totalFormatted = formatSize(total),
                                usedFormatted = formatSize(used),
                                description = desc,
                                exists = true
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
        }

        return SystemOsDetails(
            androidVersion = androidVersion,
            sdkInt = sdkInt,
            buildId = buildId,
            deviceModel = deviceModel,
            securityPatch = securityPatch,
            partitions = partitions
        )
    }

    suspend fun getStorageStats(root: File, context: Context? = null): StorageStats = withContext(Dispatchers.IO) {
        var imagesSize = 0L
        var videosSize = 0L
        var audioSize = 0L
        var docsSize = 0L
        var apksSize = 0L
        var archivesSize = 0L
        var othersSize = 0L

        var imagesCount = 0
        var videosCount = 0
        var audioCount = 0
        var docsCount = 0
        var apksCount = 0
        var archivesCount = 0
        var othersCount = 0

        try {
            root.walkTopDown()
                .onFail { _, _ -> OnErrorAction.SKIP }
                .filter { it.isFile }
                .forEach { file ->
                    val size = file.length()
                    val ext = file.extension.lowercase(Locale.ROOT)
                    when {
                        ext in imageExts -> {
                            imagesSize += size
                            imagesCount++
                        }
                        ext in videoExts -> {
                            videosSize += size
                            videosCount++
                        }
                        ext in audioExts -> {
                            audioSize += size
                            audioCount++
                        }
                        ext in docExts -> {
                            docsSize += size
                            docsCount++
                        }
                        ext in apkExts -> {
                            apksSize += size
                            apksCount++
                        }
                        ext in archiveExts -> {
                            archivesSize += size
                            archivesCount++
                        }
                        else -> {
                            othersSize += size
                            othersCount++
                        }
                    }
                }
        } catch (_: Exception) {}

        val totalCapacity = root.totalSpace.coerceAtLeast(0L)
        val freeCapacity = root.freeSpace.coerceAtLeast(0L)
        val realUsedSpace = (totalCapacity - freeCapacity).coerceAtLeast(0L)

        val scannedUserFilesTotal = imagesSize + videosSize + audioSize + docsSize + apksSize + archivesSize + othersSize
        val totalSystemAndAppsSpace = (realUsedSpace - scannedUserFilesTotal).coerceAtLeast(0L)

        var totalAppsSize = 0L
        var installedAppsCount = 0

        if (context != null) {
            try {
                val appItems = AppManager.getAppsWithStorageStats(context)
                installedAppsCount = appItems.size
                totalAppsSize = appItems.sumOf { it.totalBytes }
            } catch (_: Exception) {}
        }

        val finalAppsSize = if (totalAppsSize > 0) totalAppsSize.coerceAtMost(totalSystemAndAppsSpace) else (totalSystemAndAppsSpace * 0.6).toLong()
        val systemOsSize = (totalSystemAndAppsSpace - finalAppsSize).coerceAtLeast(0L)

        val categories = mutableListOf(
            StorageCategory("Images", imagesSize, formatSize(imagesSize), imagesCount, "image"),
            StorageCategory("Videos", videosSize, formatSize(videosSize), videosCount, "video"),
            StorageCategory("Audio", audioSize, formatSize(audioSize), audioCount, "audio"),
            StorageCategory("Documents", docsSize, formatSize(docsSize), docsCount, "description"),
            StorageCategory("APKs & Tools", apksSize, formatSize(apksSize), apksCount, "tool"),
            StorageCategory("Archives", archivesSize, formatSize(archivesSize), archivesCount, "folder_zip"),
            StorageCategory("Other Files", othersSize, formatSize(othersSize), othersCount, "insert_drive_file")
        )

        if (finalAppsSize > 0 || installedAppsCount > 0) {
            categories.add(
                StorageCategory("Installed Apps", finalAppsSize, formatSize(finalAppsSize), installedAppsCount, "android")
            )
        }

        if (systemOsSize > 0) {
            categories.add(
                StorageCategory("System OS", systemOsSize, formatSize(systemOsSize), 1, "settings")
            )
        }

        val displayUsed = if (realUsedSpace > 0) realUsedSpace else scannedUserFilesTotal

        StorageStats(
            usedSize = displayUsed,
            totalSize = totalCapacity,
            usedSizeFormatted = formatSize(displayUsed),
            totalSizeFormatted = formatSize(totalCapacity),
            categories = categories.sortedByDescending { it.sizeBytes }
        )
    }

    suspend fun getCategoryFiles(root: File, categoryName: String, context: Context? = null, limit: Int = 150): List<LargeFile> = withContext(Dispatchers.IO) {
        val matchedFiles = mutableListOf<File>()
        val catLower = categoryName.lowercase(Locale.ROOT)

        if ((catLower.contains("app") || catLower.contains("system")) && context != null) {
            try {
                val pm = context.packageManager
                val installedApps = pm.getInstalledApplications(0)
                installedApps.forEach { app ->
                    val sourceDir = app.sourceDir
                    if (sourceDir != null) {
                        val file = File(sourceDir)
                        if (file.exists() && file.length() > 0) {
                            matchedFiles.add(file)
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        try {
            root.walkTopDown()
                .onFail { _, _ -> OnErrorAction.SKIP }
                .filter { it.isFile }
                .forEach { file ->
                    val ext = file.extension.lowercase(Locale.ROOT)
                    val matches = when {
                        catLower.contains("image") -> ext in imageExts
                        catLower.contains("video") -> ext in videoExts
                        catLower.contains("audio") -> ext in audioExts
                        catLower.contains("doc") -> ext in docExts
                        catLower.contains("apk") || catLower.contains("tool") -> ext in apkExts
                        catLower.contains("archive") || catLower.contains("zip") -> ext in archiveExts
                        catLower.contains("app") || catLower.contains("system") -> file.path.contains("/Android/", ignoreCase = true) || ext == "obb"
                        catLower.contains("other") -> ext !in imageExts && ext !in videoExts && ext !in audioExts && ext !in docExts && ext !in apkExts && ext !in archiveExts
                        else -> true
                    }
                    if (matches) {
                        matchedFiles.add(file)
                    }
                }
        } catch (_: Exception) {}

        matchedFiles.distinctBy { it.absolutePath }
            .sortedByDescending { it.length() }
            .take(limit)
            .map { LargeFile(it, formatSize(it.length()), it.length()) }
    }

    suspend fun getOldFiles(root: File, months: Int = 6, limit: Int = 100): List<LargeFile> = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - (months.toLong() * 30 * 24 * 60 * 60 * 1000)
        val matchedFiles = mutableListOf<File>()
        try {
            root.walkTopDown()
                .onFail { _, _ -> OnErrorAction.SKIP }
                .filter { it.isFile && it.lastModified() < cutoff && it.extension.lowercase(Locale.ROOT) != "obb" }
                .forEach { matchedFiles.add(it) }
        } catch (_: Exception) {}

        matchedFiles.sortedByDescending { it.length() }
            .take(limit)
            .map { LargeFile(it, formatSize(it.length()), it.length()) }
    }

    suspend fun getLargeFiles(root: File, limit: Int = 50): List<LargeFile> = withContext(Dispatchers.IO) {
        val matchedFiles = mutableListOf<File>()
        try {
            root.walkTopDown()
                .onFail { _, _ -> OnErrorAction.SKIP }
                .filter { it.isFile && it.length() >= 20 * 1024 * 1024L && it.extension.lowercase(Locale.ROOT) != "obb" }
                .forEach { matchedFiles.add(it) }
        } catch (_: Exception) {}

        matchedFiles.sortedByDescending { it.length() }
            .take(limit)
            .map { LargeFile(it, formatSize(it.length()), it.length()) }
    }

    suspend fun getDuplicateFiles(root: File): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val sizeMap = mutableMapOf<Long, MutableList<File>>()

        try {
            root.walkTopDown()
                .onFail { _, _ -> OnErrorAction.SKIP }
                .filter { it.isFile && it.extension.lowercase(Locale.ROOT) != "obb" }
                .forEach { file ->
                    val size = file.length()
                    if (size > 1024) { // Ignore very small files
                        sizeMap.getOrPut(size) { mutableListOf() }.add(file)
                    }
                }
        } catch (_: Exception) {}

        val duplicates = mutableListOf<DuplicateGroup>()

        sizeMap.filter { it.value.size > 1 }.forEach { (size, files) ->
            val hashMap = mutableMapOf<String, MutableList<File>>()
            files.forEach { file ->
                val hash = try { calculateMD5(file) } catch (e: Exception) { null }
                if (hash != null) {
                    hashMap.getOrPut(hash) { mutableListOf() }.add(file)
                }
            }

            hashMap.filter { it.value.size > 1 }.forEach { (hash, dups) ->
                val wasteSize = size * (dups.size - 1)
                duplicates.add(DuplicateGroup(hash, dups, formatSize(wasteSize), wasteSize))
            }
        }

        duplicates.sortedByDescending { it.totalSizeBytes }
    }

    private fun calculateMD5(file: File): String {
        if (!file.canRead()) return ""
        val size = file.length()
        if (size > 1024 * 1024) {
            try {
                val digest = MessageDigest.getInstance("MD5")
                digest.update(size.toString().toByteArray())
                file.inputStream().use { fis ->
                    val buffer = ByteArray(64 * 1024)
                    val read = fis.read(buffer)
                    if (read > 0) digest.update(buffer, 0, read)
                }
                RandomAccessFile(file, "r").use { raf ->
                    raf.seek(size - 64 * 1024)
                    val buffer = ByteArray(64 * 1024)
                    val read = raf.read(buffer)
                    if (read > 0) digest.update(buffer, 0, read)
                }
                return digest.digest().joinToString("") { "%02x".format(it) }
            } catch (_: Exception) {}
        }

        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { fis ->
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceAtMost(units.size - 1)
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
