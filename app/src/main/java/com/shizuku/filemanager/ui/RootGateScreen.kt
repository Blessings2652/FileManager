package com.shizuku.filemanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun RootGateScreen(
    checking: Boolean,
    granted: Boolean,
    onRetry: () -> Unit,
    onPickDifferentEngine: () -> Unit
) {
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
                        Icons.Filled.AdminPanelSettings, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            when {
                checking -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Requesting root access…", 
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), 
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Approve the prompt from your root manager (Magisk, etc.) if one appears.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                !granted -> {
                    Text(
                        "Root access denied", 
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), 
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "su either isn't available or the request was denied. Grant root to this app in your root manager, then retry.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(48.dp))
                    GradientButton(
                        text = "Retry",
                        onClick = onRetry
                    )
                    Spacer(Modifier.height(16.dp))
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
    }
}
