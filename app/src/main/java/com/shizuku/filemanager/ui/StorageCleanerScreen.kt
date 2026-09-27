package com.shizuku.filemanager.ui

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.shizuku.filemanager.fs.FileEntry
import com.shizuku.filemanager.fs.engine.EngineType
import com.shizuku.filemanager.sys.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageCleanerScreen(
    onBack: () -> Unit,
    onOpenFile: (FileEntry) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
    }

    // Shared scanner state across tabs to build hero dashboard
    var duplicatesList by remember { mutableStateOf<List<DuplicateGroup>>(emptyList()) }
    var largeFilesList by remember { mutableStateOf<List<LargeFile>>(emptyList()) }
    var unusedAppsList by remember { mutableStateOf<List<UnusedApp>>(emptyList()) }

    var selectedDuplicates by remember { mutableStateOf<Set<File>>(emptySet()) }
    var selectedLargeFiles by remember { mutableStateOf<Set<LargeFile>>(emptySet()) }
    var selectedUnusedApps by remember { mutableStateOf<Set<UnusedApp>>(emptySet()) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isCleaningInProgress by remember { mutableStateOf(false) }

    // Calculate total cleanable size
    val totalDuplicateWaste = remember(duplicatesList) {
        duplicatesList.sumOf { it.totalSizeBytes }
    }
    val totalLargeFilesSize = remember(largeFilesList) {
        largeFilesList.sumOf { it.sizeBytes }
    }
    val totalSelectedSize = remember(selectedDuplicates, selectedLargeFiles) {
        val dupBytes = selectedDuplicates.sumOf { it.length() }
        val largeBytes = selectedLargeFiles.sumOf { it.sizeBytes }
        dupBytes + largeBytes
    }
    val totalSelectedItemsCount = remember(selectedDuplicates, selectedLargeFiles, selectedUnusedApps) {
        selectedDuplicates.size + selectedLargeFiles.size + selectedUnusedApps.size
    }

    ScreenScaffold(
        topBar = {
            ModernTopBar(
                title = "Storage Cleaner",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Rescan trigger
                        duplicatesList = emptyList()
                        largeFilesList = emptyList()
                        selectedDuplicates = emptySet()
                        selectedLargeFiles = emptySet()
                        selectedUnusedApps = emptySet()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rescan Storage")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!hasStoragePermission) {
                CleanerPermissionState(
                    onGrantPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } else {
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                )
            } else {
                Column(Modifier.fillMaxSize()) {
                    // HERO DASHBOARD (NO CARDS - Seamless surface layout)
                    CleanerHeroHeader(
                        totalReclaimableBytes = totalDuplicateWaste + totalLargeFilesSize,
                        duplicateCount = duplicatesList.sumOf { it.files.size - 1 }.coerceAtLeast(0),
                        duplicateSizeFormatted = formatSize(totalDuplicateWaste),
                        largeFileCount = largeFilesList.size,
                        largeFileSizeFormatted = formatSize(totalLargeFilesSize),
                        unusedAppsCount = unusedAppsList.size,
                        selectedTab = selectedTab,
                        onTabSelect = { selectedTab = it }
                    )

                    // TAB CONTENTS
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (selectedTab) {
                            0 -> DuplicateFilesTab(
                                duplicates = duplicatesList,
                                onDuplicatesLoaded = { duplicatesList = it },
                                selectedFiles = selectedDuplicates,
                                onSelectionChanged = { selectedDuplicates = it },
                                onOpenFile = onOpenFile
                            )
                            1 -> LargeFilesTab(
                                largeFiles = largeFilesList,
                                onLargeFilesLoaded = { largeFilesList = it },
                                selectedFiles = selectedLargeFiles,
                                onSelectionChanged = { selectedLargeFiles = it },
                                onOpenFile = onOpenFile
                            )
                            2 -> UnusedAppsTab(
                                unusedApps = unusedAppsList,
                                onUnusedAppsLoaded = { unusedAppsList = it },
                                selectedApps = selectedUnusedApps,
                                onSelectionChanged = { selectedUnusedApps = it }
                            )
                        }
                    }
                }

                // FLOATING BATCH CLEAN ACTION BAR (NO CARDS)
                AnimatedVisibility(
                    visible = totalSelectedItemsCount > 0,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    BatchCleanActionBar(
                        selectedCount = totalSelectedItemsCount,
                        selectedSizeFormatted = formatSize(totalSelectedSize),
                        onCleanClick = { showDeleteConfirmDialog = true }
                    )
                }
            }
        }
    }

    // CONFIRMATION DIALOG FOR BATCH CLEAN
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCleaningInProgress) showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Delete Selected Items?",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "Are you sure you want to clean $totalSelectedItemsCount item(s)?" +
                            if (totalSelectedSize > 0) " This will permanently free up ${formatSize(totalSelectedSize)}." else "",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isCleaningInProgress = true
                        scope.launch(Dispatchers.IO) {
                            // Process deletion
                            val filesToDelete = selectedDuplicates.toList() + selectedLargeFiles.map { it.file }
                            filesToDelete.forEach { file ->
                                try {
                                    if (file.exists()) file.delete()
                                } catch (_: Exception) {}
                            }

                            withContext(Dispatchers.Main) {
                                // Update states
                                duplicatesList = duplicatesList.map { group ->
                                    group.copy(files = group.files.filter { it !in selectedDuplicates })
                                }.filter { it.files.size > 1 }

                                largeFilesList = largeFilesList.filter { it !in selectedLargeFiles }

                                selectedDuplicates = emptySet()
                                selectedLargeFiles = emptySet()
                                selectedUnusedApps = emptySet()

                                isCleaningInProgress = false
                                showDeleteConfirmDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isCleaningInProgress
                ) {
                    if (isCleaningInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    enabled = !isCleaningInProgress
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// HERO DASHBOARD HEADER (STRICTLY NO CARDS)
// ==========================================
@Composable
fun CleanerHeroHeader(
    totalReclaimableBytes: Long,
    duplicateCount: Int,
    duplicateSizeFormatted: String,
    largeFileCount: Int,
    largeFileSizeFormatted: String,
    unusedAppsCount: Int,
    selectedTab: Int,
    onTabSelect: (Int) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.05f)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing Circular Gauge Center
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(pulseScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                tonalElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = formatSize(totalReclaimableBytes),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Total Cleanable Space",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        // Horizontal Summary Stats Pills (Clickable to switch tab)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                StatPill(
                    title = "Duplicates",
                    value = duplicateSizeFormatted,
                    countText = "$duplicateCount files",
                    icon = Icons.Default.CopyAll,
                    accentColor = Color(0xFFFFB74D),
                    isSelected = selectedTab == 0,
                    onClick = { onTabSelect(0) }
                )
            }
            item {
                StatPill(
                    title = "Large Files",
                    value = largeFileSizeFormatted,
                    countText = "$largeFileCount files",
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    accentColor = Color(0xFFE57373),
                    isSelected = selectedTab == 1,
                    onClick = { onTabSelect(1) }
                )
            }
            item {
                StatPill(
                    title = "Unused Apps",
                    value = "$unusedAppsCount apps",
                    countText = "To review",
                    icon = Icons.Default.Apps,
                    accentColor = Color(0xFF64B5F6),
                    isSelected = selectedTab == 2,
                    onClick = { onTabSelect(2) }
                )
            }
        }
    }
}

@Composable
fun StatPill(
    title: String,
    value: String,
    countText: String,
    icon: ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = accentColor
            )
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = " · $countText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==========================================
// TAB 1: DUPLICATE FILES TAB
// ==========================================
@Composable
fun DuplicateFilesTab(
    duplicates: List<DuplicateGroup>,
    onDuplicatesLoaded: (List<DuplicateGroup>) -> Unit,
    selectedFiles: Set<File>,
    onSelectionChanged: (Set<File>) -> Unit,
    onOpenFile: (FileEntry) -> Unit
) {
    var isLoading by remember { mutableStateOf(duplicates.isEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (duplicates.isEmpty()) {
            try {
                val result = withContext(Dispatchers.IO) {
                    StorageScanner.getDuplicateFiles(Environment.getExternalStorageDirectory())
                }
                onDuplicatesLoaded(result)
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Failed to scan duplicate files."
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    if (isLoading) {
        CleanerLoadingState("Scanning storage for duplicate files...")
    } else if (error != null) {
        EmptyState("Error: $error", Icons.Default.ErrorOutline)
    } else if (duplicates.isEmpty()) {
        EmptyState("No duplicate files found", Icons.Default.CheckCircle)
    } else {
        Column(Modifier.fillMaxSize()) {
            // Auto Select Toolbar Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${duplicates.size} Duplicate Group(s)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = {
                        // Smart Auto-Select: Select all files except the first one in each group
                        val autoSelected = duplicates.flatMap { group ->
                            group.files.drop(1)
                        }.toSet()
                        onSelectionChanged(autoSelected)
                    }
                ) {
                    Icon(
                        Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Auto-Select Duplicates")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    items = duplicates,
                    key = { "${it.hash}_${it.files.firstOrNull()?.absolutePath ?: ""}" }
                ) { group ->
                    DuplicateGroupSection(
                        group = group,
                        selectedFiles = selectedFiles,
                        onSelectionChanged = onSelectionChanged,
                        onOpenFile = onOpenFile,
                        onDeleteFile = { fileToDelete ->
                            fileToDelete.delete()
                            val updatedGroup = group.copy(files = group.files.filter { it != fileToDelete })
                            val newDuplicates = duplicates.map { if (it === group || (it.hash == group.hash && it.files == group.files)) updatedGroup else it }
                                .filter { it.files.size > 1 }
                            onDuplicatesLoaded(newDuplicates)
                            onSelectionChanged(selectedFiles - fileToDelete)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DuplicateGroupSection(
    group: DuplicateGroup,
    selectedFiles: Set<File>,
    onSelectionChanged: (Set<File>) -> Unit,
    onOpenFile: (FileEntry) -> Unit,
    onDeleteFile: (File) -> Unit
) {
    val allGroupFilesSelected = remember(group, selectedFiles) {
        group.files.all { it in selectedFiles }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Full-bleed Section Bar Header (NO CARDS)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFFFB74D))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${group.files.size} Copies",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "(${group.totalSizeFormatted} waste)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            TextButton(
                onClick = {
                    if (allGroupFilesSelected) {
                        onSelectionChanged(selectedFiles - group.files.toSet())
                    } else {
                        onSelectionChanged(selectedFiles + group.files.drop(1).toSet())
                    }
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = if (allGroupFilesSelected) "Deselect All" else "Select Duplicates",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Group File Items
        group.files.forEachIndexed { index, file ->
            val isChecked = file in selectedFiles
            ModernSelectableFileRow(
                file = file,
                isSelected = isChecked,
                isOriginalTag = index == 0,
                onCheckedChange = { checked ->
                    if (checked) {
                        onSelectionChanged(selectedFiles + file)
                    } else {
                        onSelectionChanged(selectedFiles - file)
                    }
                },
                onOpenFile = onOpenFile,
                onDelete = { onDeleteFile(file) }
            )
            if (index < group.files.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

// ==========================================
// TAB 2: LARGE FILES TAB
// ==========================================
@Composable
fun LargeFilesTab(
    largeFiles: List<LargeFile>,
    onLargeFilesLoaded: (List<LargeFile>) -> Unit,
    selectedFiles: Set<LargeFile>,
    onSelectionChanged: (Set<LargeFile>) -> Unit,
    onOpenFile: (FileEntry) -> Unit
) {
    var isLoading by remember { mutableStateOf(largeFiles.isEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val categories = listOf("All", "Videos", "Images", "Audio", "Archives/APKs")

    LaunchedEffect(Unit) {
        if (largeFiles.isEmpty()) {
            try {
                val result = withContext(Dispatchers.IO) {
                    StorageScanner.getLargeFiles(Environment.getExternalStorageDirectory())
                }
                onLargeFilesLoaded(result)
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Failed to scan large files."
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    val filteredFiles = remember(largeFiles, selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "Videos" -> largeFiles.filter { isVideoFile(it.file) }
            "Images" -> largeFiles.filter { isImageFile(it.file) }
            "Audio" -> largeFiles.filter { isAudioFile(it.file) }
            "Archives/APKs" -> largeFiles.filter { isArchiveOrApk(it.file) }
            else -> largeFiles
        }
    }

    val allFilteredSelected = remember(filteredFiles, selectedFiles) {
        filteredFiles.isNotEmpty() && filteredFiles.all { it in selectedFiles }
    }

    if (isLoading) {
        CleanerLoadingState("Scanning storage for large files (>20MB)...")
    } else if (error != null) {
        EmptyState("Error: $error", Icons.Default.ErrorOutline)
    } else if (largeFiles.isEmpty()) {
        EmptyState("No large files found", Icons.Default.CheckCircle)
    } else {
        Column(Modifier.fillMaxSize()) {
            // Filter Chips Bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(cat, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            // Selection Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredFiles.size} File(s) Found",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = {
                        if (allFilteredSelected) {
                            onSelectionChanged(selectedFiles - filteredFiles.toSet())
                        } else {
                            onSelectionChanged(selectedFiles + filteredFiles.toSet())
                        }
                    }
                ) {
                    Text(if (allFilteredSelected) "Deselect All" else "Select All")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredFiles, key = { it.file.absolutePath }) { item ->
                    val isChecked = item in selectedFiles
                    ModernSelectableFileRow(
                        file = item.file,
                        isSelected = isChecked,
                        customSizeText = item.sizeFormatted,
                        onCheckedChange = { checked ->
                            if (checked) {
                                onSelectionChanged(selectedFiles + item)
                            } else {
                                onSelectionChanged(selectedFiles - item)
                            }
                        },
                        onOpenFile = onOpenFile,
                        onDelete = {
                            item.file.delete()
                            onLargeFilesLoaded(largeFiles.filter { it != item })
                            onSelectionChanged(selectedFiles - item)
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

// Helper file category predicates
private fun isVideoFile(file: File): Boolean {
    val ext = file.extension.lowercase(Locale.ROOT)
    return ext in setOf("mp4", "mkv", "webm", "avi", "3gp", "mov", "m4v")
}

private fun isImageFile(file: File): Boolean {
    val ext = file.extension.lowercase(Locale.ROOT)
    return ext in setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "bmp")
}

private fun isAudioFile(file: File): Boolean {
    val ext = file.extension.lowercase(Locale.ROOT)
    return ext in setOf("mp3", "wav", "flac", "m4a", "ogg", "aac", "opus")
}

private fun isArchiveOrApk(file: File): Boolean {
    val ext = file.extension.lowercase(Locale.ROOT)
    return ext in setOf("apk", "xapk", "zip", "rar", "7z", "tar", "gz")
}

// ==========================================
// TAB 3: UNUSED APPS TAB
// ==========================================
@Composable
fun UnusedAppsTab(
    unusedApps: List<UnusedApp>,
    onUnusedAppsLoaded: (List<UnusedApp>) -> Unit,
    selectedApps: Set<UnusedApp>,
    onSelectionChanged: (Set<UnusedApp>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(unusedApps.isEmpty()) }
    var hasPermission by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val checkAndLoad = rememberUpdatedState {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )

        if (mode == AppOpsManager.MODE_ALLOWED) {
            hasPermission = true
            if (unusedApps.isEmpty()) {
                isLoading = true
                scope.launch {
                    try {
                        val apps = AppManager.getUnusedApps(context)
                        onUnusedAppsLoaded(apps)
                    } catch (_: Exception) {
                    } finally {
                        isLoading = false
                    }
                }
            }
        } else {
            hasPermission = false
            isLoading = false
        }
    }

    val uninstallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        checkAndLoad.value()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkAndLoad.value()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!hasPermission) {
        PermissionRequestState {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            context.startActivity(intent)
        }
    } else if (isLoading) {
        CleanerLoadingState("Analyzing app usage statistics...")
    } else if (unusedApps.isEmpty()) {
        EmptyState("No unused apps found! All apps active.", Icons.Default.SentimentSatisfiedAlt)
    } else {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${unusedApps.size} Unused App(s)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(unusedApps, key = { it.appInfo.packageName }) { item ->
                    ModernUnusedAppRow(
                        item = item,
                        onUninstall = {
                            uninstallLauncher.launch(AppManager.getUninstallIntent(item.appInfo.packageName))
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 64.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

// ==========================================
// REUSABLE MODERN LIST ROWS (STRICTLY NO CARDS)
// ==========================================
@Composable
fun ModernSelectableFileRow(
    file: File,
    isSelected: Boolean,
    isOriginalTag: Boolean = false,
    customSizeText: String? = null,
    onCheckedChange: (Boolean) -> Unit,
    onOpenFile: (FileEntry) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val entry = remember(file) { file.toFileEntry(locale) }

    var appIcon by remember(entry.path) { mutableStateOf<Drawable?>(null) }
    var thumbnail by remember(entry.path) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(entry.path) {
        if (ThumbnailManager.isImage(entry) || entry.isVideo) {
            withContext(Dispatchers.IO) {
                thumbnail = ThumbnailManager.loadThumbnail(context, entry, EngineType.STANDARD)
            }
        } else if (entry.isApk) {
            withContext(Dispatchers.IO) {
                appIcon = AppManager.getApkIcon(context, entry.path)
            }
        }
    }

    val iconBitmap = remember(appIcon) {
        appIcon?.let {
            try {
                it.toBitmap().asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenFile(entry) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onCheckedChange
        )

        Spacer(Modifier.width(8.dp))

        // Icon / Thumbnail Pill Box (NO CARDS)
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                when {
                    iconBitmap != null -> {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    thumbnail != null -> {
                        Image(
                            bitmap = thumbnail!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = when {
                                entry.isDirectory -> Icons.Default.Folder
                                entry.isArchive -> Icons.Default.Archive
                                entry.isVideo -> Icons.Default.VideoFile
                                entry.isImage -> Icons.Default.Image
                                entry.isAudio -> Icons.Default.AudioFile
                                else -> Icons.AutoMirrored.Filled.InsertDriveFile
                            },
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = when {
                                entry.isDirectory -> MaterialTheme.colorScheme.primary
                                entry.isArchive -> Color(0xFFFF8F00)
                                entry.isVideo -> Color(0xFFE91E63)
                                entry.isImage -> Color(0xFF4CAF50)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isOriginalTag) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = "KEEP",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = file.parent ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = customSizeText ?: formatSize(file.length()),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete File",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ModernUnusedAppRow(
    item: UnusedApp,
    onUninstall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncAppIcon(item.appInfo.packageName, Modifier.size(44.dp))

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.appInfo.label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        item.daysSinceLastUse > 90 -> MaterialTheme.colorScheme.errorContainer
                        item.daysSinceLastUse > 30 -> Color(0xFFFFF3E0)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when {
                            item.lastUsed == 0L -> "Never used"
                            item.daysSinceLastUse == 1 -> "Unused for 1 day"
                            else -> "Unused for ${item.daysSinceLastUse} days"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = when {
                                item.daysSinceLastUse > 90 -> MaterialTheme.colorScheme.onErrorContainer
                                item.daysSinceLastUse > 30 -> Color(0xFFE65100)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = item.appInfo.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(
            onClick = onUninstall,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Uninstall", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

// ==========================================
// FLOATING BATCH CLEAN BAR (NO CARDS)
// ==========================================
@Composable
fun BatchCleanActionBar(
    selectedCount: Int,
    selectedSizeFormatted: String,
    onCleanClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$selectedCount Item(s) Selected",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (selectedSizeFormatted != "0 B") {
                    Text(
                        text = "Frees up $selectedSizeFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Button(
                onClick = onCleanClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Clean Selected",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

// ==========================================
// STATES & UTILITIES
// ==========================================
@Composable
fun CleanerLoadingState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyState(message: String, icon: ImageVector) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CleanerPermissionState(onGrantPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Storage Permission Required",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Storage Cleaner needs access to your device files to detect duplicate items, large files, and junk data.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(28.dp))
        GradientButton(
            text = "Grant Permission",
            onClick = onGrantPermission
        )
    }
}

@Composable
fun PermissionRequestState(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Assessment,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Usage Access Required",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "To detect unused applications, please enable Usage Access permissions in system settings.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(28.dp))
        GradientButton(onClick = onClick, text = "Open Settings")
    }
}

private fun File.toFileEntry(locale: Locale): FileEntry {
    return FileEntry(
        name = name,
        path = absolutePath,
        isDirectory = isDirectory,
        isSymlink = false,
        sizeBytes = length(),
        permissions = (if (canRead()) "r" else "") + (if (canWrite()) "w" else "") + (if (canExecute()) "x" else ""),
        owner = "user",
        group = "group",
        modified = SimpleDateFormat("yyyy-MM-dd HH:mm", locale).format(Date(lastModified()))
    )
}
