package com.shizuku.filemanager.fs.engine

import com.shizuku.filemanager.fs.FileEntry

/**
 * SKELETON — same status as SmbFileEngine.kt: not wired in, needs a
 * dependency this source drop doesn't have.
 *
 * Add to build.gradle:
 *   implementation("commons-net:commons-net:3.11.1")     // FTP/FTPS
 * For SFTP instead, use:
 *   implementation("com.hierynomus:sshj:0.38.0")
 *
 * Map org.apache.commons.net.ftp.FTPClient.listFiles()/retrieveFile()/
 * storeFile() to FileEntry / readBytes / writeBytes. Keep one FTPClient
 * per engine instance, connect lazily on first call, and guard every
 * call with a mutex — FTPClient is not safe for concurrent use from the
 * multiple coroutines FileBrowserViewModel may fire off (thumbnailing,
 * listing, etc).
 */
class FtpFileEngine(
    private val host: String,
    private val port: Int = 21,
    private val username: String,
    private val password: String,
    private val useFtps: Boolean = false,
) : FileEngine {

    override val type: EngineType get() = throw NotImplementedError("Add EngineType.FTP first")
    override val rootPath: String = "/"

    override suspend fun list(path: String): Result<List<FileEntry>> =
        Result.failure(NotImplementedError("Wire up FTPClient.listFiles() here"))

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
