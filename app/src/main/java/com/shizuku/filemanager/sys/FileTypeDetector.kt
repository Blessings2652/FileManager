package com.shizuku.filemanager.sys

import java.io.File
import java.io.FileInputStream

object FileTypeDetector {
    private val MAGIC_NUMBERS = mapOf(
        "89504E47" to "image/png",
        "FFD8FFE0" to "image/jpeg",
        "FFD8FFE1" to "image/jpeg",
        "47494638" to "image/gif",
        "25504446" to "application/pdf",
        "504B0304" to "application/zip",
        "504B0506" to "application/zip",
        "504B0708" to "application/zip",
        "7F454C46" to "application/x-elf",
        "D0CF11E0" to "application/msword",
        "1F8B08" to "application/x-gzip"
    )

    fun detectMimeType(file: File): String {
        try {
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(4)
                if (fis.read(buffer) != 4) return "application/octet-stream"
                val hex = buffer.joinToString("") { "%02X".format(it) }
                
                MAGIC_NUMBERS.forEach { (magic, mime) ->
                    if (hex.startsWith(magic)) return mime
                }
            }
        } catch (e: Exception) {
            // fallback to extension
        }
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension) ?: "application/octet-stream"
    }
}
