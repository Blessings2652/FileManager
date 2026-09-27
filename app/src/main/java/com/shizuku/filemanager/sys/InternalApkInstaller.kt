package com.shizuku.filemanager.sys

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.IntentCompat
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicInteger

sealed class InstallStatus {
    object Idle : InstallStatus()
    object Initializing : InstallStatus()
    data class Installing(val progress: Float) : InstallStatus()
    object Success : InstallStatus()
    data class Failure(val message: String) : InstallStatus()
}

object InternalApkInstaller {
    val status = MutableStateFlow<InstallStatus>(InstallStatus.Idle)
    private val activeSessionId = AtomicInteger(-1)
    private var isCallbackRegistered = false

    private val sessionCallback = object : PackageInstaller.SessionCallback() {
        override fun onCreated(sessionId: Int) {}
        override fun onBadgingChanged(sessionId: Int) {}
        override fun onActiveChanged(sessionId: Int, active: Boolean) {}
        override fun onProgressChanged(sessionId: Int, progress: Float) {
            if (sessionId == activeSessionId.get()) {
                val current = status.value
                if (current is InstallStatus.Installing) {
                    // Map system progress (0.0 to 1.0) to the final phase (0.8 to 0.99)
                    val weightedProgress = 0.8f + (progress * 0.19f)
                    if (weightedProgress > current.progress) {
                        status.value = InstallStatus.Installing(weightedProgress)
                    }
                }
            }
        }
        override fun onFinished(sessionId: Int, success: Boolean) {}
    }

    private fun ensureCallbackRegistered(context: Context) {
        if (!isCallbackRegistered) {
            try {
                val packageInstaller = context.packageManager.packageInstaller
                packageInstaller.registerSessionCallback(sessionCallback, Handler(Looper.getMainLooper()))
                isCallbackRegistered = true
            } catch (_: Exception) {
                // Ignore registration errors
            }
        }
    }

    fun reset() {
        status.value = InstallStatus.Idle
        activeSessionId.set(-1)
    }

    internal fun clearActiveSession(sessionId: Int) {
        if (activeSessionId.get() == sessionId) {
            activeSessionId.set(-1)
        }
    }

    fun install(context: Context, apkFile: File) {
        status.value = InstallStatus.Initializing
        
        try {
            ensureCallbackRegistered(context)
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            
            // Try to get label from APK
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            info?.applicationInfo?.let { appInfo ->
                appInfo.sourceDir = apkFile.absolutePath
                appInfo.publicSourceDir = apkFile.absolutePath
                params.setAppLabel(appInfo.loadLabel(pm))
            }

            val sessionId = packageInstaller.createSession(params)
            activeSessionId.set(sessionId)
            val session = packageInstaller.openSession(sessionId)

            val out = session.openWrite("package", 0, apkFile.length())
            val input = FileInputStream(apkFile)
            val buffer = ByteArray(65536)
            var totalWritten = 0L
            
            input.use { fis ->
                out.use { fos ->
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                        totalWritten += bytesRead
                        // Phase 1: 0.0 to 0.8 (Writing bytes)
                        val progress = if (apkFile.length() > 0) (totalWritten.toFloat() / apkFile.length()) * 0.8f else 0f
                        status.value = InstallStatus.Installing(progress)
                        session.setStagingProgress(progress)
                    }
                    session.fsync(fos)
                }
            }

            val intent = Intent(context, InstallReceiver::class.java).apply {
                action = "${context.packageName}.INSTALL_STATUS"
                putExtra("session_id", sessionId)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context, 
                sessionId, 
                intent, 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
            )

            session.commit(pendingIntent.intentSender)
            session.close()

        } catch (e: Exception) {
            status.value = InstallStatus.Failure(e.message ?: "Unknown error")
            activeSessionId.set(-1)
        }
    }
}

class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getIntExtra("session_id", -1)
        InternalApkInstaller.clearActiveSession(sessionId)

        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                InternalApkInstaller.status.value = InstallStatus.Success
            }
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                confirmIntent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
            }
            else -> {
                InternalApkInstaller.status.value = InstallStatus.Failure(message ?: "Error code: $status")
            }
        }
    }
}

