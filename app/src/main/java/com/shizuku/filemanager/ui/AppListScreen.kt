package com.shizuku.filemanager.ui

import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.shizuku.filemanager.sys.AppInfo
import com.shizuku.filemanager.sys.AppManager
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    onBack: () -> Unit,
    onAppDetails: (AppInfo) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showSystemApps by remember { mutableStateOf(false) }
    var selectedApps by remember { mutableStateOf(setOf<AppInfo>()) }
    var overflowOpen by rememberSaveable { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 5 }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    suspend fun loadApps() {
        apps = AppManager.getInstalledApps(context)
        isLoading = false
        isRefreshing = false
        selectedApps = emptySet()
    }

    LaunchedEffect(Unit) {
        loadApps()
    }

    val filteredApps = remember(apps, searchQuery, showSystemApps) {
        apps.filter { app ->
            (app.isSystemApp == showSystemApps) &&
            (app.label.contains(searchQuery, ignoreCase = true) || app.packageName.contains(searchQuery, ignoreCase = true))
        }
    }

    ScreenScaffold(
        floatingActionButton = {
            androidx.compose.animation.AnimatedVisibility(
                visible = showScrollToTop,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut()
            ) {
                SmallFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top")
                }
            }
        },
        topBar = {
            ModernTopBar(
                title = if (selectedApps.isNotEmpty()) "${selectedApps.size} selected" else "Installed Apps",
                navigationIcon = {
                    if (selectedApps.isNotEmpty()) {
                        IconButton(onClick = { selectedApps = emptySet() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear selection")
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (selectedApps.isNotEmpty()) {
                        IconButton(onClick = {
                            scope.launch {
                                val dest = File(context.cacheDir, "extracted_apks")
                                var count = 0
                                selectedApps.forEach { app ->
                                    AppManager.extractApk(context, app, dest).onSuccess { count++ }
                                }
                                selectedApps = emptySet()
                            }
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Extract APKs")
                        }
                    }
                    
                    Box {
                        IconButton(onClick = { overflowOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = overflowOpen,
                            onDismissRequest = { overflowOpen = false },
                            properties = PopupProperties(focusable = true, clippingEnabled = false)
                        ) {
                            val allSelected = selectedApps.size == filteredApps.size && filteredApps.isNotEmpty()
                            DropdownMenuItem(
                                text = { Text(if (allSelected) "Unselect All" else "Select All") },
                                onClick = { 
                                    overflowOpen = false
                                    selectedApps = if (allSelected) emptySet() else filteredApps.toSet()
                                },
                                leadingIcon = { Icon(if (allSelected) Icons.Filled.ClearAll else Icons.Filled.SelectAll, null) }
                            )
                            if (selectedApps.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Clear Selection") },
                                    onClick = { 
                                        overflowOpen = false
                                        selectedApps = emptySet()
                                    },
                                    leadingIcon = { Icon(Icons.Filled.Clear, null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Refresh") },
                                onClick = { 
                                    overflowOpen = false
                                    isRefreshing = true
                                    scope.launch { loadApps() }
                                },
                                leadingIcon = { Icon(Icons.Filled.Refresh, null) }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) Text("Search apps...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !showSystemApps,
                    onClick = { showSystemApps = false },
                    label = { Text("User Apps") }
                )
                FilterChip(
                    selected = showSystemApps,
                    onClick = { showSystemApps = true },
                    label = { Text("System Apps") }
                )
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    scope.launch { loadApps() }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (filteredApps.isEmpty()) {
                    AppListEmptyState(searchQuery)
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredApps) { app ->
                            AppRow(
                                app = app,
                                isSelected = selectedApps.contains(app),
                                isSelectionMode = selectedApps.isNotEmpty(),
                                onToggleSelection = {
                                    selectedApps = if (selectedApps.contains(app)) {
                                        selectedApps - app
                                    } else {
                                        selectedApps + app
                                    }
                                },
                                onClick = { onAppDetails(app) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppListEmptyState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (query.isEmpty()) "No apps found" else "No results for \"$query\"",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Try adjusting your filters or search query",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    app: AppInfo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onClick: () -> Unit
) {
    ModernListItem(
        title = app.label,
        subtitle = "${app.category} · ${app.packageName}",
        onClick = { if (isSelectionMode) onToggleSelection() else onClick() },
        modifier = Modifier
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelection() else onClick() },
                onLongClick = onToggleSelection
            )
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent),
        leadingIcon = {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                AsyncAppIcon(app.packageName, Modifier.padding(8.dp))
            }
        },
        trailingContent = {
            if (isSelected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else if (app.isSystemApp) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        "System",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    )
}

@Composable
fun AsyncAppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var icon by remember(packageName) { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(packageName) {
        icon = AppManager.getAppIcon(context, packageName)
    }

    val iconBitmap = remember(icon) {
        icon?.let {
            try {
                it.toBitmap().asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxSize(0.8f)
            )
        }
    }
}
