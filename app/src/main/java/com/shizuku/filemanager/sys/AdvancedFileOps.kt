package com.shizuku.filemanager.sys

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.security.SecureRandom
import kotlin.math.min

object AdvancedFileOps {

    suspend fun bulkRename(
        files: List<File>,
        pattern: String,
        replacement: String,
        useRegex: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var count = 0
            files.forEach { file ->
                val newName = if (useRegex) {
                    file.name.replace(Regex(pattern), replacement)
                } else {
                    file.name.replace(pattern, replacement)
                }
                
                if (newName != file.name) {
                    val dest = File(file.parent, newName)
                    if (file.renameTo(dest)) {
                        count++
                    }
                }
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun secureDelete(file: File, passes: Int = 3): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (file.isDirectory) {
                file.listFiles()?.forEach { child ->
                    secureDelete(child, passes)
                }
                file.delete()
            } else {
                val length = file.length()
                if (length > 0) {
                    RandomAccessFile(file, "rws").use { raf ->
                        val random = SecureRandom()
                        val buffer = ByteArray(min(length, 8192).toInt())
                        
                        repeat(passes) {
                            random.nextBytes(buffer)
                            raf.seek(0)
                            var written = 0L
                            while (written < length) {
                                val toWrite = min(buffer.size.toLong(), length - written).toInt()
                                raf.write(buffer, 0, toWrite)
                                written += toWrite
                            }
                        }
                    }
                }
                file.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
