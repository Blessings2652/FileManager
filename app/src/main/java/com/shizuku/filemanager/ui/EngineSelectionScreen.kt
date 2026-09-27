package com.shizuku.filemanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shizuku.filemanager.fs.RootManager
import com.shizuku.filemanager.fs.engine.EnginePrefs
import com.shizuku.filemanager.fs.engine.EngineType

@Composable
fun EngineSelectionScreen(onSelect: (EngineType) -> Unit) {
    val context = LocalContext.current
    val isRootPresent = remember { RootManager.isSuBinaryPresent() }
    
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Spacer(Modifier.height(32.dp))
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Welcome to FileManager",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Select how you'd like to access your files. This determines the level of system access.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                val availableEngines = if (isRootPresent) EngineType.entries else EngineType.entries.filter { it != EngineType.ROOT }
                items(availableEngines) { type ->
                    EngineOptionCard(type = type, onClick = { onSelect(type) })
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSelect(EnginePrefs.detectBestEngine(context)) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Auto-detect (Recommended)")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EngineOptionCard(type: EngineType, onClick: () -> Unit) {
    val containerColor = when (type) {
        EngineType.SHIZUKU -> MaterialTheme.colorScheme.primaryContainer
        EngineType.ROOT -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    
    val contentColor = when (type) {
        EngineType.SHIZUKU -> MaterialTheme.colorScheme.onPrimaryContainer
        EngineType.ROOT -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (type) {
                        EngineType.SHIZUKU -> Icons.Filled.Code
                        EngineType.ROOT -> Icons.Filled.AdminPanelSettings
                        EngineType.SAF -> Icons.Filled.Folder
                        EngineType.STANDARD -> Icons.Filled.Storage
                    },
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = contentColor
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    type.label,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
            }
        }
    }
}
