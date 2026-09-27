package com.shizuku.filemanager.sys

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.FileObserver
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class FolderWatcherService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val observers = mutableMapOf<String, FileObserver>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification().build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val path = intent?.getStringExtra("path")
        val action = intent?.action
        
        if (path != null) {
            if (action == "STOP_WATCHING") {
                stopWatching(path)
            } else {
                startWatching(path)
            }
        }
        return START_STICKY
    }

    private fun startWatching(path: String) {
        if (observers.containsKey(path)) return
        
        try {
            val observer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                object : FileObserver(File(path), FileObserver.CREATE or FileObserver.DELETE or FileObserver.MODIFY or FileObserver.MOVED_TO or FileObserver.MOVED_FROM) {
                    override fun onEvent(event: Int, fileName: String?) {
                        handleEvent(event, path, fileName)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                object : FileObserver(path, FileObserver.CREATE or FileObserver.DELETE or FileObserver.MODIFY or FileObserver.MOVED_TO or FileObserver.MOVED_FROM) {
                    override fun onEvent(event: Int, fileName: String?) {
                        handleEvent(event, path, fileName)
                    }
                }
            }
            observer.startWatching()
            observers[path] = observer
        } catch (_: Exception) {}
    }

    private fun stopWatching(path: String) {
        observers.remove(path)?.stopWatching()
    }

    private fun handleEvent(event: Int, path: String, fileName: String?) {
        val type = when {
            event and FileObserver.CREATE != 0 -> "NEW"
            event and FileObserver.MODIFY != 0 -> "MODIFIED"
            event and FileObserver.DELETE != 0 -> "DELETED"
            event and FileObserver.MOVED_TO != 0 -> "NEW"
            event and FileObserver.MOVED_FROM != 0 -> "DELETED"
            else -> return
        }
        
        val name = fileName ?: "unknown"
        val fullPath = "$path/$name"

        serviceScope.launch {
            EventLogger.logEvent(applicationContext, FileEvent(name, fullPath, type, System.currentTimeMillis()))
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "folder_watcher",
                "Folder Watcher",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, "folder_watcher")
            .setContentTitle("FileManager Watcher")
            .setContentText("Monitoring folder changes...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    override fun onDestroy() {
        observers.values.forEach { it.stopWatching() }
        observers.clear()
        super.onDestroy()
    }
}
