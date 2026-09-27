package com.shizuku.filemanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shizuku.filemanager.sys.IntegrityManager
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileIntegrityScreen(
    filePath: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    
    var md5 by remember { mutableStateOf("Calculating...") }
    var sha1 by remember { mutableStateOf("Calculating...") }
    var sha256 by remember { mutableStateOf("Calculating...") }
    
    var inputHash by remember { mutableStateOf("") }
    var verificationResult by remember { mutableStateOf<Boolean?>(null) }

    val file = remember { File(filePath) }

    LaunchedEffect(filePath) {
        launch { md5 = IntegrityManager.calculateHash(file, "MD5") }
        launch { sha1 = IntegrityManager.calculateHash(file, "SHA-1") }
        launch { sha256 = IntegrityManager.calculateHash(file, "SHA-256") }
    }

    ScreenScaffold(
        topBar = {
            ModernTopBar(
                title = "File Integrity",
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "File Information",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            
            SectionCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(file.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        file.absolutePath, 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            
            Text(
                "Checksums",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            SectionCard {
                Column {
                    HashItem("MD5", md5) { scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("MD5", md5))) } }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    HashItem("SHA-1", sha1) { scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("SHA-1", sha1))) } }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    HashItem("SHA-256", sha256) { scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("SHA-256", sha256))) } }
                }
            }

            Spacer(Modifier.height(32.dp))
            
            Text(
                "Verification",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            SectionCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = inputHash,
                        onValueChange = { inputHash = it; verificationResult = null },
                        label = { Text("Enter expected hash") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(16.dp))
                    
                    GradientButton(
                        text = "Verify Checksum",
                        onClick = {
                            verificationResult = inputHash.equals(md5, true) || 
                                                 inputHash.equals(sha1, true) || 
                                                 inputHash.equals(sha256, true)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = inputHash.isNotBlank()
                    )

                    verificationResult?.let { result ->
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            color = if (result) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (result) Icons.Default.Verified else Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = if (result) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    if (result) "Verified Match!" else "Hash Mismatch!",
                                    color = if (result) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HashItem(label: String, value: String, onCopy: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) },
        supportingContent = { 
            Text(
                value, 
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            ) 
        },
        trailingContent = {
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(20.dp))
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
