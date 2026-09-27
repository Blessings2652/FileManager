package com.shizuku.filemanager.fs.engine

import android.content.Context
import android.os.Environment
import com.shizuku.filemanager.fs.FileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StandardFileEngine(private val context: Context) : FileEngine {

    override val type = EngineType.STANDARD
    override val rootPath = Environment.getExternalStorageDirectory().absolutePath

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    override suspend fun list(path: String): Result<List<FileEntry>> = withContext(Dispatchers.IO) {
        try {
            val dir = File(path)
            val cleanPath = path.removeSuffix("/")
            val storageRoot = rootPath
            val androidData = "$storageRoot/Android/data"
            val androidObb = "$storageRoot/Android/obb"
            
            // Handle Android/data and Android/obb specifically for Android 11+
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                if (cleanPath == androidData || cleanPath == androidObb ||
                    cleanPath.startsWith("$androidData/") || cleanPath.startsWith("$androidObb/")) {
                    return@withContext Result.failure(RestrictedAccessException(path))
                }
            }

            if (!dir.exists()) return@withContext Result.failure(IOException("Directory does not exist: $path"))
            if (!dir.isDirectory) return@withContext Result.failure(IOException("Not a directory: $path"))

            val files = dir.listFiles() ?: return@withContext Result.success(emptyList())
            
            val entries = files.map { file ->
                FileEntry(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    isSymlink = false, // java.io.File doesn't expose this easily
                    sizeBytes = if (file.isDirectory) 0L else file.length(),
                    permissions = getPermissions(file),
                    owner = "-", // Not easily available via java.io.File
                    group = "-", 
                    modified = dateFormat.format(Date(file.lastModified()))
                )
            }.sortedWith(compareByDescending<FileEntry> { it.isDirectory }.thenBy { it.name.lowercase() })

            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getPermissions(file: File): String {
        val r = if (file.canRead()) "r" else "-"
        val w = if (file.canWrite()) "w" else "-"
        val x = if (file.canExecute()) "x" else "-"
        val type = if (file.isDirectory) "d" else "-"
        return "$type$r$w$x"
    }

    override suspend fun mkdir(parentPath: String, name: String): Result<String> = withContext(Dispatchers.IO) {
        val newDir = File(parentPath, name)
        if (newDir.mkdirs() || newDir.exists()) Result.success(newDir.absolutePath)
        else Result.failure(IOException("Failed to create directory: ${newDir.absolutePath}"))
    }

    override suspend fun createFile(parentPath: String, name: String): Result<String> = withContext(Dispatchers.IO) {
        val newFile = File(parentPath, name)
        try {
            if (newFile.createNewFile()) Result.success(newFile.absolutePath)
            else if (newFile.exists()) Result.success(newFile.absolutePath)
            else Result.failure(IOException("Failed to create file: ${newFile.absolutePath}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(entry: FileEntry): Result<Unit> = withContext(Dispatchers.IO) {
        val file = File(entry.path)
        if (file.deleteRecursively()) Result.success(Unit)
        else Result.failure(IOException("Failed to delete: ${entry.path}"))
    }

    override suspend fun rename(entry: FileEntry, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        val source = File(entry.path)
        val destination = File(source.parentFile, newName)
        if (source.renameTo(destination)) Result.success(Unit)
        else Result.failure(IOException("Failed to rename to: $newName"))
    }

    override suspend fun copy(entry: FileEntry, srcParentPath: String, destDirPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val source = File(entry.path)
            val dest = File(destDirPath, source.name)
            if (source.isDirectory) {
                source.copyRecursively(dest, overwrite = true)
            } else {
                source.copyTo(dest, overwrite = true)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun move(entry: FileEntry, srcParentPath: String, destDirPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        val source = File(entry.path)
        val dest = File(destDirPath, source.name)
        
        // Try atomic move first
        if (source.renameTo(dest)) {
            Result.success(Unit)
        } else {
            // Fallback: Copy and Delete (useful for cross-partition moves)
            try {
                if (source.isDirectory) {
                    source.copyRecursively(dest, overwrite = true)
                } else {
                    source.copyTo(dest, overwrite = true)
                }
                if (source.deleteRecursively()) {
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("Move partially succeeded: copied to $destDirPath but could not delete original."))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun parentPath(path: String): String? {
        if (path == rootPath) return null
        val file = File(path)
        val parent = file.parentFile ?: return null
        return parent.absolutePath
    }

    override fun getChildPath(parentPath: String, name: String): String =
        File(parentPath, name).absolutePath

    override fun getDisplayName(path: String): String {
        if (path == rootPath || path == "/storage/emulated/0" || path == "/sdcard") return "Internal Storage"
        if (path == "/storage/emulated" || path == "/storage") return "Internal Storage"
        if (path == "/") return "Root"
        return File(path).name.ifBlank { path }
    }

    override suspend fun readText(path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Result.success(File(path).readText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun writeText(path: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            File(path).writeText(text)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun readBytes(path: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            Result.success(File(path).readBytes())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun writeBytes(path: String, bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            File(path).writeBytes(bytes)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
