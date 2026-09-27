package com.shizuku.filemanager.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shizuku.filemanager.sys.UpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateDialogInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    var showUpToDateDialog by remember { mutableStateOf(false) }

    ScreenScaffold(
        topBar = {
            ModernTopBar(
                title = "About App",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "FileManager Pro",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Version 1.0.0 (Build 1)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Fast, Private & Powerful Android File Manager",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Check for Updates via Appteka Store & GitHub
            Card(
                onClick = {
                    if (!isCheckingUpdate) {
                        scope.launch {
                            isCheckingUpdate = true
                            val result = UpdateManager.checkForUpdates("1.0")
                            isCheckingUpdate = false
                            result.onSuccess { info ->
                                if (info.isUpdateAvailable) {
                                    updateDialogInfo = info
                                } else {
                                    showUpToDateDialog = true
                                }
                            }.onFailure {
                                showUpToDateDialog = true
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isCheckingUpdate) "Checking for Updates..." else "Check for Updates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Get app updates via Appteka Store (appteka.store)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open Appteka",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            SectionHeader("Legal & Open Source", icon = Icons.Default.Gavel)

            // Terms of Use
            Card(
                onClick = { showTermsDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "Terms of Use",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Privacy Policy
            Card(
                onClick = { showPrivacyDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PrivacyTip, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Licenses
            Card(
                onClick = { showLicensesDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Code, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "Open Source Licenses",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "© 2026 FileManager Pro. Distributed via Appteka.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }

    // Terms of Use Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms of Use") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("1. Acceptance of Terms", fontWeight = FontWeight.Bold)
                    Text("By using FileManager Pro, you agree to these Terms of Use. If you do not agree, please uninstall the application.")

                    Text("2. Local File Management", fontWeight = FontWeight.Bold)
                    Text("FileManager Pro provides tools for local file management, archive extraction, media playback, and device analysis. You are solely responsible for the files you modify, rename, or delete.")

                    Text("3. App Updates", fontWeight = FontWeight.Bold)
                    Text("Application updates, patches, and version releases are officially distributed through Appteka Store (https://appteka.store). Always download official releases to ensure authenticity and safety.")

                    Text("4. Limitation of Liability", fontWeight = FontWeight.Bold)
                    Text("The software is provided 'as-is' without warranty of any kind. The developers are not liable for accidental data loss caused by user operations.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("1. 100% On-Device Privacy", fontWeight = FontWeight.Bold)
                    Text("FileManager Pro is designed with privacy first. All file indexing, thumbnail generation, vault encryption, and storage analysis take place strictly on your device.")

                    Text("2. No Remote Data Collection", fontWeight = FontWeight.Bold)
                    Text("We do NOT collect, transmit, track, or sell your files, passwords, search queries, or personal information. No external analytics or tracking SDKs are included.")

                    Text("3. Safe Folder & Vault Security", fontWeight = FontWeight.Bold)
                    Text("Files added to the Safe Folder are encrypted locally using AES-256-GCM. Passwords and PINs are stored as SHA-256 cryptographic hashes.")

                    Text("4. Updates via Appteka", fontWeight = FontWeight.Bold)
                    Text("Official app packages and updates are made available at https://appteka.store.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Open Source Licenses Dialog
    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text("Open Source Licenses") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Jetpack Compose & AndroidX (Apache 2.0)")
                    Text("Kotlin Coroutines (Apache 2.0)")
                    Text("Media3 ExoPlayer (Apache 2.0)")
                    Text("Coil Image Loader (Apache 2.0)")
                    Text("OkHttp (Apache 2.0)")
                    Text("FFmpeg Kit (GPL 3.0)")
                    Text("PDFBox Android (Apache 2.0)")
                    Text("Shizuku API (Apache 2.0)")
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Update Available Dialog
    updateDialogInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { updateDialogInfo = null },
            title = { Text("Update Available! (v${info.latestVersion})") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("A new version of FileManager Pro is available.", fontWeight = FontWeight.Bold)
                    Text("Release Notes:\n${info.releaseNotes}")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                    updateDialogInfo = null
                }) {
                    Text("Update Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { updateDialogInfo = null }) {
                    Text("Later")
                }
            }
        )
    }

    // Up to Date Dialog
    if (showUpToDateDialog) {
        AlertDialog(
            onDismissRequest = { showUpToDateDialog = false },
            title = { Text("You're Up to Date!") },
            text = { Text("You are running the latest version of FileManager Pro (v1.0.0). No updates are currently available.") },
            confirmButton = {
                TextButton(onClick = { showUpToDateDialog = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUpToDateDialog = false
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://appteka.store")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }) {
                    Text("Visit Appteka")
                }
            }
        )
    }
}
