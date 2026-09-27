package com.shizuku.filemanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shizuku.filemanager.fs.FileEntry
import com.shizuku.filemanager.fs.engine.FileEngine
import com.shizuku.filemanager.sys.AppManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    engine: FileEngine,
    onBack: () -> Unit,
    onOpenFile: (FileEntry, FileEngine) -> Unit
) {
    var trashPath by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(engine) {
        engine.mkdir(engine.rootPath, ".trash").onSuccess {
            trashPath = it
            error = null
        }.onFailure {
            error = it.message ?: "Could not create trash folder"
        }
    }

    if (error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: $error", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
        }
        return
    }

    if (trashPath == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    TrashList(engine, trashPath!!, onBack, onOpenFile = onOpenFile)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashList(
    engine: FileEngine,
    trashPath: String,
    onBack: () -> Unit,
    onOpenFile: (FileEntry, FileEngine) -> Unit,
    vm: FileBrowserViewModel = viewModel(
        key = "trash_" + engine.type.name + trashPath,
        factory = FileBrowserViewModel.Factory(
            LocalContext.current.applicationContext as android.app.Application,
            engine,
            trashPath,
            forceShowHidden = true
        )
    )
) {
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(trashPath) {
        vm.refresh()
    }

    ScreenScaffold(
        topBar = {
            ModernTopBar(
                title = "Trash Bin",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.entries.isNotEmpty()) {
                        IconButton(onClick = { 
                            scope.launch {
                                for (entry in state.entries) {
                                    engine.delete(entry)
                                }
                                vm.refresh()
                            }
                        }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Empty Trash")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
                state.entries.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.DeleteSweep, 
                        null, 
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Trash is empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.entries) { entry ->
                        TrashItem(entry, engine, trashPath, vm)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrashItem(
    entry: FileEntry,
    engine: FileEngine,
    trashPath: String,
    vm: FileBrowserViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apkIcon by remember(entry.path) { mutableStateOf<android.graphics.drawable.Drawable?>(null) }

    LaunchedEffect(entry.path) {
        if (entry.name.lowercase().endsWith(".apk")) {
            apkIcon = AppManager.getApkIcon(context, entry.path)
        }
    }

    ModernListItem(
        title = entry.name,
        subtitle = entry.modified,
        onClick = { /* Could open preview */ },
        leadingIcon = {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (apkIcon != null) {
                        androidx.compose.foundation.Image(
                            bitmap = apkIcon!!.toBitmap().asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.InsertDriveFile, 
                            null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        trailingContent = {
            IconButton(onClick = {
                scope.launch {
                    val parent = engine.rootPath // Restore to root for now
                    engine.move(entry, trashPath, parent)
                    vm.refresh()
                }
            }) {
                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
            }
        }
    )
}
