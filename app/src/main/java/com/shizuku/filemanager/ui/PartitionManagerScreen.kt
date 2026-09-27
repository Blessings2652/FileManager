package com.shizuku.filemanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shizuku.filemanager.sys.PartitionManager
import kotlinx.coroutines.launch

/**
 * Root-only: lists /proc/partitions block devices and /proc/mounts mount
 * points side by side, with a guarded remount action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartitionManagerScreen(onBack: () -> Unit) {
    var devices by remember { mutableStateOf<List<PartitionManager.BlockDevice>>(emptyList()) }
    var mounts by remember { mutableStateOf<List<PartitionManager.MountPoint>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showTab by remember { mutableIntStateOf(0) } // 0 = mounts, 1 = block devices
    var remountTarget by remember { mutableStateOf<PartitionManager.MountPoint?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        PartitionManager.listMounts().onSuccess { mounts = it }.onFailure { error = it.message }
        PartitionManager.listBlockDevices().onSuccess { devices = it }.onFailure { error = it.message }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Partitions & Mounts") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = showTab) {
                Tab(selected = showTab == 0, onClick = { showTab = 0 }, text = { Text("Mounts (${mounts.size})") })
                Tab(selected = showTab == 1, onClick = { showTab = 1 }, text = { Text("Block devices (${devices.size})") })
            }
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            if (showTab == 0) {
                LazyColumn {
                    items(mounts) { m ->
                        ListItem(
                            headlineContent = { Text(m.mountPath) },
                            supportingContent = { Text("${m.device} · ${m.fsType} · ${m.options}", maxLines = 1) },
                            trailingContent = {
                                TextButton(onClick = { remountTarget = m }) { Text("Remount") }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            } else {
                LazyColumn {
                    items(devices) { d ->
                        ListItem(
                            headlineContent = { Text(d.name) },
                            supportingContent = { Text("${d.path} · ${d.majorMinor} · ${d.sizeBlocks} blocks") },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    remountTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { remountTarget = null },
            title = { Text("Remount ${target.mountPath}?") },
            text = { Text("This directly affects a live mount point. Only proceed if you know what this partition is for.") },
            confirmButton = {
                TextButton(onClick = {
                    val rw = !target.options.contains("rw")
                    scope.launch {
                        PartitionManager.remount(target.mountPath, rw).onFailure { error = it.message }
                        refresh()
                        remountTarget = null
                    }
                }) { Text(if (target.options.contains("rw")) "Remount read-only" else "Remount read-write") }
            },
            dismissButton = { TextButton(onClick = { remountTarget = null }) { Text("Cancel") } }
        )
    }
}
