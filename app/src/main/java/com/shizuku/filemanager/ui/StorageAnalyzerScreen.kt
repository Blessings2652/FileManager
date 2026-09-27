package com.shizuku.filemanager.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shizuku.filemanager.NavScreen
import com.shizuku.filemanager.fs.FileEntry
import com.shizuku.filemanager.fs.engine.EngineType
import com.shizuku.filemanager.fs.engine.StandardFileEngine
import com.shizuku.filemanager.sys.AppManager
import com.shizuku.filemanager.sys.AppStorageItem
import com.shizuku.filemanager.sys.FileIntentUtils
import com.shizuku.filemanager.sys.StorageCategory
import com.shizuku.filemanager.sys.StorageScanner
import com.shizuku.filemanager.sys.StorageStats
import com.shizuku.filemanager.sys.ThumbnailManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAnalyzerScreen(
    onBack: () -> Unit,
    onOpenFile: (FileEntry) -> Unit,
    onNavigate: ((NavScreen) -> Unit)? = null,
    viewModel: StorageAnalyzerViewModel = viewModel()
) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()

    // Dialog & Action States
    var fileToRename by remember { mutableStateOf<File?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var fileDetailsToShow by remember { mutableStateOf<File?>(null) }
    var activeActionFile by remember { mutableStateOf<File?>(null) }
    var appSearchQuery by remember { mutableStateOf("") }

    ScreenScaffold(
        topBar = {
            ModernTopBar(
                title = "Smart Storage Analyzer",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    uiState.stats?.let { stats ->
                        StorageOverview(
                            stats = stats,
                            onSelectCategory = { cat -> viewModel.selectCategory(cat) }
                        )
                    }
                }

                item {
                    SectionHeader("Large Files (> 50 MB)")
                }

                if (uiState.largeFiles.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                "No large files found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(uiState.largeFiles.take(15)) { item ->
                        AnalyzerFileItem(
                            file = item.file,
                            size = item.sizeFormatted,
                            onOpenFile = onOpenFile,
                            onMoreOptions = { activeActionFile = item.file }
                        )
                    }
                }

                item {
                    SectionHeader("Old Files (> 6 months)")
                }

                if (uiState.oldFiles.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                "No old files found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(uiState.oldFiles.take(15)) { item ->
                        AnalyzerFileItem(
                            file = item.file,
                            size = item.sizeFormatted,
                            onOpenFile = onOpenFile,
                            onMoreOptions = { activeActionFile = item.file }
                        )
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }

        // Category Files Modal Bottom Sheet
        uiState.selectedCategory?.let { category ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.selectCategory(null) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.88f)
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            val catLower = category.name.lowercase(Locale.ROOT)
                            val subtitle = when {
                                catLower.contains("app") || catLower.contains("tool") -> "${uiState.appStorageItems.size} apps • ${category.sizeFormatted}"
                                catLower.contains("system") -> "Android OS System Firmware • ${category.sizeFormatted}"
                                else -> "${category.fileCount} files • ${category.sizeFormatted}"
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.selectCategory(null) }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider()

                    val catLower = category.name.lowercase(Locale.ROOT)
                    when {
                        catLower.contains("app") || catLower.contains("tool") -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                OutlinedTextField(
                                    value = appSearchQuery,
                                    onValueChange = { appSearchQuery = it },
                                    label = { Text("Search installed apps") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )

                                val filteredApps = remember(uiState.appStorageItems, appSearchQuery) {
                                    if (appSearchQuery.isBlank()) {
                                        uiState.appStorageItems
                                    } else {
                                        uiState.appStorageItems.filter {
                                            it.appInfo.label.contains(appSearchQuery, ignoreCase = true) ||
                                                    it.appInfo.packageName.contains(appSearchQuery, ignoreCase = true) ||
                                                    it.appInfo.category.contains(appSearchQuery, ignoreCase = true)
                                        }
                                    }
                                }

                                if (filteredApps.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No applications found",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(filteredApps) { appItem ->
                                            AppStorageItemRow(
                                                item = appItem,
                                                onClick = {
                                                    try {
                                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                            data = Uri.parse("package:${appItem.appInfo.packageName}")
                                                        }
                                                        context.startActivity(intent)
                                                    } catch (_: Exception) {
                                                        Toast.makeText(context, "Cannot open app details", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        catLower.contains("system") -> {
                            val osDetails = remember { StorageScanner.getRealSystemOsDetails() }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 12.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    text = osDetails.deviceModel,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Android ${osDetails.androidVersion} (API ${osDetails.sdkInt}) • Build ${osDetails.buildId}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "Security Patch: ${osDetails.securityPatch}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Real System Partitions (${osDetails.partitions.size} Mounted)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                osDetails.partitions.forEach { part ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${part.name} (${part.path})",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${part.usedFormatted} / ${part.totalFormatted}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Text(
                                                text = part.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            val partRatio = if (part.totalBytes > 0) part.usedBytes.toFloat() / part.totalBytes.toFloat() else 0f
                                            LinearProgressIndicator(
                                                progress = { partRatio },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(CircleShape),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                Text(
                                    text = "Note: All Android System OS partitions are mounted as read-only by the Linux kernel for device security.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }
                        }

                        else -> {
                            if (uiState.isCategoryLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else if (uiState.categoryFiles.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No files found in ${category.name}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(uiState.categoryFiles) { item ->
                                        AnalyzerFileItem(
                                            file = item.file,
                                            size = item.sizeFormatted,
                                            onOpenFile = onOpenFile,
                                            onMoreOptions = { activeActionFile = item.file }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // File Action BottomSheet
        activeActionFile?.let { targetFile ->
            val entry = remember(targetFile, locale) { targetFile.toFileEntry(locale) }
            val engine = remember { StandardFileEngine(context.applicationContext) }

            ModalBottomSheet(
                onDismissRequest = { activeActionFile = null }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = targetFile.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = StorageScanner.formatSize(targetFile.length()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider()

                    // Open / View Action
                    ListItem(
                        headlineContent = { Text("Open / View") },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) },
                        modifier = Modifier.clickable {
                            activeActionFile = null
                            onOpenFile(entry)
                        }
                    )

                    // Edit Action
                    ListItem(
                        headlineContent = { Text("Edit File") },
                        leadingContent = { Icon(Icons.Default.Edit, null) },
                        modifier = Modifier.clickable {
                            activeActionFile = null
                            if (entry.isImage) {
                                scope.launch {
                                    FileIntentUtils.editWith(context, entry, engine)
                                }
                            } else if (onNavigate != null) {
                                when {
                                    entry.isText || entry.isDocument -> onNavigate(NavScreen.CodeEditor(entry, engine))
                                    entry.isJson -> onNavigate(NavScreen.JsonViewer(entry, engine))
                                    entry.isAudio || entry.isVideo -> onNavigate(NavScreen.MetadataEditor(entry.path))
                                    else -> onNavigate(NavScreen.MetadataEditor(entry.path))
                                }
                            } else {
                                onOpenFile(entry)
                            }
                        }
                    )

                    // Rename Action
                    ListItem(
                        headlineContent = { Text("Rename") },
                        leadingContent = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                        modifier = Modifier.clickable {
                            val f = activeActionFile
                            activeActionFile = null
                            f?.let {
                                fileToRename = it
                                renameInputText = it.name
                            }
                        }
                    )

                    // Share Action
                    ListItem(
                        headlineContent = { Text("Share") },
                        leadingContent = { Icon(Icons.Default.Share, null) },
                        modifier = Modifier.clickable {
                            val f = activeActionFile
                            activeActionFile = null
                            f?.let {
                                scope.launch {
                                    FileIntentUtils.shareFile(context, entry, engine)
                                }
                            }
                        }
                    )

                    // Details Action
                    ListItem(
                        headlineContent = { Text("File Details") },
                        leadingContent = { Icon(Icons.Default.Info, null) },
                        modifier = Modifier.clickable {
                            val f = activeActionFile
                            activeActionFile = null
                            f?.let {
                                fileDetailsToShow = it
                            }
                        }
                    )

                    // Delete Action
                    ListItem(
                        headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            val f = activeActionFile
                            activeActionFile = null
                            f?.let {
                                fileToDelete = it
                            }
                        }
                    )
                }
            }
        }

        // Rename Dialog
        fileToRename?.let { file ->
            AlertDialog(
                onDismissRequest = { fileToRename = null },
                title = { Text("Rename File") },
                text = {
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        label = { Text("New Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val newName = renameInputText.trim()
                            if (newName.isNotEmpty() && newName != file.name) {
                                viewModel.renameFile(file, newName) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Renamed to $newName", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to rename file", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            fileToRename = null
                        }
                    ) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { fileToRename = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        fileToDelete?.let { file ->
            AlertDialog(
                onDismissRequest = { fileToDelete = null },
                title = { Text("Delete File") },
                text = { Text("Are you sure you want to permanently delete \"${file.name}\"?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteFile(file) { success ->
                                if (success) {
                                    Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to delete file", Toast.LENGTH_SHORT).show()
                                }
                            }
                            fileToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { fileToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // File Details Dialog
        fileDetailsToShow?.let { file ->
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale).format(Date(file.lastModified()))
            AlertDialog(
                onDismissRequest = { fileDetailsToShow = null },
                title = { Text("File Details") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Name: ${file.name}", fontWeight = FontWeight.Bold)
                        Text("Path: ${file.absolutePath}", style = MaterialTheme.typography.bodySmall)
                        Text("Size: ${StorageScanner.formatSize(file.length())}")
                        Text("Last Modified: $dateStr")
                        Text("Readable: ${file.canRead()} | Writable: ${file.canWrite()}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { fileDetailsToShow = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun StorageOverview(
    stats: StorageStats,
    onSelectCategory: (StorageCategory) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            "Storage Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(8.dp))

        val percent = if (stats.totalSize > 0) (stats.usedSize.toDouble() / stats.totalSize.toDouble() * 100).toInt() else 0
        LinearProgressIndicator(
            progress = { if (stats.totalSize > 0) stats.usedSize.toFloat() / stats.totalSize.toFloat() else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape),
            color = if (percent > 90) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${stats.usedSizeFormatted} / ${stats.totalSizeFormatted}", style = MaterialTheme.typography.labelSmall)
            Text("$percent%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Categories (Tap to inspect)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        stats.categories.forEach { category ->
            CategoryRow(
                category = category,
                onClick = { onSelectCategory(category) }
            )
            if (stats.categories.last() != category) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@Composable
fun CategoryRow(
    category: StorageCategory,
    onClick: () -> Unit
) {
    val icon = when (category.icon) {
        "image" -> Icons.Default.Image
        "video" -> Icons.Default.Movie
        "audio" -> Icons.Default.MusicNote
        "description" -> Icons.Default.Description
        "android" -> Icons.Default.Android
        "settings" -> Icons.Default.Settings
        "tool" -> Icons.Default.Build
        "folder_zip" -> Icons.Default.Archive
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

    val color = when (category.icon) {
        "image" -> Color(0xFF4285F4)
        "video" -> Color(0xFFEA4335)
        "audio" -> Color(0xFFFBBC05)
        "description" -> Color(0xFF34A853)
        "android" -> Color(0xFF9C27B0)
        "settings" -> Color(0xFF607D8B)
        "tool" -> Color(0xFF009688)
        "folder_zip" -> Color(0xFFFF8F00)
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, modifier = Modifier.size(20.dp), tint = color)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(category.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            val catLower = category.name.lowercase(Locale.ROOT)
            val subtitle = when {
                catLower.contains("app") || catLower.contains("tool") -> "${category.fileCount} installed apps"
                catLower.contains("system") -> "Android OS & System Firmware"
                else -> "${category.fileCount} files"
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(category.sizeFormatted, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
fun AppStorageItemRow(
    item: AppStorageItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            AsyncAppIcon(item.appInfo.packageName, Modifier.size(46.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.appInfo.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.appInfo.isSystemApp) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "System",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${item.appInfo.packageName} • v${item.appInfo.version}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                val codeStr = StorageScanner.formatSize(item.codeBytes)
                val dataStr = StorageScanner.formatSize(item.dataBytes + item.cacheBytes)
                Text(
                    text = "App Code: $codeStr  |  Data & Cache: $dataStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.totalSizeFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Manage",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AnalyzerFileItem(
    file: File,
    size: String,
    onOpenFile: (FileEntry) -> Unit,
    onMoreOptions: () -> Unit
) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val entry = remember(file, locale) { file.toFileEntry(locale) }
    var apkIcon by remember(file.absolutePath) { mutableStateOf<Drawable?>(null) }
    var apkLabel by remember(file.absolutePath) { mutableStateOf<String?>(null) }
    var thumbnail by remember(file.absolutePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(file.absolutePath) {
        if (entry.isApk) {
            apkIcon = AppManager.getApkIcon(context, file.absolutePath)
            try {
                val pm = context.packageManager
                val info = pm.getPackageArchiveInfo(file.absolutePath, 0)
                val appInfo = info?.applicationInfo
                if (appInfo != null) {
                    appInfo.sourceDir = file.absolutePath
                    appInfo.publicSourceDir = file.absolutePath
                    val label = appInfo.loadLabel(pm).toString()
                    if (label.isNotBlank() && label != file.name) {
                        apkLabel = "$label (${file.name})"
                    }
                }
            } catch (_: Exception) {}
        } else if (ThumbnailManager.isThumbnailSupported(entry)) {
            thumbnail = ThumbnailManager.loadThumbnail(
                context,
                entry,
                EngineType.STANDARD
            )
        }
    }

    val apkBitmap = remember(apkIcon) {
        apkIcon?.let {
            try {
                it.toBitmap().asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    val displayTitle = apkLabel ?: file.name

    ModernListItem(
        title = displayTitle,
        subtitle = size,
        onClick = { onOpenFile(entry) },
        leadingIcon = {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (apkBitmap != null) {
                        Image(
                            bitmap = apkBitmap,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    } else if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val icon = when {
                            entry.isImage -> Icons.Default.Image
                            entry.isVideo -> Icons.Default.Movie
                            entry.isAudio -> Icons.Default.MusicNote
                            entry.isDocument -> Icons.Default.Description
                            else -> Icons.AutoMirrored.Filled.InsertDriveFile
                        }
                        Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onMoreOptions) {
                Icon(Icons.Default.MoreVert, contentDescription = "More Options")
            }
        }
    )
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
