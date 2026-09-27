package com.shizuku.filemanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shizuku.filemanager.shizuku.ShizukuManager

@Composable
fun ShizukuGateScreen(
    isAvailable: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onPickDifferentEngine: () -> Unit
) {
    val context = LocalContext.current
    val isInstalled = remember { ShizukuManager.isAppInstalled(context) }

    ScreenScaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.FolderOpen, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            
            val titleText = when {
                !isAvailable && isInstalled -> "Shizuku is installed"
                !isAvailable -> "Shizuku isn't running"
                !hasPermission -> "Shizuku permission needed"
                else -> "Shizuku Ready"
            }

            val descText = when {
                !isAvailable && isInstalled -> "Shizuku is detected on your mobile device! Please start the Shizuku service to unlock full access to system storage and restricted folders."
                !isAvailable -> "Start Shizuku (via Wireless Debugging or Root) from the Shizuku app, then come back here."
                !hasPermission -> "This app needs Shizuku permission to browse and manage files as the shell user."
                else -> "Shizuku is active and ready."
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = descText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(48.dp))

            if (!isAvailable && isInstalled) {
                GradientButton(
                    text = "Start Shizuku Service",
                    onClick = { ShizukuManager.launchShizukuApp(context) }
                )
                Spacer(Modifier.height(16.dp))
            } else if (isAvailable && !hasPermission) {
                GradientButton(
                    text = "Grant permission",
                    onClick = onRequestPermission
                )
                Spacer(Modifier.height(16.dp))
            }
            
            TextButton(
                onClick = onPickDifferentEngine,
                modifier = Modifier.fillMaxWidth()
            ) { 
                Text(
                    "Use a different access method",
                    style = MaterialTheme.typography.labelLarge
                ) 
            }
        }
    }
}
