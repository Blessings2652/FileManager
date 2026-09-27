package com.shizuku.filemanager.fs

import android.content.Context
import com.shizuku.filemanager.db.ClipboardItem
import com.shizuku.filemanager.db.ExtrasDatabase
import com.shizuku.filemanager.fs.engine.EngineType
import com.shizuku.filemanager.fs.engine.FileEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class ClipMode { CUT, COPY }

data class ClipboardState(
    val items: List<ClipboardItem> = emptyList(),
    val mode: ClipMode = ClipMode.COPY,
) {
    val isEmpty: Boolean get() = items.isEmpty()
}

/**
 * File cut/copy clipboard that survives process death (backed by Room),
 * unlike a plain in-memory list — so navigating away, or Android killing
 * the app in the background mid-browse, doesn't silently drop a pending
 * paste. Only path/engine metadata is stored, never file bytes.
 */
object FileClipboard {

    private val _state = MutableStateFlow(ClipboardState())
    val state = _state.asStateFlow()

    private fun dao(context: Context) = ExtrasDatabase.getDatabase(context.applicationContext).clipboardDao()

    suspend fun restore(context: Context) {
        val items = dao(context).getAll()
        if (items.isNotEmpty()) {
            val mode = runCatching { ClipMode.valueOf(items.first().mode) }.getOrDefault(ClipMode.COPY)
            _state.value = ClipboardState(items, mode)
        }
    }

    suspend fun set(context: Context, entries: List<FileEntry>, srcParentPath: String, engineType: EngineType, mode: ClipMode) {
        val batchId = UUID.randomUUID().toString()
        val rows = entries.map {
            ClipboardItem(
                batchId = batchId,
                path = it.path,
                name = it.name,
                isDirectory = it.isDirectory,
                srcParentPath = srcParentPath,
                engineType = engineType.name,
                mode = mode.name
            )
        }
        dao(context).clear()
        dao(context).addAll(rows)
        _state.value = ClipboardState(rows, mode)
    }

    suspend fun clear(context: Context) {
        dao(context).clear()
        _state.value = ClipboardState()
    }

    /**
     * Pastes every clipboard entry into [destDirPath] using [engine].
     * Only meaningful when the clipboard's engineType matches [engine];
     * cross-engine paste (e.g. Root source -> SAF destination) isn't
     * supported by the underlying FileEngine.copy/move contract, so the
     * caller should check [ClipboardState] against the active engine
     * before offering "Paste" in the UI.
     */
    suspend fun paste(context: Context, engine: FileEngine, destDirPath: String): List<Pair<String, Throwable>> {
        val current = _state.value
        val failures = mutableListOf<Pair<String, Throwable>>()
        for (item in current.items) {
            val entry = FileEntry(
                name = item.name,
                path = item.path,
                isDirectory = item.isDirectory,
                isSymlink = false,
                sizeBytes = 0,
                permissions = "",
                owner = "",
                group = "",
                modified = ""
            )
            val result = if (current.mode == ClipMode.CUT) {
                engine.move(entry, item.srcParentPath, destDirPath)
            } else {
                engine.copy(entry, item.srcParentPath, destDirPath)
            }
            result.onFailure { failures.add(item.name to it) }
        }
        if (current.mode == ClipMode.CUT && failures.isEmpty()) {
            clear(context)
        }
        return failures
    }
}
