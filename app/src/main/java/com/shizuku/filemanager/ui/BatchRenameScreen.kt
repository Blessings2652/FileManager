package com.shizuku.filemanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.shizuku.filemanager.fs.FileEntry
import com.shizuku.filemanager.fs.engine.FileEngine
import com.shizuku.filemanager.sys.BatchRenameEngine
import kotlinx.coroutines.launch

/**
 * Batch rename for a multi-select. Live preview updates as the user
 * edits the pattern; [onConfirm] receives only the entries whose name
 * actually changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchRenameScreen(
    entries: List<FileEntry>,
    engine: FileEngine,
    onBack: () -> Unit,
    onConfirm: suspend (List<BatchRenameEnginePlan>) -> Unit,
) {
    var options by remember { mutableStateOf(BatchRenameEngine.Options()) }
    val plans = remember(entries, options) { BatchRenameEngine.preview(entries, options) }
    val error = remember(plans) { BatchRenameEngine.validate(plans) }
    val scope = rememberCoroutineScope()
    var applying by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Batch rename (${entries.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.padding(16.dp)) {
                    if (error != null) {
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(
                        onClick = {
                            applying = true
                            scope.launch {
                                onConfirm(plans.filter { it.changed })
                                applying = false
                            }
                        },
                        enabled = error == null && !applying && plans.any { it.changed },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (applying) "Renaming…" else "Apply to ${plans.count { it.changed }} file(s)")
                    }
                }
            }
        }
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = options.findText,
                    onValueChange = { options = options.copy(findText = it) },
                    label = { Text("Find") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = options.replaceText,
                    onValueChange = { options = options.copy(replaceText = it) },
                    label = { Text("Replace with") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = options.useRegex, onCheckedChange = { options = options.copy(useRegex = it) }, modifier = Modifier.scale(0.8f))
                    Text("Use regex")
                }
                OutlinedTextField(
                    value = options.prefix,
                    onValueChange = { options = options.copy(prefix = it) },
                    label = { Text("Add prefix") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = options.suffix,
                    onValueChange = { options = options.copy(suffix = it) },
                    label = { Text("Add suffix") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = options.numbering, onCheckedChange = { options = options.copy(numbering = it) }, modifier = Modifier.scale(0.8f))
                    Text("Append sequence number")
                }
                if (options.numbering) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = options.numberStart.toString(),
                            onValueChange = { options = options.copy(numberStart = it.toIntOrNull() ?: options.numberStart) },
                            label = { Text("Start at") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = options.numberPadding.toString(),
                            onValueChange = { options = options.copy(numberPadding = it.toIntOrNull() ?: options.numberPadding) },
                            label = { Text("Digits") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text("Case", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BatchRenameEngine.CaseMode.entries.forEach { mode ->
                        FilterChip(
                            selected = options.caseMode == mode,
                            onClick = { options = options.copy(caseMode = mode) },
                            label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
            Column(Modifier.weight(1f)) {
                Text("Preview", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(16.dp))
                LazyColumn(Modifier.fillMaxSize()) {
                    items(plans, key = { it.original.path }) { plan ->
                        ListItem(
                            headlineContent = { Text(plan.newName, maxLines = 1) },
                            supportingContent = { Text(plan.original.name, maxLines = 1, style = MaterialTheme.typography.bodySmall) },
                        )
                    }
                }
            }
        }
    }
}

private typealias BatchRenameEnginePlan = BatchRenameEngine.Plan
