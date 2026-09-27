package com.shizuku.filemanager.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizuku.filemanager.sys.ZipEntryInfo
import com.shizuku.filemanager.sys.ArchiveManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ZipUiState(
    val zipPath: String = "",
    val currentPath: String = "",
    val allEntries: List<ZipEntryInfo> = emptyList(),
    val filteredEntries: List<ZipEntryInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isExtracting: Boolean = false,
    val error: String? = null
)

class ZipViewerViewModel(val zipPath: String) : ViewModel() {
    private val _state = MutableStateFlow(ZipUiState(zipPath = zipPath))
    val state: StateFlow<ZipUiState> = _state

    init {
        loadEntries()
    }

    private fun loadEntries() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            ArchiveManager.listZipEntries(zipPath).fold(
                onSuccess = { entries ->
                    _state.update { it.copy(allEntries = entries, isLoading = false) }
                    updateFilteredEntries("")
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun navigateTo(path: String) {
        _state.update { it.copy(currentPath = path) }
        updateFilteredEntries(path)
    }

    fun navigateUp(): Boolean {
        val current = _state.value.currentPath
        if (current.isEmpty()) return false
        
        val parent = if (current.endsWith("/")) {
            current.substring(0, current.length - 1).substringBeforeLast("/", "")
        } else {
            current.substringBeforeLast("/", "")
        }
        
        val newPath = if (parent.isEmpty()) "" else "$parent/"
        navigateTo(newPath)
        return true
    }

    private fun updateFilteredEntries(path: String) {
        val entries = _state.value.allEntries
        
        // Zip entries are like "folder/file.txt" or "folder/"
        val refined = entries.filter { it.name.startsWith(path) && it.name != path }
            .map { entry ->
                val relative = entry.name.substring(path.length)
                val firstSlash = relative.indexOf('/')
                if (firstSlash == -1) {
                    entry
                } else {
                    // It's a directory (or inside one)
                    val dirName = path + relative.substring(0, firstSlash + 1)
                    ZipEntryInfo(dirName, 0, 0, true, entry.time)
                }
            }.distinctBy { it.name }
            .sortedWith(compareByDescending<ZipEntryInfo> { it.isDirectory }.thenBy { it.name })

        _state.update { it.copy(filteredEntries = refined) }
    }

    fun extractAndOpenFile(context: Context, entry: ZipEntryInfo, onReady: (File) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isExtracting = true) }
            val tempDir = File(context.cacheDir, "zip_temp")
            if (tempDir.exists()) tempDir.deleteRecursively()
            tempDir.mkdirs()
            
            val outputFile = File(tempDir, entry.name.substringAfterLast('/'))
            ArchiveManager.extractEntry(zipPath, entry.name, outputFile).fold(
                onSuccess = {
                    _state.update { it.copy(isExtracting = false) }
                    onReady(outputFile)
                },
                onFailure = { e ->
                    _state.update { it.copy(isExtracting = false, error = e.message) }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZipViewerScreen(
    zipPath: String,
    onBack: () -> Unit,
    onOpenFile: (File) -> Unit
) {
    val viewModel = remember(zipPath) { ZipViewerViewModel(zipPath) }
    val state by viewModel.state.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
    val context = LocalContext.current

    BackHandler {
        if (!viewModel.navigateUp()) {
            onBack()
        }
    }

    ScreenScaffold(
        topBar = {
            ModernTopBar(
                title = zipPath.substringAfterLast("/"),
                navigationIcon = {
                    IconButton(onClick = { if (!viewModel.navigateUp()) onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
                state.error != null -> Text(state.error!!, Modifier.align(Alignment.Center).padding(24.dp), color = MaterialTheme.colorScheme.error)
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.filteredEntries) { entry ->
                            SectionCard {
                                ZipEntryRow(
                                    entry = entry,
                                    dateFormat = dateFormat,
                                    onClick = {
                                        if (entry.isDirectory) {
                                            viewModel.navigateTo(entry.name)
                                        } else {
                                            viewModel.extractAndOpenFile(context, entry) { file ->
                                                onOpenFile(file)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            if (state.isExtracting) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(16.dp))
                            Text("Extracting...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZipEntryRow(
    entry: ZipEntryInfo,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    val name = if (entry.isDirectory) {
        entry.name.trimEnd('/').substringAfterLast('/')
    } else {
        entry.name.substringAfterLast('/')
    }

    ModernListItem(
        title = name,
        subtitle = if (!entry.isDirectory) {
            "${formatSize(entry.size)} · ${dateFormat.format(Date(entry.time))}"
        } else {
            dateFormat.format(Date(entry.time))
        },
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = if (entry.isDirectory) Icons.Filled.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
                tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    )
}
