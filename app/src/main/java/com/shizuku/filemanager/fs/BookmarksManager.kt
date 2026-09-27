package com.shizuku.filemanager.fs

import android.content.Context
import com.shizuku.filemanager.db.Bookmark
import com.shizuku.filemanager.db.ExtrasDatabase
import com.shizuku.filemanager.fs.engine.EngineType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pinned folders, persisted across sessions. Shown in the drawer above
 * the raw engine tree so frequently-used locations are one tap away
 * regardless of how deep they are nested.
 */
object BookmarksManager {

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks = _bookmarks.asStateFlow()

    private fun dao(context: Context) = ExtrasDatabase.getDatabase(context.applicationContext).bookmarkDao()

    suspend fun refresh(context: Context) {
        _bookmarks.value = dao(context).getAll()
    }

    suspend fun add(context: Context, path: String, label: String, engineType: EngineType) {
        dao(context).add(Bookmark(path = path, label = label, engineType = engineType.name))
        refresh(context)
    }

    suspend fun remove(context: Context, path: String, engineType: EngineType) {
        dao(context).remove(path, engineType.name)
        refresh(context)
    }

    suspend fun toggle(context: Context, path: String, label: String, engineType: EngineType): Boolean {
        val d = dao(context)
        val already = d.isBookmarked(path, engineType.name) > 0
        if (already) {
            d.remove(path, engineType.name)
        } else {
            d.add(Bookmark(path = path, label = label, engineType = engineType.name))
        }
        refresh(context)
        return !already
    }

    suspend fun isBookmarked(context: Context, path: String, engineType: EngineType): Boolean {
        return dao(context).isBookmarked(path, engineType.name) > 0
    }
}
