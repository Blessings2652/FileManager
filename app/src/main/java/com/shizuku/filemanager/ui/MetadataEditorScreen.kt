package com.shizuku.filemanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shizuku.filemanager.sys.FileMetadata
import com.shizuku.filemanager.sys.MetadataManager
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataEditorScreen(
    filePath: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var metadata by remember { mutableStateOf<FileMetadata?>(null) }
    var editableTags by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(filePath) {
        metadata = MetadataManager.getMetadata(File(filePath))
        editableTags = metadata?.tags ?: emptyMap()
        isLoading = false
    }

    ScreenScaffold(
        topBar = {
            ModernTopBar(
                title = "Metadata Editor",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (metadata?.isEditable == true) {
                        IconButton(
                            onClick = {
                                isSaving = true
                                scope.launch {
                                    MetadataManager.updateMetadata(filePath, editableTags)
                                    isSaving = false
                                    onBack()
                                }
                            },
                            enabled = !isSaving
                        ) {
                            if (isSaving) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SectionCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                metadata?.fileName ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                metadata?.filePath ?: "", 
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Text(
                        "Properties",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                item {
                    SectionCard {
                        Column {
                            editableTags.toList().forEachIndexed { index, (tag, value) ->
                                if (metadata?.isEditable == true) {
                                    OutlinedTextField(
                                        value = value,
                                        onValueChange = { newValue ->
                                            editableTags = editableTags.toMutableMap().apply { put(tag, newValue) }
                                        },
                                        label = { Text(tag) },
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                } else {
                                    ListItem(
                                        headlineContent = { Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) },
                                        supportingContent = { Text(value, style = MaterialTheme.typography.bodyLarge) },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }
                                if (index < editableTags.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
