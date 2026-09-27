package com.shizuku.filemanager.permissions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AdvancedPermissionsDialog(
    filePath: String,
    fileName: String,
    isDirectory: Boolean,
    manager: PermissionsManager,
    onDismiss: () -> Unit,
    onApplied: () -> Unit
) {
    var permissions by remember { mutableStateOf<FilePermissions?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var applying by remember { mutableStateOf(false) }
    var recursive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(filePath) {
        loading = true
        error = null
        permissions = manager.readPermissions(filePath)
        if (permissions == null) error = "Couldn't read permissions for this file"
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Advanced Permissions") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = fileName,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Modify R/W/X and ownership",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                when {
                    loading -> Box(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    error != null -> Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error
                    )

                    permissions != null -> {
                        val current = permissions!!

                        PermissionClassRow(
                            label = "Owner",
                            triple = current.owner,
                            onChange = { permissions = current.copy(owner = it) }
                        )
                        PermissionClassRow(
                            label = "Group",
                            triple = current.group,
                            onChange = { permissions = current.copy(group = it) }
                        )
                        PermissionClassRow(
                            label = "Other",
                            triple = current.other,
                            onChange = { permissions = current.copy(other = it) }
                        )

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Octal: ${current.octalString}",
                            style = MaterialTheme.typography.labelLarge
                        )

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(16.dp))

                        Text("Ownership", fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = current.ownerUser,
                            onValueChange = { permissions = current.copy(ownerUser = it) },
                            label = { Text("User") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = current.ownerGroup,
                            onValueChange = { permissions = current.copy(ownerGroup = it) },
                            label = { Text("Group") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isDirectory) {
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = recursive, onCheckedChange = { recursive = it }, modifier = Modifier.scale(0.8f))
                                Text("Apply recursively to contents")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = permissions != null && !applying,
                onClick = {
                    val toApply = permissions ?: return@TextButton
                    applying = true
                    error = null
                    scope.launch {
                        val result = manager.applyPermissions(filePath, toApply, recursive)
                        applying = false
                        when (result) {
                            is PermissionResult.Success -> onApplied()
                            is PermissionResult.Failure -> error = result.message
                        }
                    }
                }
            ) {
                if (applying) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Apply")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PermissionClassRow(
    label: String,
    triple: PermissionTriple,
    onChange: (PermissionTriple) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(64.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        PermissionCheckbox("R", triple.read) { onChange(triple.copy(read = it)) }
        PermissionCheckbox("W", triple.write) { onChange(triple.copy(write = it)) }
        PermissionCheckbox("X", triple.execute) { onChange(triple.copy(execute = it)) }
    }
}

@Composable
private fun PermissionCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.scale(0.8f))
        Text(label, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.width(8.dp))
    }
}
