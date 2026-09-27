package com.shizuku.filemanager.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shizuku.filemanager.sys.EventLogger
import com.shizuku.filemanager.sys.FileEvent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(onBack: () -> Unit) {
    var allEvents by remember { mutableStateOf<List<FileEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showClearDialog by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val locale = LocalLocale.current.platformLocale

    LaunchedEffect(Unit) {
        allEvents = EventLogger.getEvents(context)
        isLoading = false
    }

    val filteredEvents = remember(allEvents, filterType, searchQuery) {
        allEvents.filter { 
            (filterType == "ALL" || it.type == filterType) &&
            (searchQuery.isEmpty() || it.fileName.contains(searchQuery, ignoreCase = true) || it.path.contains(searchQuery, ignoreCase = true))
        }
    }

    val groupedEvents = remember(filteredEvents, locale) {
        filteredEvents.groupBy { event ->
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000
            val diff = now - event.timestamp
            when {
                diff < dayMs && SimpleDateFormat("d", Locale.US).format(Date(now)) == SimpleDateFormat("d", Locale.US).format(Date(event.timestamp)) -> "Today"
                diff < 2 * dayMs -> "Yesterday"
                else -> SimpleDateFormat("MMMM dd, yyyy", locale).format(Date(event.timestamp))
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Timeline") },
            text = { Text("Are you sure you want to clear the entire file event history?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        EventLogger.clearEvents(context)
                        allEvents = emptyList()
                        showClearDialog = false
                    }
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ScreenScaffold(
        topBar = {
            if (isSearching) {
                val onActiveChange: (Boolean) -> Unit = { active -> if (!active) isSearching = false }
                val colors1 = SearchBarDefaults.colors()
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = { isSearching = false },
                            expanded = false,
                            onExpandedChange = onActiveChange,
                            enabled = true,
                            placeholder = { Text("Search timeline...") },
                            leadingIcon = {
                                        IconButton(onClick = { isSearching = false; searchQuery = "" }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                        }
                                    },
                            trailingIcon = null,
                            colors = colors1.inputFieldColors,
                            interactionSource = null,
                        )
                    },
                    expanded = false,
                    onExpandedChange = onActiveChange,
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp),
                    shape = SearchBarDefaults.inputFieldShape,
                    colors = colors1,
                    tonalElevation = SearchBarDefaults.TonalElevation,
                    shadowElevation = SearchBarDefaults.ShadowElevation,
                    windowInsets = SearchBarDefaults.windowInsets,
                    content = {},
                )
            } else {
                ModernTopBar(
                    title = "File Timeline",
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        if (allEvents.isNotEmpty()) {
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear All")
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "NEW", "MODIFIED", "DELETED").forEach { type ->
                    FilterChip(
                        selected = filterType == type,
                        onClick = { filterType = type },
                        label = { Text(type.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (allEvents.isEmpty()) {
                EmptyState(Icons.Default.History, "No file events recorded yet.")
            } else if (filteredEvents.isEmpty()) {
                EmptyState(Icons.Default.Search, "No matching events found.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    groupedEvents.forEach { (date, eventsInDate) ->
                        stickyHeader {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            ) {
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                        items(eventsInDate) { event ->
                            TimelineItem(event)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon, 
            null, 
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TimelineItem(event: FileEvent) {
    val locale = LocalLocale.current.platformLocale
    val icon = when (event.type) {
        "NEW" -> Icons.Default.Add
        "MODIFIED" -> Icons.Default.Edit
        "DELETED" -> Icons.Default.Delete
        else -> Icons.Default.History
    }
    
    val color = when (event.type) {
        "NEW" -> Color(0xFF4CAF50)
        "MODIFIED" -> Color(0xFF2196F3)
        "DELETED" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.primary
    }

    ModernListItem(
        title = event.fileName,
        subtitle = "${event.type} • ${SimpleDateFormat("HH:mm:ss", locale).format(Date(event.timestamp))}\n${event.path}",
        onClick = {},
        leadingIcon = {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
        }
    )
}
