package com.shizuku.filemanager.fs.engine

import com.shizuku.filemanager.fs.FileEntry

/**
 * Common contract for file operations, implemented separately for Shizuku
 * (shell UID), Root (su), and SAF (DocumentFile) access.
 *
 * Paths are opaque engine-specific identifiers: raw filesystem paths for
 * Shizuku/Root, document Uri strings for SAF. Callers (the ViewModel/UI)
 * never parse or build them manually — they always go through
 * [parentPath] for navigation and pass the [FileEntry.path] straight
 * back into the engine for operations.
 */
interface FileEngine {

    val type: EngineType

    /** Starting directory to show when the browser first opens. */
    val rootPath: String

    suspend fun list(path: String): Result<List<FileEntry>>

    suspend fun mkdir(parentPath: String, name: String): Result<String>

    suspend fun createFile(parentPath: String, name: String): Result<String>

    suspend fun delete(entry: FileEntry): Result<Unit>

    suspend fun rename(entry: FileEntry, newName: String): Result<Unit>

    suspend fun copy(entry: FileEntry, srcParentPath: String, destDirPath: String): Result<Unit>

    suspend fun move(entry: FileEntry, srcParentPath: String, destDirPath: String): Result<Unit>

    /** Returns the parent of [path], or null if [path] is already at the top of what's browsable. */
    fun parentPath(path: String): String?

    /** Resolves a child name against a parent path. */
    fun getChildPath(parentPath: String, name: String): String

    /** Returns a user-friendly name for a path segment. */
    fun getDisplayName(path: String): String

    suspend fun readText(path: String): Result<String>

    suspend fun writeText(path: String, text: String): Result<Unit>

    suspend fun readBytes(path: String): Result<ByteArray>

    suspend fun writeBytes(path: String, bytes: ByteArray): Result<Unit>
}
