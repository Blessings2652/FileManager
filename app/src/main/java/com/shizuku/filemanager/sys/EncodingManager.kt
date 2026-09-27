package com.shizuku.filemanager.sys

import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object EncodingManager {
    
    fun detectEncoding(file: File): Charset {
        val bytes = try {
            file.inputStream().use { input ->
                val buffer = ByteArray(1024)
                val count = input.read(buffer)
                if (count > 0) buffer.copyOf(count) else ByteArray(0)
            }
        } catch (e: Exception) {
            return StandardCharsets.UTF_8
        }

        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return StandardCharsets.UTF_8
        }
        
        // Simple heuristic: if all bytes are < 128, it's likely US-ASCII or UTF-8
        if (bytes.all { it.toInt() in 0..127 }) {
            return StandardCharsets.UTF_8
        }
        
        return Charset.defaultCharset()
    }

    fun convertFile(file: File, from: Charset, to: Charset): Result<Unit> {
        return try {
            val content = file.readText(from)
            file.writeText(content, to)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
