package com.shizuku.filemanager.fs.engine

import java.io.File

object FileUtils {
    fun getRecursiveSize(file: File): Long {
        if (!file.exists()) return 0
        if (!file.isDirectory) return file.length()
        
        var size = 0L
        val files = file.listFiles()
        if (files != null) {
            for (child in files) {
                size += getRecursiveSize(child)
            }
        }
        return size
    }
}
