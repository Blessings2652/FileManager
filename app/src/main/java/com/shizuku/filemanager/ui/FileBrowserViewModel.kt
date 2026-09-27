package com.shizuku.filemanager.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shizuku.filemanager.fs.FileEntry
import com.shizuku.filemanager.fs.engine.EnginePrefs
import com.shizuku.filemanager.fs.engine.FileEngine
import com.shizuku.filemanager.sys.EventLogger
import com.shizuku.filemanager.sys.FolderWatcherService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

sealed class ClipboardOp {
    data class Copy(val entries: Set<FileEntry>, val parentPath: String) : ClipboardOp()
    data class Cut(val entries: Set<FileEntry>, val parentPath: String) : ClipboardOp()
}

enum class SortBy { Name, Date, Size, Type }
enum class SortOrder { Ascending, Descending }

data class BrowserUiState(
    val currentPath: String = "",
    val entries: List<FileEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val clipboard: ClipboardOp? = null,
    val selected: FileEntry? = null,
    val selectedItems: Set<FileEntry> = emptySet(),
    val isSelectionMode: Boolean = false,
    val pendingDelete: FileEntry? = null,
    val pendingRename: FileEntry? = null,
    val showNewFolderDialog: Boolean = false,
    val showNewFileDialog: Boolean = false,
    val extractionProgress: Float? = null,
    val opError: String? = null,
    val showDetails: FileEntry? = null,
    val detailedSize: Long? = null,
    val settingsVersion: Int = 0,
    val pendingBulkDelete: Set<FileEntry>? = null,
    // Search and Sort
    val searchQuery: String = "",
    val showSearch: Boolean = false,
    val sortBy: SortBy = SortBy.Name,
    val sortOrder: SortOrder = SortOrder.Ascending,
    // Settings mirrored for performance
    val viewMode: String = "List",
    val thumbnailsEnabled: Boolean = true,
    val showExtensions: Boolean = true,
    val singleClick: Boolean = true,
    val itemSizeMultiplier: Float = 1.0f,
    val fontSizeMultiplier: Float = 1.0f,
    val folderCount: Int = 0,
    val fileCount: Int = 0,
    val totalFilesSize: Long = 0L,
    val isDocumentMode: Boolean = false,
    val restrictedPath: String? = null,
)

class FileBrowserViewModel(
    application: Application,
    private val engine: FileEngine,
    initialPath: String? = null,
    private val forceShowHidden: Boolean = false,
    private val lockedRoot: String? = null,
    val singleClick: Boolean = true,
    private val isDocumentMode: Boolean = false,
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()

    private val _state = MutableStateFlow(BrowserUiState(
        currentPath = initialPath ?: engine.rootPath,
        isDocumentMode = isDocumentMode
    ))
    val state: StateFlow<BrowserUiState> = _state

    init {
        loadSettings()
        refresh()
        viewModelScope.launch {
            EnginePrefs.settingsChanged.collect {
                onSettingsChanged()
            }
        }
        
        // Reactive refresh based on file system events
        viewModelScope.launch {
            EventLogger.eventsFlow.collect { event ->
                val current = _state.value.currentPath
                if (event.path.startsWith(current) && !_state.value.isLoading) {
                    // Slight delay to avoid multiple refreshes for bulk operations
                    delay(200.milliseconds)
                    refresh(silent = true)
                }
            }
        }

        // Ensure service is watching current path
        updateWatcher(_state.value.currentPath)
    }

    private fun updateWatcher(path: String) {
        try {
            val intent = Intent(context, FolderWatcherService::class.java).apply {
                putExtra("path", path)
            }
            context.startForegroundService(intent)
        } catch (_: Exception) {}
    }

    private fun loadSettings() {
        _state.update { 
            it.copy(
                viewMode = EnginePrefs.getViewMode(context),
                thumbnailsEnabled = EnginePrefs.isThumbnailsEnabled(context),
                showExtensions = EnginePrefs.isShowExtensions(context),
                singleClick = EnginePrefs.isSingleClickToOpen(context),
                itemSizeMultiplier = EnginePrefs.getItemSizeMultiplier(context),
                fontSizeMultiplier = EnginePrefs.getFontSizeMultiplier(context),
                sortBy = try { SortBy.valueOf(EnginePrefs.getSortBy(context)) } catch (e: Exception) { SortBy.Name },
                sortOrder = try { SortOrder.valueOf(EnginePrefs.getSortOrder(context)) } catch (e: Exception) { SortOrder.Ascending }
            )
        }
    }

    fun onSettingsChanged() {
        loadSettings()
        _state.update { it.copy(settingsVersion = it.settingsVersion + 1) }
        refresh(silent = false)
    }

    private var searchJob: kotlinx.coroutines.Job? = null
    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300.milliseconds)
            refresh(silent = true)
        }
    }

    fun toggleSearch() {
        _state.update { 
            val newShow = !it.showSearch
            it.copy(
                showSearch = newShow,
                searchQuery = if (newShow) it.searchQuery else ""
            )
        }
        if (!_state.value.showSearch) refresh(silent = true)
    }

    fun setSortBy(sortBy: SortBy) {
        EnginePrefs.setSortBy(context, sortBy.name)
        onSettingsChanged()
    }

    fun setSortOrder(order: SortOrder) {
        EnginePrefs.setSortOrder(context, order.name)
        onSettingsChanged()
    }

    fun refresh(silent: Boolean = false) {
        val path = _state.value.currentPath
        val showHidden = forceShowHidden || EnginePrefs.isShowHidden(context)
        val highPriority = EnginePrefs.isHighPriority(context)
        val query = _state.value.searchQuery
        val sortBy = _state.value.sortBy
        val sortOrder = _state.value.sortOrder
        
        if (!silent) {
            _state.update { it.copy(isLoading = true, error = null) }
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            if (highPriority) {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_FOREGROUND)
            }
            
            engine.list(path).fold(
                onSuccess = { allEntries ->
                    var filtered = if (showHidden) allEntries else allEntries.filter { !it.name.startsWith(".") }
                    
                    if (query.isNotBlank()) {
                        filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
                    }

                    val sorted = when (sortBy) {
                        SortBy.Name -> filtered.sortedBy { it.name.lowercase() }
                        SortBy.Date -> filtered.sortedBy { it.modified }
                        SortBy.Size -> filtered.sortedBy { it.sizeBytes }
                        SortBy.Type -> filtered.sortedBy { if (it.isDirectory) " " else it.extension.lowercase() }
                    }

                    val finalEntries = if (sortOrder == SortOrder.Descending) sorted.reversed() else sorted
                    
                    val folders = finalEntries.count { it.isDirectory }
                    val files = finalEntries.count { !it.isDirectory }
                    val size = finalEntries.filter { !it.isDirectory }.sumOf { it.sizeBytes }

                    // Chunked loading for better perceived performance in large folders
                    if (finalEntries.size > 100 && query.isBlank()) {
                        val firstChunk = finalEntries.take(100)
                        _state.update { it.copy(
                            entries = firstChunk,
                            isLoading = true,
                            folderCount = folders,
                            fileCount = files,
                            totalFilesSize = size
                        ) }
                        delay(16.milliseconds) // Brief pause to allow UI thread to breathe
                        _state.update { it.copy(entries = finalEntries, isLoading = false) }
                    } else {
                        _state.update { it.copy(
                            entries = finalEntries, 
                            isLoading = false,
                            folderCount = folders,
                            fileCount = files,
                            totalFilesSize = size,
                            restrictedPath = null
                        ) }
                    }

                    // Background thumbnail pre-fetching
                    if (_state.value.thumbnailsEnabled) {
                        com.shizuku.filemanager.sys.ThumbnailManager.preloadThumbnails(context, finalEntries, engine.type)
                    }
                },
                onFailure = { e ->
                    if (e is com.shizuku.filemanager.fs.engine.RestrictedAccessException) {
                        _state.update { it.copy(isLoading = false, restrictedPath = e.path, error = null) }
                    } else {
                        _state.update { it.copy(isLoading = false, error = e.message, restrictedPath = null) }
                    }
                }
            )
        }
    }

    fun navigateTo(path: String) {
        // Prevent navigation outside locked root if set
        if (lockedRoot != null && (path != lockedRoot) && !path.startsWith("$lockedRoot/")) {
            return
        }
        _state.update { it.copy(currentPath = path) }
        updateWatcher(path)
        refresh()
    }

    fun navigateUp(): Boolean {
        if (_state.value.currentPath == lockedRoot) return false
        val parent = engine.parentPath(_state.value.currentPath) ?: return false
        navigateTo(parent)
        return true
    }

    val canNavigateUp: Boolean
        get() = _state.value.currentPath != lockedRoot && engine.parentPath(_state.value.currentPath) != null

    fun openEntry(entry: FileEntry) {
        if (entry.isDirectory || entry.isSymlink) {
            navigateTo(entry.path)
        } else {
            _state.update { it.copy(selected = entry) }
        }
    }

    fun clearSelection() = _state.update { it.copy(selected = null, selectedItems = emptySet(), isSelectionMode = false) }

    fun toggleSelection(entry: FileEntry) {
        _state.update { 
            val newSelection = if (it.selectedItems.contains(entry)) {
                it.selectedItems - entry
            } else {
                it.selectedItems + entry
            }
            it.copy(
                selectedItems = newSelection,
                isSelectionMode = newSelection.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _state.update { 
            it.copy(
                selectedItems = it.entries.toSet(),
                isSelectionMode = it.entries.isNotEmpty()
            )
        }
    }
    
    fun showDetails(entry: FileEntry) {
        _state.update { it.copy(showDetails = entry, detailedSize = if (entry.isDirectory) null else entry.sizeBytes) }
        if (entry.isDirectory && engine.type == com.shizuku.filemanager.fs.engine.EngineType.STANDARD) {
            val highPriority = EnginePrefs.isHighPriority(context)
            viewModelScope.launch(Dispatchers.IO) {
                if (highPriority) {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
                }
                val size = com.shizuku.filemanager.fs.engine.FileUtils.getRecursiveSize(java.io.File(entry.path))
                _state.update { it.copy(detailedSize = size) }
            }
        }
    }
    
    fun dismissDetails() = _state.update { it.copy(showDetails = null, detailedSize = null) }

    fun dismissOpError() = _state.update { it.copy(opError = null) }

    fun requestDelete(entry: FileEntry) = _state.update { it.copy(pendingDelete = entry) }
    fun requestBulkDelete(entries: Set<FileEntry>) = _state.update { it.copy(pendingBulkDelete = entries) }
    fun cancelDelete() = _state.update { it.copy(pendingDelete = null, pendingBulkDelete = null) }
    
    fun directDelete(entry: FileEntry) {
        viewModelScope.launch {
            val result = moveToTrash(entry)
            result.onFailure { e -> _state.update { it.copy(opError = e.message) } }
            refresh()
        }
    }

    fun confirmDelete() {
        val target = _state.value.pendingDelete
        val bulkTarget = _state.value.pendingBulkDelete
        
        viewModelScope.launch {
            if (target != null) {
                val result = moveToTrash(target)
                result.onFailure { e -> _state.update { it.copy(opError = e.message) } }
            } else if (bulkTarget != null) {
                var failed = 0
                bulkTarget.forEach { entry ->
                    val result = moveToTrash(entry)
                    if (result.isFailure) failed++
                }
                if (failed > 0) {
                    _state.update { it.copy(opError = "Failed to move $failed items to trash") }
                }
            }
            
            _state.update { it.copy(pendingDelete = null, pendingBulkDelete = null, selectedItems = emptySet(), isSelectionMode = false) }
            refresh()
        }
    }

    private suspend fun moveToTrash(entry: FileEntry): Result<Unit> {
        val trashResult = engine.mkdir(engine.rootPath, ".trash")
        return trashResult.fold(
            onSuccess = { trashPath ->
                val parentPath = engine.parentPath(entry.path) ?: engine.rootPath
                val uniqueName = getUniqueTrashName(entry.name)
                
                // First rename in place to ensure a unique name in the trash
                engine.rename(entry, uniqueName).fold(
                    onSuccess = {
                        val renamedEntry = entry.copy(
                            name = uniqueName,
                            path = engine.getChildPath(parentPath, uniqueName)
                        )
                        engine.move(renamedEntry, parentPath, trashPath)
                    },
                    onFailure = { Result.failure(it) }
                )
            },
            onFailure = { Result.failure(it) }
        )
    }

    fun moveToSafe(entry: FileEntry) {
        viewModelScope.launch {
            engine.mkdir(engine.rootPath, ".safe").onSuccess { safePath ->
                val parentPath = engine.parentPath(entry.path) ?: engine.rootPath
                engine.move(entry, parentPath, safePath).onFailure { e ->
                    _state.update { it.copy(opError = e.message) }
                }
                refresh()
            }.onFailure { e ->
                _state.update { it.copy(opError = "Could not access Safe Folder: ${e.message}") }
            }
        }
    }

    fun moveFromSafe(entry: FileEntry) {
        viewModelScope.launch {
            val destPath = engine.rootPath
            val parentPath = engine.parentPath(entry.path) ?: return@launch
            engine.move(entry, parentPath, destPath).onFailure { e ->
                _state.update { it.copy(opError = e.message) }
            }
            refresh()
        }
    }

    private fun getUniqueTrashName(name: String): String {
        val timestamp = System.currentTimeMillis()
        val dotIndex = name.lastIndexOf('.')
        return if (dotIndex > 0) {
            "${name.substring(0, dotIndex)}_$timestamp${name.substring(dotIndex)}"
        } else {
            "${name}_$timestamp"
        }
    }

    fun requestRename(entry: FileEntry) = _state.update { it.copy(pendingRename = entry) }
    fun cancelRename() = _state.update { it.copy(pendingRename = null) }
    fun confirmRename(newName: String) {
        val target = _state.value.pendingRename ?: return
        viewModelScope.launch {
            engine.rename(target, newName).onFailure { e -> _state.update { it.copy(opError = e.message) } }
            _state.update { it.copy(pendingRename = null) }
            refresh()
        }
    }

    fun copyToClipboard(entries: Set<FileEntry>) =
        _state.update { it.copy(clipboard = ClipboardOp.Copy(entries, _state.value.currentPath), isSelectionMode = false) }

    fun cutToClipboard(entries: Set<FileEntry>) =
        _state.update { it.copy(clipboard = ClipboardOp.Cut(entries, _state.value.currentPath), isSelectionMode = false) }

    fun pasteHere() {
        val op = _state.value.clipboard ?: return
        val dest = _state.value.currentPath
        viewModelScope.launch {
            var failed = 0
            val entries = when (op) {
                is ClipboardOp.Copy -> op.entries
                is ClipboardOp.Cut -> op.entries
            }
            
            entries.forEach { entry ->
                val result = when (op) {
                    is ClipboardOp.Copy -> engine.copy(entry, op.parentPath, dest)
                    is ClipboardOp.Cut -> engine.move(entry, op.parentPath, dest)
                }
                if (result.isFailure) failed++
            }

            if (failed > 0) {
                _state.update { it.copy(opError = "Failed to paste $failed items") }
            }
            
            _state.update { it.copy(clipboard = null) }
            refresh()
        }
    }

    fun showNewFolderDialog() = _state.update { it.copy(showNewFolderDialog = true) }
    fun dismissNewFolderDialog() = _state.update { it.copy(showNewFolderDialog = false) }

    fun showNewFileDialog() = _state.update { it.copy(showNewFileDialog = true) }
    fun dismissNewFileDialog() = _state.update { it.copy(showNewFileDialog = false) }

    fun createArchive(
        name: String,
        level: Int = 6,
        format: String = "zip",
        password: String? = null
    ) {
        val targets = _state.value.selectedItems.ifEmpty { 
            _state.value.selected?.let { setOf(it) } ?: emptySet()
        }.map { java.io.File(it.path) }
        
        if (targets.isEmpty()) return
        
        val extension = when(format.lowercase()) {
            "7z" -> ".7z"
            "tar" -> ".tar"
            else -> ".zip"
        }
        
        val finalName = if (name.endsWith(extension)) name else "$name$extension"
        val zipFile = java.io.File(_state.value.currentPath, finalName)
        
        _state.update { it.copy(extractionProgress = 0f) }
        viewModelScope.launch {
            // For now, we only have ZIP implementation. 
            // In a real app, we'd use a library for 7z/Password.
            com.shizuku.filemanager.sys.ArchiveManager.createZip(targets, zipFile, level) { progress ->
                _state.update { it.copy(extractionProgress = progress) }
            }.fold(
                onSuccess = {
                    _state.update { it.copy(extractionProgress = null, selectedItems = emptySet(), isSelectionMode = false) }
                    refresh()
                },
                onFailure = { e ->
                    _state.update { it.copy(extractionProgress = null, opError = e.message) }
                }
            )
        }
    }

    fun bulkRename(pattern: String, replacement: String) {
        val targets = _state.value.selectedItems.map { java.io.File(it.path) }
        if (targets.isEmpty()) return
        
        viewModelScope.launch {
            com.shizuku.filemanager.sys.AdvancedFileOps.bulkRename(targets, pattern, replacement).fold(
                onSuccess = { count ->
                    _state.update { it.copy(opError = "Renamed $count files", selectedItems = emptySet(), isSelectionMode = false) }
                    refresh()
                },
                onFailure = { e ->
                    _state.update { it.copy(opError = e.message) }
                }
            )
        }
    }
    fun createFolder(name: String) {
        val parent = _state.value.currentPath
        viewModelScope.launch {
            engine.mkdir(parent, name).onFailure { e -> _state.update { it.copy(opError = e.message) } }
            dismissNewFolderDialog()
            refresh()
        }
    }

    fun createFile(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val finalName = if (trimmed.endsWith(".txt", ignoreCase = true)) trimmed else "$trimmed.txt"
        val parent = _state.value.currentPath
        viewModelScope.launch {
            engine.createFile(parent, finalName).onSuccess {
                // Optionally open the file after creation
            }.onFailure { e -> 
                _state.update { it.copy(opError = e.message) } 
            }
            dismissNewFileDialog()
            refresh()
        }
    }

    override fun onCleared() {
        try {
            val intent = Intent(context, FolderWatcherService::class.java).apply {
                action = "STOP_WATCHING"
                putExtra("path", _state.value.currentPath)
            }
            context.startService(intent)
        } catch (_: Exception) {}
    }

    fun extractZip(entry: FileEntry) {
        val parent = engine.parentPath(entry.path) ?: return
        val outputName = entry.name.substringBeforeLast(".") + "_extracted"
        val outputDir = engine.getChildPath(parent, outputName)
        
        _state.update { it.copy(extractionProgress = 0f) }
        viewModelScope.launch(Dispatchers.IO) {
            com.shizuku.filemanager.sys.ArchiveManager.extractZip(entry.path, outputDir) { progress ->
                _state.update { it.copy(extractionProgress = progress) }
            }.fold(
                onSuccess = {
                    _state.update { it.copy(extractionProgress = null) }
                    refresh()
                },
                onFailure = { e ->
                    _state.update { it.copy(extractionProgress = null, opError = e.message) }
                }
            )
        }
    }

    class Factory(
        private val application: Application,
        private val engine: FileEngine,
        private val initialPath: String? = null,
        private val forceShowHidden: Boolean = false,
        private val lockedRoot: String? = null,
        private val isDocumentMode: Boolean = false
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FileBrowserViewModel(application, engine, initialPath, forceShowHidden, lockedRoot, isDocumentMode = isDocumentMode) as T
        }
    }
}
