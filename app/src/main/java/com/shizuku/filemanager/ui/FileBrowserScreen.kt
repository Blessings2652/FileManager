package com.shizuku.filemanager.ui

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.EditAttributes
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shizuku.filemanager.R
import com.shizuku.filemanager.fs.BookmarksManager
import com.shizuku.filemanager.fs.FileEntry
import com.shizuku.filemanager.fs.engine.EnginePrefs
import com.shizuku.filemanager.fs.engine.EngineType
import com.shizuku.filemanager.fs.engine.FileEngine
import com.shizuku.filemanager.permissions.AdvancedPermissionsDialog
import com.shizuku.filemanager.permissions.PermissionsManager
import com.shizuku.filemanager.shizuku.ShizukuManager
import com.shizuku.filemanager.sys.AppManager
import com.shizuku.filemanager.sys.FolderCategoryDetector
import com.shizuku.filemanager.sys.ThumbnailManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    engine: FileEngine,
    onOpenDrawer: () -> Unit,
    initialPath: String? = null,
    onOpenFile: (FileEntry, FileEngine) -> Unit = { _, _ -> },
    lockedRoot: String? = null,
    isDocumentMode: Boolean = false,
    database: com.shizuku.filemanager.db.TagDatabase? = null,
    onSwitchEngine: (EngineType) -> Unit = {},
    vm: FileBrowserViewModel = viewModel(
        key = engine.type.name + (initialPath ?: "") + (lockedRoot ?: "") + isDocumentMode,
        factory = FileBrowserViewModel.Factory(
            LocalContext.current.applicationContext as android.app.Application,
            engine,
            initialPath,
            lockedRoot = lockedRoot,
            isDocumentMode = isDocumentMode
        )
    )
) {
    val screenContext = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by vm.state.collectAsState()

    var tagMap by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    LaunchedEffect(state.entries, database) {
        database?.tagDao()?.getAll()?.let { tags ->
            tagMap = tags.associate { it.path to it.tags.split(",").filter { t -> t.isNotBlank() } }
        }
    }
    var menuTarget by remember { mutableStateOf<FileEntry?>(null) }
    var renameText by remember { mutableStateOf("") }
    var newFolderText by remember { mutableStateOf("") }
    var overflowOpen by remember { mutableStateOf(false) }
    var showViewModeDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showBulkRenameDialog by remember { mutableStateOf(false) }
    var showCreateArchiveDialog by remember { mutableStateOf(false) }
    var showArchiveOptions by remember { mutableStateOf(false) }
    var bulkRenamePattern by remember { mutableStateOf("") }
    var bulkRenameReplacement by remember { mutableStateOf("") }
    var showTagEditor by remember { mutableStateOf<String?>(null) }
    var permissionsTarget by remember { mutableStateOf<FileEntry?>(null) }
    var showCreateMenu by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    
    val showScrollToTop by remember {
        derivedStateOf {
            when (state.viewMode) {
                "Grid", "Small Icons" -> gridState.firstVisibleItemIndex > 5
                else -> listState.firstVisibleItemIndex > 5
            }
        }
    }

    val permissionsManager = remember { PermissionsManager() }
    val snackbarHostState = remember { SnackbarHostState() }

    val focusRequester = remember { FocusRequester() }

    BackHandler(enabled = vm.canNavigateUp || state.showSearch || state.isSelectionMode) {
        if (state.isSelectionMode) {
            vm.clearSelection()
        } else if (state.showSearch) {
            vm.toggleSearch()
        } else {
            vm.navigateUp()
        }
    }

    LaunchedEffect(state.showSearch) {
        if (state.showSearch) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(state.opError) {
        state.opError?.let {
            snackbarHostState.showSnackbar(it)
            vm.dismissOpError()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.onSettingsChanged()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ScreenScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AnimatedVisibility(
                visible = !state.isDocumentMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    if (state.showSearch && !state.isSelectionMode) {
                        ModernTopBar(
                            titleContent = {
                                TextField(
                                    value = state.searchQuery,
                                    onValueChange = { vm.setSearchQuery(it) },
                                    placeholder = { Text("Search files...") },
                                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                    )
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { vm.toggleSearch() }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close search")
                                }
                            },
                            actions = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { vm.setSearchQuery("") }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                    }
                                }
                            }
                        )
                    } else {
                        ModernTopBar(
                            title = if (state.isSelectionMode) "${state.selectedItems.size} selected" else stringResource(R.string.app_name),
                            navigationIcon = {
                                if (state.isSelectionMode) {
                                    IconButton(onClick = { vm.clearSelection() }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                                    }
                                } else {
                                    IconButton(onClick = onOpenDrawer) {
                                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                    }
                                }
                            },
                            actions = {
                                if (state.isSelectionMode) {
                                    IconButton(onClick = { vm.copyToClipboard(state.selectedItems) }) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                                    }
                                    IconButton(onClick = { vm.cutToClipboard(state.selectedItems) }) {
                                        Icon(Icons.Filled.ContentCut, contentDescription = "Cut")
                                    }
                                    IconButton(onClick = { vm.requestBulkDelete(state.selectedItems) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }

                                    Box {
                                        var selectionOverflowOpen by remember { mutableStateOf(false) }
                                        IconButton(onClick = { selectionOverflowOpen = true }) {
                                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                                        }
                                        if (selectionOverflowOpen) {
                                            ModernBottomSheet(onDismissRequest = { selectionOverflowOpen = false }) {
                                                Column(
                                                    modifier = Modifier
                                                        .verticalScroll(rememberScrollState())
                                                        .navigationBarsPadding()
                                                        .padding(bottom = 32.dp)
                                                ) {
                                                    SectionHeader(title = "Selection Options", icon = Icons.Default.SelectAll)
                                                    val allSelected = state.selectedItems.size == state.entries.size && state.entries.isNotEmpty()
                                                    ModernListItem(
                                                        title = if (allSelected) "Unselect All" else "Select All",
                                                        leadingIcon = { Icon(if (allSelected) Icons.Filled.ClearAll else Icons.Filled.SelectAll, null) },
                                                        onClick = { 
                                                            selectionOverflowOpen = false
                                                            if (allSelected) vm.clearSelection() else vm.selectAll()
                                                        }
                                                    )
                                                    if (state.selectedItems.isNotEmpty()) {
                                                        ModernListItem(
                                                            title = "Clear Selection",
                                                            leadingIcon = { Icon(Icons.Filled.Clear, null) },
                                                            onClick = { selectionOverflowOpen = false; vm.clearSelection() }
                                                        )
                                                    }
                                                    ModernListItem(
                                                        title = "Bulk Rename",
                                                        leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, null) },
                                                        onClick = { 
                                                            selectionOverflowOpen = false
                                                            showBulkRenameDialog = true 
                                                        }
                                                    )
                                                    ModernListItem(
                                                        title = "Create Archive",
                                                        leadingIcon = { Icon(Icons.Filled.Archive, null) },
                                                        onClick = { 
                                                            selectionOverflowOpen = false
                                                            showCreateArchiveDialog = true 
                                                        }
                                                    )
                                                    ModernListItem(
                                                        title = "Share",
                                                        leadingIcon = { Icon(Icons.Filled.Share, null) },
                                                        onClick = {
                                                            selectionOverflowOpen = false
                                                            scope.launch {
                                                                com.shizuku.filemanager.sys.FileIntentUtils.shareFiles(
                                                                    screenContext,
                                                                    state.selectedItems.toList(),
                                                                    engine
                                                                )
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    IconButton(onClick = { vm.toggleSearch() }) {
                                        Icon(Icons.Filled.Search, contentDescription = "Search")
                                    }
                                    if (state.clipboard != null) {
                                        IconButton(onClick = { vm.pasteHere() }) {
                                            Icon(Icons.Filled.ContentPaste, contentDescription = "Paste")
                                        }
                                    }
                                    Box {
                                        IconButton(onClick = { overflowOpen = true }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                                        }
                                        if (overflowOpen) {
                                            ModernBottomSheet(onDismissRequest = { overflowOpen = false }) {
                                                Column(
                                                    modifier = Modifier
                                                        .verticalScroll(rememberScrollState())
                                                        .navigationBarsPadding()
                                                        .padding(bottom = 32.dp)
                                                ) {
                                                    SectionHeader(title = "Options", icon = Icons.Default.MoreVert)
                                                    ModernListItem(
                                                        title = "Select All",
                                                        leadingIcon = { Icon(Icons.Filled.SelectAll, null) },
                                                        onClick = { overflowOpen = false; vm.selectAll() }
                                                    )
                                                    ModernListItem(
                                                        title = "View Mode",
                                                        leadingIcon = { Icon(Icons.Filled.GridView, null) },
                                                        onClick = { overflowOpen = false; showViewModeDialog = true }
                                                    )
                                                    ModernListItem(
                                                        title = "Sort",
                                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null) },
                                                        onClick = { overflowOpen = false; showSortDialog = true }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                    PathBar(
                        engine = engine,
                        currentPath = state.currentPath,
                        onNavigate = { vm.navigateTo(it) },
                        lockedRoot = lockedRoot,
                        fontSizeMultiplier = state.fontSizeMultiplier
                    )
                    
                    // Storage Info Bar - Modernized
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Label, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(12.dp), 
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                val summaryText = buildString {
                                    if (state.folderCount > 0) {
                                        append("${state.folderCount} folder${if (state.folderCount > 1) "s" else ""}")
                                    }
                                    if (state.fileCount > 0) {
                                        if (isNotEmpty()) append(", ")
                                        append("${state.fileCount} file${if (state.fileCount > 1) "s" else ""}")
                                    }
                                    if (isEmpty()) append("Empty folder")
                                }
                                Text(
                                    text = summaryText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize * state.fontSizeMultiplier,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (state.fileCount > 0) {
                                Text(
                                    text = formatSize(state.totalFilesSize),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize * state.fontSizeMultiplier,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    val isShizukuInstalled = remember { ShizukuManager.isAppInstalled(screenContext) }
                    var dismissShizukuBanner by remember { mutableStateOf(false) }

                    if (isShizukuInstalled && !ShizukuManager.isAvailable.value && !dismissShizukuBanner && !state.isSelectionMode) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Shizuku Installed",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Start Shizuku to unlock full access to system storage.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Button(
                                    onClick = { ShizukuManager.launchShizukuApp(screenContext) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Start", style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(
                                    onClick = { dismissShizukuBanner = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(
                    visible = showScrollToTop,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                when (state.viewMode) {
                                    "Grid", "Small Icons" -> gridState.animateScrollToItem(0)
                                    else -> listState.animateScrollToItem(0)
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Scroll to top")
                    }
                }
                if (showScrollToTop) Spacer(Modifier.height(16.dp))


                
                Box {
                    FloatingActionButton(onClick = { showCreateMenu = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Create new")
                    }
                    if (showCreateMenu) {
                        ModernBottomSheet(onDismissRequest = { showCreateMenu = false }) {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .navigationBarsPadding()
                                    .padding(bottom = 32.dp)
                            ) {
                                SectionHeader(title = "Create New", icon = Icons.Filled.Add)
                                ModernListItem(
                                    title = "New File",
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.NoteAdd, null) },
                                    onClick = { 
                                        showCreateMenu = false
                                        vm.showNewFileDialog() 
                                    }
                                )
                                ModernListItem(
                                    title = "New Folder",
                                    leadingIcon = { Icon(Icons.Filled.CreateNewFolder, null) },
                                    onClick = { 
                                        showCreateMenu = false
                                        vm.showNewFolderDialog() 
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { vm.refresh() },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            state.extractionProgress?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                )
            }
            
            AnimatedContent(
                targetState = Triple(state.isLoading && state.entries.isEmpty(), state.error, state.entries.isEmpty() && state.restrictedPath == null),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ContentState"
            ) { (loading, error, empty) ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        state.restrictedPath != null -> RestrictedView(
                            path = state.restrictedPath!!,
                            onSwitchToShizuku = { onSwitchEngine(EngineType.SHIZUKU) },
                            onStartShizukuApp = {
                                if (!ShizukuManager.launchShizukuApp(screenContext)) {
                                    Toast.makeText(screenContext, "Shizuku app not found", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.align(Alignment.Center),
                            fontSizeMultiplier = state.fontSizeMultiplier,
                            itemSizeMultiplier = state.itemSizeMultiplier
                        )
                        error != null -> ErrorView(
                            message = error,
                            onRetry = { vm.refresh() },
                            modifier = Modifier.align(Alignment.Center),
                            fontSizeMultiplier = state.fontSizeMultiplier,
                            itemSizeMultiplier = state.itemSizeMultiplier
                        )
                        empty -> EmptyFolderView(
                            modifier = Modifier.align(Alignment.Center),
                            fontSizeMultiplier = state.fontSizeMultiplier,
                            itemSizeMultiplier = state.itemSizeMultiplier
                        )
                        else -> {
                            val viewMode = state.viewMode
                            val thumbnailsEnabled = state.thumbnailsEnabled
                            val showExtensions = state.showExtensions

                            when (viewMode) {
                                "Grid", "Small Icons" -> {
                                    LazyVerticalGrid(
                                        state = gridState,
                                        columns = GridCells.Adaptive(minSize = 100.dp * state.itemSizeMultiplier),
                                        contentPadding = PaddingValues(bottom = 80.dp, start = 8.dp, end = 8.dp, top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(
                                            items = state.entries, 
                                            key = { it.path },
                                            contentType = { if (it.isDirectory) "folder" else "file" }
                                        ) { entry ->
                                            FileGridItem(
                                                entry = entry,
                                                engineType = engine.type,
                                                thumbnailsEnabled = thumbnailsEnabled,
                                                showExtensions = showExtensions,
                                                isSelected = state.selectedItems.contains(entry),
                                                tags = tagMap[entry.path] ?: emptyList(),
                                                itemSizeMultiplier = state.itemSizeMultiplier,
                                                fontSizeMultiplier = state.fontSizeMultiplier,
                                                onClick = {
                                                    if (state.isSelectionMode) vm.toggleSelection(entry)
                                                    else if (entry.isDirectory) vm.openEntry(entry)
                                                    else menuTarget = entry
                                                },
                                                onLongClick = { 
                                                    if (!state.isSelectionMode && entry.isDirectory) {
                                                        menuTarget = entry
                                                    } else {
                                                        vm.toggleSelection(entry)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                "Columns" -> {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        FileColumnHeader()
                                        HorizontalDivider()
                                        LazyColumn(
                                            state = listState,
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(bottom = 80.dp)
                                        ) {
                                            items(
                                                items = state.entries, 
                                                key = { it.path },
                                                contentType = { if (it.isDirectory) "folder" else "file" }
                                            ) { entry ->
                                                FileColumnRow(
                                                    entry = entry,
                                                    isSelected = state.selectedItems.contains(entry),
                                                    itemSizeMultiplier = state.itemSizeMultiplier,
                                                    fontSizeMultiplier = state.fontSizeMultiplier,
                                                    onClick = {
                                                        if (state.isSelectionMode) vm.toggleSelection(entry)
                                                        else if (entry.isDirectory) vm.openEntry(entry)
                                                        else menuTarget = entry
                                                    },
                                                    onLongClick = { 
                                                        if (!state.isSelectionMode && entry.isDirectory) {
                                                            menuTarget = entry
                                                        } else {
                                                            vm.toggleSelection(entry)
                                                        }
                                                    }
                                                )
                                                HorizontalDivider()
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    LazyColumn(
                                        state = listState,
                                        contentPadding = PaddingValues(bottom = 80.dp)
                                    ) {
                                        items(
                                            items = state.entries, 
                                            key = { it.path },
                                            contentType = { if (it.isDirectory) "folder" else "file" }
                                        ) { entry ->
                                            FileRow(
                                                entry = entry,
                                                engineType = engine.type,
                                                thumbnailsEnabled = thumbnailsEnabled,
                                                showExtensions = showExtensions,
                                                isSelected = state.selectedItems.contains(entry),
                                                tags = tagMap[entry.path] ?: emptyList(),
                                                itemSizeMultiplier = state.itemSizeMultiplier,
                                                onClick = {
                                                    if (state.isSelectionMode) vm.toggleSelection(entry)
                                                    else if (entry.isDirectory) vm.openEntry(entry)
                                                    else menuTarget = entry
                                                },
                                                onLongClick = { 
                                                    if (!state.isSelectionMode && entry.isDirectory) {
                                                        menuTarget = entry
                                                    } else {
                                                        vm.toggleSelection(entry)
                                                    }
                                                }
                                            )
                                            HorizontalDivider()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Per-item action menu
    menuTarget?.let { entry ->
        ModernBottomSheet(onDismissRequest = { menuTarget = null }) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp)
            ) {
                SectionHeader(
                    title = entry.name,
                    icon = if (entry.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile
                )
                
                if (!entry.isDirectory) {
                    if (entry.isArchive) {
                        if (entry.name.lowercase().endsWith(".zip")) {
                            ModernListItem(
                                title = "View Contents",
                                leadingIcon = { Icon(Icons.Filled.Visibility, null) },
                                onClick = {
                                    onOpenFile(entry.copy(permissions = "VIEW_ZIP"), engine)
                                    menuTarget = null
                                }
                            )
                        }
                        ModernListItem(
                            title = "Extract",
                            leadingIcon = { Icon(Icons.Filled.Unarchive, null) },
                            onClick = {
                                vm.extractZip(entry)
                                menuTarget = null
                            }
                        )
                    } else {
                        ModernListItem(
                            title = "Open",
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) },
                            onClick = { onOpenFile(entry, engine); menuTarget = null }
                        )
                    }
                }
                ModernListItem(
                    title = "Details",
                    leadingIcon = { Icon(Icons.Filled.Visibility, null) },
                    onClick = {
                        vm.showDetails(entry)
                        menuTarget = null
                    }
                )
                var isBookmarked by remember(entry) { mutableStateOf(false) }
                LaunchedEffect(entry) {
                    isBookmarked = BookmarksManager.isBookmarked(screenContext, entry.path, engine.type)
                }
                ModernListItem(
                    title = if (isBookmarked) "Remove Bookmark" else "Add Bookmark",
                    leadingIcon = { Icon(Icons.Filled.Star, null, tint = if (isBookmarked) MaterialTheme.colorScheme.primary else LocalContentColor.current) },
                    onClick = {
                        scope.launch {
                            BookmarksManager.toggle(screenContext, entry.path, entry.name, engine.type)
                            Toast.makeText(screenContext, if (isBookmarked) "Bookmark removed" else "Bookmark added", Toast.LENGTH_SHORT).show()
                        }
                        menuTarget = null
                    }
                )
                ModernListItem(
                    title = "Compress",
                    leadingIcon = { Icon(Icons.Filled.Archive, null) },
                    onClick = {
                        vm.toggleSelection(entry) // Add to selection for compression
                        showArchiveOptions = true
                        menuTarget = null
                    }
                )
                ModernListItem(
                    title = "Copy",
                    leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                    onClick = { vm.copyToClipboard(setOf(entry)); menuTarget = null }
                )
                ModernListItem(
                    title = "Cut",
                    leadingIcon = { Icon(Icons.Filled.ContentCut, null) },
                    onClick = { vm.cutToClipboard(setOf(entry)); menuTarget = null }
                )
                ModernListItem(
                    title = "Rename",
                    leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, null) },
                    onClick = {
                        renameText = entry.name
                        vm.requestRename(entry)
                        menuTarget = null
                    }
                )
                ModernListItem(
                    title = "Edit Labels",
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null) },
                    onClick = {
                        showTagEditor = entry.path
                        menuTarget = null
                    }
                )
                if (entry.isText || entry.isDocument) {
                    ModernListItem(
                        title = "Edit as Text",
                        leadingIcon = { Icon(Icons.Filled.EditNote, null) },
                        onClick = {
                            onOpenFile(entry.copy(permissions = "TEXT_EDIT"), engine)
                            menuTarget = null
                        }
                    )
                }
                ModernListItem(
                    title = "Delete",
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { 
                        if (EnginePrefs.isConfirmDelete(screenContext)) {
                            vm.requestDelete(entry)
                        } else {
                            vm.directDelete(entry)
                        }
                        menuTarget = null 
                    }
                )
                if (engine.type != EngineType.SAF) {
                    ModernListItem(
                        title = "Permissions",
                        leadingIcon = { Icon(Icons.Filled.Security, null) },
                        onClick = { permissionsTarget = entry; menuTarget = null }
                    )
                }
                
                ModernListItem(
                    title = "Integrity & Hash",
                    leadingIcon = { Icon(Icons.Filled.VerifiedUser, null) },
                    onClick = {
                        onOpenFile(entry.copy(permissions = "INTEGRITY"), engine) // Hack for navigation
                        menuTarget = null
                    }
                )

                if (!entry.isDirectory && entry.name.lowercase().let { it.endsWith(".jpg") || it.endsWith(".jpeg") }) {
                    ModernListItem(
                        title = "Edit Metadata",
                        leadingIcon = { Icon(Icons.Filled.EditAttributes, null) },
                        onClick = {
                            onOpenFile(entry.copy(permissions = "METADATA"), engine) // Hack for navigation
                            menuTarget = null
                        }
                    )
                }
                
                val isSafeFolder = state.currentPath.contains(".safe")
                ModernListItem(
                    title = if (isSafeFolder) "Move to Storage" else "Move to Safe Folder",
                    leadingIcon = { Icon(if (isSafeFolder) Icons.AutoMirrored.Filled.ExitToApp else Icons.Filled.VpnKey, null) },
                    onClick = {
                        if (isSafeFolder) vm.moveFromSafe(entry) else vm.moveToSafe(entry)
                        menuTarget = null
                    }
                )

                if (!entry.isDirectory) {
                    ModernListItem(
                        title = "Share",
                        leadingIcon = { Icon(Icons.Filled.Share, null) },
                        onClick = {
                            scope.launch {
                                com.shizuku.filemanager.sys.FileIntentUtils.shareFile(screenContext, entry, engine)
                            }
                            menuTarget = null
                        }
                    )
                    ModernListItem(
                        title = "Open with...",
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) },
                        onClick = {
                            scope.launch {
                                com.shizuku.filemanager.sys.FileIntentUtils.openWith(screenContext, entry, engine)
                            }
                            menuTarget = null
                        }
                    )
                }
                if (entry.name.lowercase().endsWith(".apk")) {
                    ModernListItem(
                        title = "Install APK",
                        leadingIcon = { Icon(Icons.Filled.SystemUpdate, null) },
                        onClick = {
                            onOpenFile(entry.copy(permissions = "INSTALL"), engine) // Special flag
                            menuTarget = null
                        }
                    )
                }

            }
        }
    }

    // Permissions Dialog
    permissionsTarget?.let { entry ->
        AdvancedPermissionsDialog(
            filePath = entry.path,
            fileName = entry.name,
            isDirectory = entry.isDirectory,
            manager = permissionsManager,
            onDismiss = { permissionsTarget = null },
            onApplied = {
                permissionsTarget = null
                vm.refresh()
            }
        )
    }

    // New File dialog
    if (state.showNewFileDialog) {
        var fileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { vm.dismissNewFileDialog() },
            title = { Text("New File") },
            text = {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File name (e.g. note.txt)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (fileName.isNotBlank()) {
                            vm.createFile(fileName)
                        }
                    },
                    enabled = fileName.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissNewFileDialog() }) { Text("Cancel") }
            }
        )
    }

    // New folder dialog
    if (state.showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { vm.dismissNewFolderDialog() },
            title = { Text("New folder") },
            text = {
                OutlinedTextField(
                    value = newFolderText,
                    onValueChange = { newFolderText = it },
                    singleLine = true,
                    label = { Text("Folder name") }
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.createFolder(newFolderText); newFolderText = "" }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissNewFolderDialog() }) { Text("Cancel") }
            }
        )
    }

    // Rename dialog
    state.pendingRename?.let {
        AlertDialog(
            onDismissRequest = { vm.cancelRename() },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.confirmRename(renameText) }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { vm.cancelRename() }) { Text("Cancel") }
            }
        )
    }

    // Delete confirmation
    state.pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { vm.cancelDelete() },
            title = { Text("Move to Trash?") },
            text = {
                val detail = if (entry.isDirectory) " This will include all items inside." else ""
                Text("Do you want to move \"${entry.name}\" to the trash bin?$detail You can restore it later from the Trash Bin.")
            },
            confirmButton = {
                TextButton(onClick = { vm.confirmDelete() }) { 
                    Text("Move to Trash", color = MaterialTheme.colorScheme.error) 
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.cancelDelete() }) { Text("Cancel") }
            }
        )
    }

    state.pendingBulkDelete?.let { entries ->
        AlertDialog(
            onDismissRequest = { vm.cancelDelete() },
            title = { Text("Move ${entries.size} items to Trash?") },
            text = {
                Text("Selected items will be moved to the trash bin. You can restore them later.")
            },
            confirmButton = {
                TextButton(onClick = { vm.confirmDelete() }) { 
                    Text("Move to Trash", color = MaterialTheme.colorScheme.error) 
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.cancelDelete() }) { Text("Cancel") }
            }
        )
    }

    if (showViewModeDialog) {
        val viewModes = listOf("List", "Grid", "Small Icons", "Columns")
        SingleSelectDialog(
            title = "View Mode",
            options = viewModes,
            selectedOption = EnginePrefs.getViewMode(screenContext),
            onDismiss = { showViewModeDialog = false },
            onSelect = { mode ->
                EnginePrefs.setViewMode(screenContext, mode)
                vm.onSettingsChanged()
                showViewModeDialog = false
            }
        )
    }

    if (showSortDialog) {
        SortDialog(
            currentSortBy = state.sortBy,
            currentSortOrder = state.sortOrder,
            onDismiss = { showSortDialog = false },
            onSortChanged = { sortBy, order ->
                vm.setSortBy(sortBy)
                vm.setSortOrder(order)
                showSortDialog = false
            }
        )
    }

    if (showBulkRenameDialog) {
        AlertDialog(
            onDismissRequest = { showBulkRenameDialog = false },
            title = { Text("Bulk Rename (${state.selectedItems.size} files)") },
            text = {
                Column {
                    OutlinedTextField(
                        value = bulkRenamePattern,
                        onValueChange = { bulkRenamePattern = it },
                        label = { Text("Find pattern (e.g. .jpg)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bulkRenameReplacement,
                        onValueChange = { bulkRenameReplacement = it },
                        label = { Text("Replace with (e.g. _backup.jpg)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.bulkRename(bulkRenamePattern, bulkRenameReplacement)
                    showBulkRenameDialog = false
                }) { Text("Rename All") }
            },
            dismissButton = {
                TextButton(onClick = { showBulkRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCreateArchiveDialog || showArchiveOptions) {
        val initialName = remember(state.selectedItems) {
            if (state.selectedItems.size == 1) state.selectedItems.first().name
            else "archive"
        }
        ArchiveOptionsDialog(
            initialName = initialName,
            selectedItemsCount = if (showArchiveOptions) 1 else state.selectedItems.size,
            onDismiss = { 
                showCreateArchiveDialog = false
                showArchiveOptions = false
            },
            onConfirm = { name, level, format, password ->
                vm.createArchive(name, level, format, password)
                showCreateArchiveDialog = false
                showArchiveOptions = false
            }
        )
    }

    state.showDetails?.let { entry ->
        DetailsDialog(
            entry = entry,
            detailedSize = state.detailedSize,
            onDismiss = { vm.dismissDetails() }
        )
    }

    showTagEditor?.let { path ->
        database?.let { db ->
            TagEditorDialog(
                path = path,
                database = db,
                onDismiss = { showTagEditor = null }
            )
        }
    }
}

@Composable
private fun ArchiveOptionsDialog(
    initialName: String,
    selectedItemsCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String, String?) -> Unit
) {
    var archiveName by remember { mutableStateOf(initialName) }
    var compressionLevel by remember { mutableFloatStateOf(6f) }
    var format by remember { mutableStateOf("zip") }
    var encrypt by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Archive ($selectedItemsCount items)") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = archiveName,
                    onValueChange = { archiveName = it },
                    label = { Text("Archive name") },
                    modifier = Modifier.fillMaxWidth(),
                    suffix = { Text(".$format") }
                )
                
                Spacer(Modifier.height(16.dp))
                Text("Format", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("zip", "7z", "tar").forEach { f ->
                        FilterChip(
                            selected = format == f,
                            onClick = { format = f },
                            label = { Text(f.uppercase()) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Compression Level: ${compressionLevel.toInt()}", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = compressionLevel,
                    onValueChange = { compressionLevel = it },
                    valueRange = 0f..9f,
                    steps = 8
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Fastest", style = MaterialTheme.typography.labelSmall)
                    Text("Best", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = encrypt, onCheckedChange = { encrypt = it }, modifier = Modifier.scale(0.8f))
                    Text("Encrypt Archive", style = MaterialTheme.typography.bodyMedium)
                }

                if (encrypt) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(archiveName, compressionLevel.toInt(), format, if (encrypt) password else null) },
                enabled = archiveName.isNotBlank() && (!encrypt || password.isNotBlank())
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DetailsDialog(
    entry: FileEntry,
    detailedSize: Long?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Details") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                DetailItem("Name", entry.name)
                DetailItem("Type", if (entry.isDirectory) "Folder" else "File (${entry.extension.uppercase()})")
                DetailItem("Path", entry.path)
                
                val sizeText = when {
                    detailedSize != null -> formatSize(detailedSize)
                    entry.isDirectory -> "Calculating..."
                    else -> formatSize(entry.sizeBytes)
                }
                DetailItem("Size", sizeText)
                
                DetailItem("Modified", entry.modified)
                if (entry.permissions.isNotBlank()) {
                    DetailItem("Permissions", entry.permissions)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
@Composable
private fun SortDialog(
    currentSortBy: SortBy,
    currentSortOrder: SortOrder,
    onDismiss: () -> Unit,
    onSortChanged: (SortBy, SortOrder) -> Unit
) {
    var sortBy by remember { mutableStateOf(currentSortBy) }
    var sortOrder by remember { mutableStateOf(currentSortOrder) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort By") },
        text = {
            Column {
                SortBy.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { sortBy = option },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sortBy == option, onClick = { sortBy = option }, modifier = Modifier.scale(0.8f))
                        Text(option.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SortOrder.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { sortOrder = option },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sortOrder == option, onClick = { sortOrder = option }, modifier = Modifier.scale(0.8f))
                        Text(option.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSortChanged(sortBy, sortOrder) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun FileGridItem(
    entry: FileEntry,
    engineType: EngineType,
    thumbnailsEnabled: Boolean,
    showExtensions: Boolean,
    isSelected: Boolean,
    tags: List<String> = emptyList(),
    itemSizeMultiplier: Float = 1.0f,
    fontSizeMultiplier: Float = 1.0f,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var appIcon by remember(entry.path) { mutableStateOf<Drawable?>(null) }
    var thumbnail by remember(entry.path) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var folderLabel by remember(entry.path, entry.name) {
        mutableStateOf(if (entry.isDirectory) FolderCategoryDetector.getFolderLabelByName(entry.name, entry.path) else "")
    }

    val displayName = remember(entry.name, showExtensions, entry.isDirectory) {
        if (entry.isDirectory || showExtensions || entry.isArchive) entry.name
        else entry.name.substringBeforeLast(".", entry.name)
    }

    LaunchedEffect(entry.path, entry.isDirectory) {
        if (entry.isDirectory) {
            val label = FolderCategoryDetector.getFolderLabel(entry)
            if (label.isNotEmpty()) {
                folderLabel = label
            }
        }
    }

    LaunchedEffect(entry.path, thumbnailsEnabled) {
        if (thumbnailsEnabled && ThumbnailManager.isThumbnailSupported(entry)) {
            withContext(Dispatchers.IO) {
                thumbnail = ThumbnailManager.loadThumbnail(context, entry, engineType)
            }
        } else if (entry.isApk) {
            withContext(Dispatchers.IO) {
                appIcon = AppManager.getApkIcon(context, entry.path)
            }
        } else {
            thumbnail = null
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Box {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(80.dp * itemSizeMultiplier),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        appIcon != null -> {
                            Image(
                                bitmap = appIcon!!.toBitmap().asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        thumbnail != null -> {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    bitmap = thumbnail!!.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (entry.isVideo) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayCircle,
                                        contentDescription = "Video",
                                        modifier = Modifier.size(32.dp * itemSizeMultiplier),
                                        tint = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                        entry.isArchive -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_zip_file),
                                contentDescription = "ZIP File",
                                modifier = Modifier.size(72.dp * itemSizeMultiplier),
                                tint = Color.Unspecified
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = when {
                                    entry.isSymlink -> Icons.Filled.Link
                                    entry.isDirectory -> Icons.Filled.Folder
                                    entry.isVideo -> Icons.Filled.VideoFile
                                    else -> Icons.AutoMirrored.Filled.InsertDriveFile
                                },
                                contentDescription = null,
                                modifier = Modifier.size(72.dp * itemSizeMultiplier),
                                tint = when {
                                    entry.isDirectory -> MaterialTheme.colorScheme.primary
                                    entry.isVideo -> Color(0xFFE91E63) // Pink/Red for video
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize * fontSizeMultiplier,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (entry.isDirectory && folderLabel.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = folderLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 7.5.sp * fontSizeMultiplier,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                if (tags.isNotEmpty()) {
                    Text(
                        text = tags.joinToString(", "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 8.sp * fontSizeMultiplier,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun FileRow(
    entry: FileEntry,
    engineType: EngineType,
    thumbnailsEnabled: Boolean,
    showExtensions: Boolean,
    isSelected: Boolean,
    tags: List<String> = emptyList(),
    itemSizeMultiplier: Float = 1.0f,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var appIcon by remember(entry.path) { mutableStateOf<Drawable?>(null) }
    var thumbnail by remember(entry.path) { mutableStateOf<Bitmap?>(null) }
    var folderLabel by remember(entry.path, entry.name) {
        mutableStateOf(if (entry.isDirectory) FolderCategoryDetector.getFolderLabelByName(entry.name, entry.path) else "")
    }

    val displayName = remember(entry.name, showExtensions, entry.isDirectory) {
        if (entry.isDirectory || showExtensions || entry.isArchive) entry.name
        else entry.name.substringBeforeLast(".", entry.name)
    }

    LaunchedEffect(entry.path, entry.isDirectory) {
        if (entry.isDirectory) {
            val label = FolderCategoryDetector.getFolderLabel(entry)
            if (label.isNotEmpty()) {
                folderLabel = label
            }
        }
    }

    LaunchedEffect(entry.path, thumbnailsEnabled) {
        if (thumbnailsEnabled && ThumbnailManager.isThumbnailSupported(entry)) {
            withContext(Dispatchers.IO) {
                thumbnail = ThumbnailManager.loadThumbnail(context, entry, engineType)
            }
        } else if (entry.isApk) {
            withContext(Dispatchers.IO) {
                appIcon = AppManager.getApkIcon(context, entry.path)
            }
        } else {
            thumbnail = null
        }
    }

    val subtitleText = remember(entry.modified, entry.sizeBytes, entry.isDirectory, tags, folderLabel) {
        val base = if (entry.isDirectory) {
            if (folderLabel.isNotEmpty()) "$folderLabel · ${entry.modified}" else entry.modified
        } else "${formatSize(entry.sizeBytes)} · ${entry.modified}"
        if (tags.isNotEmpty()) "$base · ${tags.joinToString(", ")}" else base
    }

    ModernListItem(
        title = displayName,
        subtitle = subtitleText,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent),
        leadingIcon = {
            Surface(
                modifier = Modifier.size(42.dp * itemSizeMultiplier),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when {
                        appIcon != null -> {
                            Image(
                                bitmap = appIcon!!.toBitmap().asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp * itemSizeMultiplier)
                            )
                        }
                        thumbnail != null -> {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    bitmap = thumbnail!!.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                if (entry.isVideo) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayCircle,
                                        contentDescription = "Video",
                                        modifier = Modifier.size(16.dp * itemSizeMultiplier),
                                        tint = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                        entry.isArchive -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_zip_file),
                                contentDescription = "ZIP File",
                                modifier = Modifier.size(30.dp * itemSizeMultiplier),
                                tint = Color.Unspecified
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = when {
                                    entry.isSymlink -> Icons.Filled.Link
                                    entry.isDirectory -> Icons.Filled.Folder
                                    entry.isVideo -> Icons.Filled.VideoFile
                                    else -> Icons.AutoMirrored.Filled.InsertDriveFile
                                },
                                contentDescription = null,
                                modifier = Modifier.size(30.dp * itemSizeMultiplier),
                                tint = when {
                                    entry.isDirectory -> MaterialTheme.colorScheme.primary
                                    entry.isVideo -> Color(0xFFE91E63)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entry.isDirectory && folderLabel.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = folderLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun FileColumnHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Name", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text("Size", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text("Type", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text("Date", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FileColumnRow(
    entry: FileEntry,
    isSelected: Boolean,
    itemSizeMultiplier: Float = 1.0f,
    fontSizeMultiplier: Float = 1.0f,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var folderLabel by remember(entry.path, entry.name) {
        mutableStateOf(if (entry.isDirectory) FolderCategoryDetector.getFolderLabelByName(entry.name, entry.path) else "")
    }

    LaunchedEffect(entry.path, entry.isDirectory) {
        if (entry.isDirectory) {
            val label = FolderCategoryDetector.getFolderLabel(entry)
            if (label.isNotEmpty()) {
                folderLabel = label
            }
        }
    }

    val fileType = remember(entry.name, entry.isDirectory, folderLabel) {
        if (entry.isDirectory) {
            if (folderLabel.isNotEmpty()) folderLabel else "Folder"
        }
        else entry.name.substringAfterLast(".", "File").uppercase()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically) {
            if (entry.isArchive) {
                Icon(
                    painter = painterResource(R.drawable.ic_zip_file),
                    contentDescription = "ZIP File",
                    modifier = Modifier.size(30.dp * itemSizeMultiplier),
                    tint = Color.Unspecified
                )
            } else {
                Icon(
                    imageVector = when {
                        entry.isDirectory -> Icons.Filled.Folder
                        else -> Icons.AutoMirrored.Filled.InsertDriveFile
                    },
                    contentDescription = null,
                    modifier = Modifier.size(30.dp * itemSizeMultiplier),
                    tint = when {
                        entry.isDirectory -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                entry.name, 
                style = MaterialTheme.typography.bodyMedium, 
                fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontSizeMultiplier,
                maxLines = 1, 
                modifier = Modifier.basicMarquee()
            )
            if (entry.isDirectory && folderLabel.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = folderLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp * fontSizeMultiplier,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
        Text(
            if (entry.isDirectory) "--" else formatSize(entry.sizeBytes),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontSize = MaterialTheme.typography.bodySmall.fontSize * fontSizeMultiplier
        )
        Text(
            fileType,
            modifier = Modifier.weight(0.8f),
            style = MaterialTheme.typography.bodySmall,
            fontSize = MaterialTheme.typography.bodySmall.fontSize * fontSizeMultiplier,
            maxLines = 1
        )
        Text(
            entry.modified.substringBefore(" "), // Just date
            modifier = Modifier.weight(1.5f),
            style = MaterialTheme.typography.bodySmall,
            fontSize = MaterialTheme.typography.bodySmall.fontSize * fontSizeMultiplier,
            maxLines = 1
        )
        if (isSelected) {
            Icon(
                Icons.Filled.CheckCircle,
                null,
                modifier = Modifier.size(24.dp).padding(4.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}



private data class PathSegment(val name: String, val path: String)

@Composable
private fun PathBar(
    engine: FileEngine,
    currentPath: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    lockedRoot: String? = null,
    fontSizeMultiplier: Float = 1.0f
) {
    val segments = remember(currentPath, engine, lockedRoot) {
        val list = mutableListOf<PathSegment>()
        var p: String? = currentPath
        while (p != null) {
            list.add(0, PathSegment(engine.getDisplayName(p), p))
            
            // Anchor at lockedRoot or rootPath
            if (p == lockedRoot) break
            if (p == engine.rootPath) break
            if (p == "/") break
            
            val parent = engine.parentPath(p)
            if (parent == p) break // Prevent infinite loop if engine is misbehaving
            p = parent
            
            if (list.size > 50) break // Practical limit for breadcrumbs
        }
        list
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        segments.forEachIndexed { index, segment ->
            Surface(
                onClick = { onNavigate(segment.path) },
                color = if (index == segments.lastIndex) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = CircleShape,
                tonalElevation = if (index == segments.lastIndex) 4.dp else 0.dp
            ) {
                Text(
                    text = segment.name,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = MaterialTheme.typography.labelLarge.fontSize * fontSizeMultiplier,
                    color = if (index == segments.lastIndex) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (index == segments.lastIndex) FontWeight.Bold else FontWeight.Medium
                )
            }
            if (index < segments.lastIndex) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun RestrictedView(
    path: String,
    onSwitchToShizuku: () -> Unit,
    onStartShizukuApp: () -> Unit,
    modifier: Modifier = Modifier,
    fontSizeMultiplier: Float = 1.0f,
    itemSizeMultiplier: Float = 1.0f
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Security,
            contentDescription = null,
            modifier = Modifier.size(100.dp * itemSizeMultiplier),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Access Restricted",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            fontSize = MaterialTheme.typography.headlineSmall.fontSize * fontSizeMultiplier,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Android restricts access to folder:\n$path",
            style = MaterialTheme.typography.bodyLarge,
            fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontSizeMultiplier,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "To access this folder, you must use Shizuku.",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontSizeMultiplier,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = onStartShizukuApp,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
            Spacer(Modifier.width(8.dp))
            Text("Start Shizuku App")
        }
        
        Spacer(Modifier.height(12.dp))
        
        OutlinedButton(
            onClick = onSwitchToShizuku,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Code, null)
            Spacer(Modifier.width(8.dp))
            Text("Switch to Shizuku Mode")
        }
    }
}

@Composable
private fun EmptyFolderView(
    modifier: Modifier = Modifier,
    fontSizeMultiplier: Float = 1.0f,
    itemSizeMultiplier: Float = 1.0f
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            modifier = Modifier.size(120.dp * itemSizeMultiplier),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Empty folder",
            style = MaterialTheme.typography.titleMedium,
            fontSize = MaterialTheme.typography.titleMedium.fontSize * fontSizeMultiplier,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Create a new file or folder to get started",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontSizeMultiplier,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    fontSizeMultiplier: Float = 1.0f,
    itemSizeMultiplier: Float = 1.0f
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            modifier = Modifier.size(60.dp * itemSizeMultiplier),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            fontSize = MaterialTheme.typography.titleMedium.fontSize * fontSizeMultiplier,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontSizeMultiplier,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text(
                "Retry",
                fontSize = MaterialTheme.typography.labelLarge.fontSize * fontSizeMultiplier
            )
        }
    }
}
