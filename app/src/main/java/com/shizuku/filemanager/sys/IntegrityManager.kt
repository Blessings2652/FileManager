package com.shizuku.filemanager.sys

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object IntegrityManager {

    suspend fun calculateHash(file: File, algorithm: String): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance(algorithm)
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun verifyHash(file: File, expectedHash: String, algorithm: String): Boolean {
        val actualHash = calculateHash(file, algorithm)
        return actualHash.equals(expectedHash, ignoreCase = true)
    }
}
