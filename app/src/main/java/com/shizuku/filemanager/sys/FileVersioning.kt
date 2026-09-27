package com.shizuku.filemanager.sys

import android.content.Context
import com.shizuku.filemanager.db.ExtrasDatabase
import com.shizuku.filemanager.db.FileVersion
import com.shizuku.filemanager.fs.engine.EngineType
import com.shizuku.filemanager.fs.engine.FileEngine
import java.security.MessageDigest

/**
 * Automatic "undo" safety net: before a file is overwritten or deleted,
 * copy its current bytes into a hidden snapshot folder so the user can
 * restore it later, distinct from Trash (which holds the whole deleted
 * item) — this specifically covers in-place edits/overwrites that Trash
 * never sees.
 *
 * Call [snapshotBeforeChange] from the two places content is destroyed:
 * FileBrowserViewModel.delete (reason = "DELETE") and CodeEditorScreen /
 * PhotoEditorScreen's save path (reason = "OVERWRITE"), before the
 * destructive engine call runs.
 */
object FileVersioning {

    private const val VERSIONS_DIR_NAME = ".filemanager_versions"
    private const val MAX_VERSIONS_PER_FILE = 10
    /** Skip snapshotting anything bigger than this; versioning huge media files isn't useful. */
    private const val MAX_SNAPSHOT_BYTES = 25L * 1024 * 1024

    private fun dao(context: Context) = ExtrasDatabase.getDatabase(context.applicationContext).fileVersionDao()

    suspend fun snapshotBeforeChange(
        context: Context,
        engine: FileEngine,
        filePath: String,
        sizeBytes: Long,
        reason: String,
    ): Result<Unit> {
        if (sizeBytes <= 0 || sizeBytes > MAX_SNAPSHOT_BYTES) return Result.success(Unit)
        val bytes = engine.readBytes(filePath).getOrElse { return Result.success(Unit) } // best-effort; don't block the real op on this

        val versionsRoot = engine.getChildPath(engine.rootPath, VERSIONS_DIR_NAME)
        engine.mkdir(engine.rootPath, VERSIONS_DIR_NAME) // ignore "already exists" failure

        val hash = MessageDigest.getInstance("SHA-256").digest(filePath.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)
        val snapshotName = "${System.currentTimeMillis()}_$hash"
        val snapshotPath = engine.getChildPath(versionsRoot, snapshotName)
        engine.writeBytes(snapshotPath, bytes).onFailure { return Result.success(Unit) }

        dao(context).add(
            FileVersion(
                originalPath = filePath,
                snapshotPath = snapshotPath,
                engineType = engine.type.name,
                reason = reason,
                sizeBytes = bytes.size.toLong(),
            )
        )
        pruneOld(context, engine, filePath)
        return Result.success(Unit)
    }

    suspend fun listVersions(context: Context, filePath: String): List<FileVersion> =
        dao(context).getVersions(filePath)

    suspend fun restore(context: Context, engine: FileEngine, version: FileVersion, restoreToPath: String): Result<Unit> {
        val bytes = engine.readBytes(version.snapshotPath).getOrElse { return Result.failure(it) }
        return engine.writeBytes(restoreToPath, bytes)
    }

    suspend fun deleteVersion(context: Context, engine: FileEngine, version: FileVersion) {
        engine.readBytes(version.snapshotPath) // no-op touch; real cleanup below
        runCatching {
            val entry = com.shizuku.filemanager.fs.FileEntry(
                name = version.snapshotPath.substringAfterLast('/'),
                path = version.snapshotPath, isDirectory = false, isSymlink = false,
                sizeBytes = version.sizeBytes, permissions = "", owner = "", group = "", modified = ""
            )
            engine.delete(entry)
        }
        dao(context).delete(version)
    }

    private suspend fun pruneOld(context: Context, engine: FileEngine, filePath: String) {
        val versions = dao(context).getVersions(filePath)
        if (versions.size > MAX_VERSIONS_PER_FILE) {
            versions.drop(MAX_VERSIONS_PER_FILE).forEach { deleteVersion(context, engine, it) }
        }
    }
}
