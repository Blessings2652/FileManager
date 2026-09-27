package com.shizuku.filemanager.fs.engine

import com.shizuku.filemanager.fs.FileEntry
import com.shizuku.filemanager.fs.RootManager

class RootFileEngine : FileEngine {

    override val type = EngineType.ROOT
    override val rootPath = "/"

    private fun q(path: String) = LsParser.quote(path)

    override suspend fun list(path: String): Result<List<FileEntry>> {
        val result = RootManager.runShell("ls -la --full-time ${q(path)} 2>&1")
        if (result.exitCode != 0) {
            return Result.failure(Exception(result.stderr.ifBlank { result.stdout }.ifBlank { "ls failed (exit ${result.exitCode})" }))
        }
        return Result.success(LsParser.parseListing(result.stdout, path))
    }

    override suspend fun mkdir(parentPath: String, name: String): Result<String> {
        val path = childPath(parentPath, name)
        return runOk("mkdir -p ${q(path)}").map { path }
    }

    override suspend fun createFile(parentPath: String, name: String): Result<String> {
        val path = childPath(parentPath, name)
        return runOk("touch ${q(path)}").map { path }
    }

    override suspend fun delete(entry: FileEntry): Result<Unit> =
        runOk("rm -rf ${q(entry.path)}")

    override suspend fun rename(entry: FileEntry, newName: String): Result<Unit> {
        val newPath = childPath(entry.path.substringBeforeLast('/'), newName)
        return runOk("mv ${q(entry.path)} ${q(newPath)}")
    }

    override suspend fun copy(entry: FileEntry, srcParentPath: String, destDirPath: String): Result<Unit> =
        runOk("cp -r ${q(entry.path)} ${q(destDirPath)}")

    override suspend fun move(entry: FileEntry, srcParentPath: String, destDirPath: String): Result<Unit> =
        runOk("mv ${q(entry.path)} ${q(destDirPath)}")

    override fun parentPath(path: String): String? {
        if (path == "/") return null
        return path.substringBeforeLast('/', "/").ifBlank { "/" }
    }

    override fun getChildPath(parentPath: String, name: String): String =
        childPath(parentPath, name)

    override fun getDisplayName(path: String): String {
        if (path == "/" || path == "") return "Root"
        if (path == "/storage/emulated/0" || path == "/sdcard") return "Internal Storage"
        return path.substringAfterLast('/').ifBlank { path }
    }

    override suspend fun readText(path: String): Result<String> {
        val res = RootManager.runShell("cat \"$path\"")
        return if (res.exitCode == 0) Result.success(res.stdout) else Result.failure(Exception(res.stderr))
    }

    override suspend fun writeText(path: String, text: String): Result<Unit> {
        // Simple write for now.
        val res = RootManager.runShell("echo '${text.replace("'", "'\\''")}' > \"$path\"")
        return if (res.exitCode == 0) Result.success(Unit) else Result.failure(Exception(res.stderr))
    }

    override suspend fun readBytes(path: String): Result<ByteArray> {
        val res = RootManager.runShell("base64 \"$path\"")
        return if (res.exitCode == 0) {
            try {
                Result.success(android.util.Base64.decode(res.stdout, android.util.Base64.DEFAULT))
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else Result.failure(Exception(res.stderr))
    }

    override suspend fun writeBytes(path: String, bytes: ByteArray): Result<Unit> {
        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        val res = RootManager.runShell("echo '$b64' | base64 -d > \"$path\"")
        return if (res.exitCode == 0) Result.success(Unit) else Result.failure(Exception(res.stderr))
    }

    private fun childPath(parent: String, name: String) =
        if (parent.endsWith("/")) "$parent$name" else "$parent/$name"

    private suspend fun runOk(cmd: String): Result<Unit> {
        val result = RootManager.runShell(cmd)
        return if (result.exitCode == 0) Result.success(Unit)
        else Result.failure(Exception(result.stderr.ifBlank { result.stdout }.ifBlank { "command failed (exit ${result.exitCode})" }))
    }
}
