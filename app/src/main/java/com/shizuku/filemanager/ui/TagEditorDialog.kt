package com.shizuku.filemanager.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shizuku.filemanager.db.FileTag
import com.shizuku.filemanager.db.TagDatabase
import kotlinx.coroutines.launch

@Composable
fun TagEditorDialog(
    path: String,
    database: TagDatabase,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val dao = database.tagDao()
    var currentTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var newTagText by remember { mutableStateOf("") }

    LaunchedEffect(path) {
        val entry = dao.getTags(path)
        currentTags = entry?.tags?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Labels") },
        text = {
            Column {
                Text("Labels for: ${path.substringAfterLast("/")}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                
                if (currentTags.isEmpty()) {
                    Text("No labels added.", color = MaterialTheme.colorScheme.outline)
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentTags.forEach { tag ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(tag, style = MaterialTheme.typography.labelMedium)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Close,
                                        null,
                                        modifier = Modifier.size(14.dp).clickable {
                                            val updated = currentTags.toMutableList().apply { remove(tag) }
                                            scope.launch {
                                                dao.saveTags(FileTag(path, updated.joinToString(",")))
                                                currentTags = updated
                                            }
                                        },
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = newTagText,
                    onValueChange = { newTagText = it },
                    label = { Text("New label") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            if (newTagText.isNotBlank()) {
                                val updated = currentTags.toMutableList().apply { add(newTagText.trim()) }
                                scope.launch {
                                    dao.saveTags(FileTag(path, updated.joinToString(",")))
                                    currentTags = updated
                                    newTagText = ""
                                }
                            }
                        }) {
                            Icon(Icons.Default.Add, null)
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
