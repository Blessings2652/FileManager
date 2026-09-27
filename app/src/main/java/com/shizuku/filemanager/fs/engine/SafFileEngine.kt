package com.shizuku.filemanager.fs.engine

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.shizuku.filemanager.fs.FileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * SAF engine: works from a single user-granted tree Uri (no root, no
 * Shizuku). Everything is scoped under that tree — you can't browse
 * outside the folder the user picked.
 *
 * DocumentFile objects are cached by their Uri string so navigation
 * (parentPath) and operations (rename/delete/etc.) can look nodes back up
 * instead of re-deriving them from string paths, which SAF doesn't
 * support the way raw filesystem paths do.
 */
class SafFileEngine(
    private val context: Context,
    treeUri: Uri
) : FileEngine {

    override val type = EngineType.SAF

    private val docCache = ConcurrentHashMap<String, DocumentFile>()
    private val rootDoc: DocumentFile = DocumentFile.fromTreeUri(context, treeUri)
        ?: throw IllegalStateException("Could not open granted folder")

    override val rootPath: String = rootDoc.uri.toString().also { docCache[it] = rootDoc }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    private fun getDoc(path: String): DocumentFile? {
        val cached = docCache[path]
        if (cached != null) return cached

        return try {
            val uri = Uri.parse(path)
            val doc = if (DocumentsContract.isDocumentUri(context, uri)) {
                DocumentFile.fromSingleUri(context, uri)
            } else {
                DocumentFile.fromTreeUri(context, uri)
            }
            doc?.also { docCache[path] = it }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun list(path: String): Result<List<FileEntry>> = withContext(Dispatchers.IO) {
        try {
            val doc = getDoc(path) ?: return@withContext Result.failure(Exception("Folder no longer accessible"))
            val entries = doc.listFiles().map { child ->
                docCache[child.uri.toString()] = child
                FileEntry(
                    name = child.name ?: "?",
                    path = child.uri.toString(),
                    isDirectory = child.isDirectory,
                    isSymlink = false,
                    sizeBytes = child.length(),
                    permissions = if (child.isDirectory) "drwx" else if (child.canWrite()) "-rw-" else "-r--",
                    owner = "-",
                    group = "-",
                    modified = if (child.lastModified() > 0) dateFormat.format(Date(child.lastModified())) else "-"
                )
            }.sortedWith(compareByDescending<FileEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
            Result.success(entries)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    override suspend fun mkdir(parentPath: String, name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val parent = getDoc(parentPath) ?: return@withContext Result.failure(Exception("Folder no longer accessible"))
            val existing = parent.findFile(name)
            if (existing != null && existing.isDirectory) return@withContext Result.success(existing.uri.toString())
            
            val created = parent.createDirectory(name) ?: return@withContext Result.failure(Exception("Could not create folder"))
            val path = created.uri.toString()
            docCache[path] = created
            Result.success(path)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    override suspend fun createFile(parentPath: String, name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val parent = getDoc(parentPath) ?: return@withContext Result.failure(Exception("Folder no longer accessible"))
            val existing = parent.findFile(name)
            if (existing != null && !existing.isDirectory) return@withContext Result.success(existing.uri.toString())
            
            val created = parent.createFile("application/octet-stream", name)
                ?: return@withContext Result.failure(Exception("Could not create file"))
            val path = created.uri.toString()
            docCache[path] = created
            Result.success(path)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    override suspend fun delete(entry: FileEntry): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = getDoc(entry.path) ?: return@withContext Result.failure(Exception("File no longer accessible"))
            if (doc.delete()) {
                docCache.remove(entry.path)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete failed"))
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    override suspend fun rename(entry: FileEntry, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = getDoc(entry.path) ?: return@withContext Result.failure(Exception("File no longer accessible"))
            if (doc.renameTo(newName)) {
                docCache.remove(entry.path)
                docCache[doc.uri.toString()] = doc
                Result.success(Unit)
            } else {
                Result.failure(Exception("Rename failed"))
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /** SAF has no native recursive copy — files are streamed byte-for-byte, directories walked recursively. */
    override suspend fun copy(entry: FileEntry, srcParentPath: String, destDirPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val src = getDoc(entry.path) ?: return@withContext Result.failure(Exception("File no longer accessible"))
                val destParent = getDoc(destDirPath) ?: return@withContext Result.failure(Exception("Destination no longer accessible"))
                copyRecursive(src, destParent)
                Result.success(Unit)
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

    private fun copyRecursive(src: DocumentFile, destParent: DocumentFile) {
        if (src.isDirectory) {
            val newDir = destParent.createDirectory(src.name ?: "folder") ?: throw Exception("Could not create ${src.name}")
            docCache[newDir.uri.toString()] = newDir
            src.listFiles().forEach { child -> copyRecursive(child, newDir) }
        } else {
            val newFile = destParent.createFile(src.type ?: "application/octet-stream", src.name ?: "file")
                ?: throw Exception("Could not create ${src.name}")
            docCache[newFile.uri.toString()] = newFile
            context.contentResolver.openInputStream(src.uri)?.use { input ->
                context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    /** True move via DocumentsContract.moveDocument when possible, else copy+delete fallback. */
    override suspend fun move(entry: FileEntry, srcParentPath: String, destDirPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val src = getDoc(entry.path) ?: return@withContext Result.failure(Exception("File no longer accessible"))
                val srcParent = getDoc(srcParentPath) ?: return@withContext Result.failure(Exception("Source folder no longer accessible"))
                val destParent = getDoc(destDirPath) ?: return@withContext Result.failure(Exception("Destination no longer accessible"))

                val moved = try {
                    DocumentsContract.moveDocument(
                        context.contentResolver, src.uri, srcParent.uri, destParent.uri
                    )
                } catch (_: Throwable) {
                    null
                }

                if (moved != null) {
                    docCache.remove(entry.path)
                    DocumentFile.fromSingleUri(context, moved)?.let { newDoc ->
                        docCache[moved.toString()] = newDoc
                    }
                    Result.success(Unit)
                } else {
                    // Fallback for providers that don't support moveDocument: copy then delete original.
                    copyRecursive(src, destParent)
                    if (src.delete()) docCache.remove(entry.path)
                    Result.success(Unit)
                }
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

    override fun parentPath(path: String): String? {
        if (path == rootPath) return null
        val doc = getDoc(path) ?: return null
        val parent = doc.parentFile ?: return null
        docCache[parent.uri.toString()] = parent
        return parent.uri.toString()
    }

    override fun getChildPath(parentPath: String, name: String): String {
        val parent = getDoc(parentPath) ?: return "$parentPath/$name"
        val child = parent.findFile(name)
        return child?.uri?.toString() ?: "$parentPath/$name"
    }

    override fun getDisplayName(path: String): String {
        if (path == rootPath) return rootDoc.name ?: "Storage"
        return getDoc(path)?.name ?: path.substringAfterLast('/')
    }

    override suspend fun readText(path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val doc = getDoc(path) ?: return@withContext Result.failure(Exception("File no longer accessible"))
            context.contentResolver.openInputStream(doc.uri)?.use { input ->
                Result.success(input.bufferedReader().readText())
            } ?: Result.failure(Exception("Could not open stream"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun writeText(path: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = getDoc(path) ?: return@withContext Result.failure(Exception("File no longer accessible"))
            context.contentResolver.openOutputStream(doc.uri, "wt")?.use { output ->
                output.bufferedWriter().use { it.write(text) }
                Result.success(Unit)
            } ?: Result.failure(Exception("Could not open stream"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun readBytes(path: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val doc = getDoc(path) ?: return@withContext Result.failure(Exception("File no longer accessible"))
            context.contentResolver.openInputStream(doc.uri)?.use { input ->
                Result.success(input.readBytes())
            } ?: Result.failure(Exception("Could not open stream"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun writeBytes(path: String, bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = getDoc(path) ?: return@withContext Result.failure(Exception("File no longer accessible"))
            context.contentResolver.openOutputStream(doc.uri, "wt")?.use { output ->
                output.write(bytes)
                Result.success(Unit)
            } ?: Result.failure(Exception("Could not open stream"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
