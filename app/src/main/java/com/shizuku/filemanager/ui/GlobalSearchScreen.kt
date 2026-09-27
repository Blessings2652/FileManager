package com.shizuku.filemanager.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.shizuku.filemanager.fs.FileEntry
import com.shizuku.filemanager.fs.GlobalSearch
import com.shizuku.filemanager.fs.engine.FileEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Recursive search starting from [startPath], streamed in as GlobalSearch
 * walks the tree. Distinct from the in-toolbar filename filter already in
 * FileBrowserScreen, which only looks at the current folder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    engine: FileEngine,
    startPath: String,
    onBack: () -> Unit,
    onOpenResult: (FileEntry, parentPath: String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var matchContent by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<GlobalSearch.Match>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun runSearch() {
        searchJob?.cancel()
        results = emptyList()
        if (query.isBlank()) return
        searching = true
        searchJob = GlobalSearch.search(engine, startPath, GlobalSearch.Options(query = query, matchContent = matchContent))
            .onEach { results = results + it }
            .launchIn(scope)
        scope.launch {
            searchJob?.join()
            searching = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search in $startPath") },
                navigationIcon = { IconButton(onClick = { searchJob?.cancel(); onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; runSearch() },
                    label = { Text("Search name" + if (matchContent) " or content" else "") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { if (searching) CircularProgressIndicator(modifier = Modifier.size(20.dp)) }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = matchContent, onCheckedChange = { matchContent = it; runSearch() }, modifier = Modifier.scale(0.8f))
                    Text("Also search inside text files")
                }
            }
            HorizontalDivider()
            LazyColumn(Modifier.fillMaxSize()) {
                items(results, key = { it.entry.path }) { match ->
                    ListItem(
                        headlineContent = { Text(match.entry.name) },
                        supportingContent = {
                            Text(
                                match.entry.path + if (match.matchedInContent) "  ·  matched in content" else "",
                                maxLines = 1
                            )
                        },
                        leadingContent = {
                            Icon(if (match.entry.isDirectory) Icons.Filled.Folder else Icons.Filled.Description, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth().clickable { onOpenResult(match.entry, match.parentPath) }
                    )
                    HorizontalDivider()
                }
            }
            if (!searching && query.isNotBlank() && results.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No matches yet")
                }
            }
        }
    }
}
