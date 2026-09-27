package com.shizuku.filemanager.fs.engine

import com.shizuku.filemanager.fs.FileEntry

/**
 * SKELETON — same status as SmbFileEngine.kt / FtpFileEngine.kt.
 * WebDAV covers Nextcloud/ownCloud and any generic WebDAV share.
 *
 * Add to build.gradle:
 *   implementation("com.github.thegrizzlylabs:sardine-android:0.9")
 *
 * Map com.thegrizzlylabs.sardineandroid.Sardine's list()/get()/put()/
 * delete()/move() calls the same way. `path` here is a full https URL
 * to the resource, which conveniently already round-trips through
 * getChildPath/parentPath without extra bookkeeping.
 */
class WebDavFileEngine(
    private val baseUrl: String,
    private val username: String,
    private val password: String,
) : FileEngine {

    override val type: EngineType get() = throw NotImplementedError("Add EngineType.WEBDAV first")
    override val rootPath: String = baseUrl

    override suspend fun list(path: String): Result<List<FileEntry>> =
        Result.failure(NotImplementedError("Wire up Sardine.list() here"))

    override suspend fun mkdir(parentPath: String, name: String): Result<String> = Result.failure(NotImplementedError())
    override suspend fun createFile(parentPath: String, name: String): Result<String> = Result.failure(NotImplementedError())
    override suspend fun delete(entry: FileEntry): Result<Unit> = Result.failure(NotImplementedError())
    override suspend fun rename(entry: FileEntry, newName: String): Result<Unit> = Result.failure(NotImplementedError())
    override suspend fun copy(entry: FileEntry, srcParentPath: String, destDirPath: String): Result<Unit> = Result.failure(NotImplementedError())
    override suspend fun move(entry: FileEntry, srcParentPath: String, destDirPath: String): Result<Unit> = Result.failure(NotImplementedError())
    override fun parentPath(path: String): String? = path.substringBeforeLast('/', "").ifEmpty { null }
    override fun getChildPath(parentPath: String, name: String): String = "$parentPath/$name"
    override fun getDisplayName(path: String): String = path.substringAfterLast('/')
    override suspend fun readText(path: String): Result<String> = Result.failure(NotImplementedError())
    override suspend fun writeText(path: String, text: String): Result<Unit> = Result.failure(NotImplementedError())
    override suspend fun readBytes(path: String): Result<ByteArray> = Result.failure(NotImplementedError())
    override suspend fun writeBytes(path: String, bytes: ByteArray): Result<Unit> = Result.failure(NotImplementedError())
}
