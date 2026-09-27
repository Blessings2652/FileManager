package com.shizuku.filemanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shizuku.filemanager.fs.FileEntry
import java.io.File
import java.io.RandomAccessFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileRepairScreen(
    entry: FileEntry,
    onBack: () -> Unit
) {
    var resultMessage by remember { mutableStateOf("") }
    var isRepairing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Repair Center") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Build, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(entry.name, style = MaterialTheme.typography.titleMedium)
            Text(entry.path, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(32.dp))
            
            if (isRepairing) {
                CircularProgressIndicator()
            } else {
                Button(onClick = {
                    isRepairing = true
                    // Mock repair logic for headers
                    val file = File(entry.path)
                    try {
                        if (entry.name.endsWith(".jpg", true)) {
                            RandomAccessFile(file, "rw").use { raf ->
                                raf.seek(0)
                                raf.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))
                            }
                            resultMessage = "JPEG Header Repaired."
                        } else {
                            resultMessage = "Repair not supported for this type."
                        }
                    } catch (e: Exception) {
                        resultMessage = "Repair failed: ${e.message}"
                    }
                    isRepairing = false
                }) {
                    Text("Attempt Header Repair")
                }
            }
            
            if (resultMessage.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(resultMessage, color = if (resultMessage.contains("failed")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        }
    }
}
