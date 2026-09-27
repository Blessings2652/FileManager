package com.shizuku.filemanager.fs.engine

import com.shizuku.filemanager.fs.FileEntry

/**
 * SKELETON — not wired into EngineType/EnginePrefs yet, and will not
 * compile as-is: it depends on a SMB2/3 client library that is not part
 * of this source drop (no build.gradle was included in the uploaded
 * project, so I'm not silently inventing dependency versions for you).
 *
 * To make this real, add to your module's build.gradle:
 *   implementation("com.hierynomus:smbj:0.13.0")
 *
 * Then implement each method using smbj's `SMBClient` -> `Session` ->
 * `DiskShare`, mapping DiskShare.list()/openFile()/etc to FileEntry the
 * same way StandardFileEngine maps java.io.File. A `path` for this
 * engine should be an opaque "share/relative/path" string (mirroring
 * how SafFileEngine treats its Uri strings as opaque paths) so the rest
 * of the app (ViewModel/UI) doesn't need to know it's talking to SMB.
 *
 * Credentials: prompt for host/share/user/password once (a new
 * "Add network location" screen), store them via Android's
 * EncryptedSharedPreferences (androidx.security:security-crypto),
 * never in EnginePrefs' plain SharedPreferences.
 */
class SmbFileEngine(
    private val host: String,
    private val share: String,
    private val username: String,
    private val password: String,
) : FileEngine {

    override val type: EngineType get() = throw NotImplementedError("Add EngineType.SMB first")
    override val rootPath: String = ""

    override suspend fun list(path: String): Result<List<FileEntry>> =
        Result.failure(NotImplementedError("Wire up smbj DiskShare.list() here"))

    override suspend fun mkdir(parentPath: String, name: String): Result<String> =
        Result.failure(NotImplementedError())

    override suspend fun createFile(parentPath: String, name: String): Result<String> =
        Result.failure(NotImplementedError())

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
