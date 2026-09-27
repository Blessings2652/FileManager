package com.shizuku.filemanager.sys

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object ArchiveManager {

    suspend fun listZipEntries(zipFilePath: String): Result<List<ZipEntryInfo>> = withContext(Dispatchers.IO) {
        try {
            val zipFile = ZipFile(zipFilePath)
            val entries = zipFile.entries().asSequence().map { entry ->
                ZipEntryInfo(
                    name = entry.name,
                    size = entry.size,
                    compressedSize = entry.compressedSize,
                    isDirectory = entry.isDirectory,
                    time = entry.time
                )
            }.toList()
            zipFile.close()
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun extractZip(
        zipFilePath: String,
        outputDir: String,
        onProgress: ((Float) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val zipFile = ZipFile(zipFilePath)
            val allEntries = zipFile.entries().asSequence().toList()
            val totalEntries = allEntries.size
            var processedEntries = 0

            val outputFolder = File(outputDir)
            if (!outputFolder.exists()) outputFolder.mkdirs()

            for (entry in allEntries) {
                val entryFile = File(outputDir, entry.name)

                if (entry.isDirectory) {
                    entryFile.mkdirs()
                } else {
                    entryFile.parentFile?.mkdirs()
                    zipFile.getInputStream(entry).use { input ->
                        FileOutputStream(entryFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                processedEntries++
                onProgress?.invoke(processedEntries.toFloat() / totalEntries)
            }
            zipFile.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun extractEntry(zipFilePath: String, entryName: String, outputFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val zipFile = ZipFile(zipFilePath)
            val entry = zipFile.getEntry(entryName) ?: return@withContext Result.failure(Exception("Entry not found"))
            
            outputFile.parentFile?.mkdirs()
            zipFile.getInputStream(entry).use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            zipFile.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createZip(
        files: List<File>,
        zipFile: File,
        level: Int = 6, // 0-9
        onProgress: ((Float) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                zos.setLevel(level)
                var processed = 0
                files.forEach { file ->
                    addFileToZip(file, file.name, zos)
                    processed++
                    onProgress?.invoke(processed.toFloat() / files.size)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addFileToZip(file: File, fileName: String, zos: ZipOutputStream) {
        if (file.isDirectory) {
            val entry = ZipEntry(if (fileName.endsWith("/")) fileName else "$fileName/")
            zos.putNextEntry(entry)
            zos.closeEntry()
            file.listFiles()?.forEach { child ->
                addFileToZip(child, "$fileName/${child.name}", zos)
            }
        } else {
            FileInputStream(file).use { fis ->
                val entry = ZipEntry(fileName)
                zos.putNextEntry(entry)
                fis.copyTo(zos)
                zos.closeEntry()
            }
        }
    }
}

data class ZipEntryInfo(
    val name: String,
    val size: Long,
    val compressedSize: Long,
    val isDirectory: Boolean,
    val time: Long
)
