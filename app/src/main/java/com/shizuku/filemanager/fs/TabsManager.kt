package com.shizuku.filemanager.fs

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.shizuku.filemanager.fs.engine.EngineType
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    var path: String,
    val engineType: EngineType,
    var label: String,
)

/**
 * In-memory (not persisted across process death — like most file
 * managers' tabs) list of open browser tabs. FileBrowserScreen keeps
 * owning its own navigation/back-stack per tab; this only tracks which
 * tab is active and what its "current folder" is, updated as the user
 * navigates within a tab so switching back restores the right spot.
 */
object TabsManager {

    val tabs = mutableStateListOf<BrowserTab>()
    val activeTabId = mutableStateOf<String?>(null)

    fun openTab(path: String, engineType: EngineType, label: String = path.substringAfterLast('/', path)): BrowserTab {
        val tab = BrowserTab(path = path, engineType = engineType, label = label.ifBlank { "/" })
        tabs.add(tab)
        activeTabId.value = tab.id
        return tab
    }

    fun closeTab(id: String) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index == -1) return
        tabs.removeAt(index)
        if (activeTabId.value == id) {
            activeTabId.value = tabs.getOrNull(index.coerceAtMost(tabs.size - 1))?.id
        }
    }

    fun setActive(id: String) {
        if (tabs.any { it.id == id }) activeTabId.value = id
    }

    fun updatePath(id: String, path: String, label: String) {
        tabs.find { it.id == id }?.let {
            it.path = path
            it.label = label.ifBlank { "/" }
        }
    }

    fun activeTab(): BrowserTab? = tabs.find { it.id == activeTabId.value }

    /** Ensures there is always at least one tab (call once at app start with the initial browser path/engine). */
    fun ensureAtLeastOne(path: String, engineType: EngineType) {
        if (tabs.isEmpty()) openTab(path, engineType)
    }
}
