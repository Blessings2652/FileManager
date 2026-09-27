package com.shizuku.filemanager

import android.app.Application
import com.shizuku.filemanager.shizuku.ShizukuManager

class FileManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ShizukuManager.init()
    }

    override fun onTerminate() {
        ShizukuManager.destroy()
        super.onTerminate()
    }
}
