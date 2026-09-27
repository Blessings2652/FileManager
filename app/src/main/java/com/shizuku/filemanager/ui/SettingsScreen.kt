package com.shizuku.filemanager.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.shizuku.filemanager.fs.RootManager
import com.shizuku.filemanager.fs.engine.EnginePrefs
import com.shizuku.filemanager.fs.engine.EngineType
import com.shizuku.filemanager.sys.ThumbnailManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onSwitchEngine: (EngineType?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var highPriority by remember { mutableStateOf(EnginePrefs.isHighPriority(context)) }
    var calcFolderSize by remember { mutableStateOf(EnginePrefs.isCalculateFolderSize(context)) }
    var defaultFormat by remember { mutableStateOf(EnginePrefs.getDefaultArchiveFormat(context)) }
    var compressionLevel by remember { mutableStateOf(EnginePrefs.getCompressionLevel(context)) }
    var autoEmptyTrash by remember { mutableStateOf(EnginePrefs.isAutoEmptyTrash(context)) }
    var trashDays by remember { mutableIntStateOf(EnginePrefs.getTrashRetentionDays(context)) }
    var trashLimit by remember { mutableStateOf(EnginePrefs.getTrashSizeLimit(context)) }
    var refreshRate by remember { mutableLongStateOf(EnginePrefs.getRefreshRate(context)) }
    var itemSizeMultiplier by remember { mutableFloatStateOf(EnginePrefs.getItemSizeMultiplier(context)) }
    var fontSizeMultiplier by remember { mutableFloatStateOf(EnginePrefs.getFontSizeMultiplier(context)) }

    var showHidden by remember { mutableStateOf(EnginePrefs.isShowHidden(context)) }
    var showExtensions by remember { mutableStateOf(EnginePrefs.isShowExtensions(context)) }
    var singleClickOpen by remember { mutableStateOf(EnginePrefs.isSingleClickToOpen(context)) }
    var useInternalViewer by remember { mutableStateOf(EnginePrefs.isUseInternalViewer(context)) }

    var viewMode by remember { mutableStateOf(EnginePrefs.getViewMode(context)) }
    var themeMode by remember { mutableStateOf(EnginePrefs.getThemeMode(context)) }
    var thumbnailsEnabled by remember { mutableStateOf(EnginePrefs.isThumbnailsEnabled(context)) }
    var showTypeIcon by remember { mutableStateOf(EnginePrefs.isShowTypeIconOnThumbnails(context)) }
    var showRecent by remember { mutableStateOf(EnginePrefs.isShowRecentFiles(context)) }
    var appLockEnabled by remember { mutableStateOf(EnginePrefs.isAppLockEnabled(context)) }
    var vaultEnabled by remember { mutableStateOf(EnginePrefs.isVaultEnabled(context)) }
    var currentEngine by remember { mutableStateOf(EnginePrefs.getSavedEngine(context)) }

    var showFormatDialog by remember { mutableStateOf(false) }
    var showLevelDialog by remember { mutableStateOf(false) }
    var showTrashDaysDialog by remember { mutableStateOf(false) }
    var showTrashLimitDialog by remember { mutableStateOf(false) }
    var showViewModeDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showRefreshRateDialog by remember { mutableStateOf(false) }
    var showItemSizeDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }

    ScreenScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ModernTopBar(
                title = "Settings",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader("Privacy & Security", icon = Icons.Default.Security)
            SettingsClickItem(
                title = "Safe Folder / Vault",
                value = if (vaultEnabled) "Encrypted Vault Active" else "Setup encrypted storage",
                onClick = { 
                    vaultEnabled = !vaultEnabled
                    EnginePrefs.setVaultEnabled(context, vaultEnabled)
                    scope.launch {
                        snackbarHostState.showSnackbar(if (vaultEnabled) "Vault enabled" else "Vault disabled")
                    }
                }
            )

            SettingsSwitchItem(
                title = "App Lock (Biometrics)",
                description = "Secure the app with fingerprint or face unlock.",
                checked = appLockEnabled,
                onCheckedChange = {
                    appLockEnabled = it
                    EnginePrefs.setAppLockEnabled(context, it)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader("Trash Management", icon = Icons.Default.DeleteSweep)
            SettingsSwitchItem(
                title = "Auto-Empty Trash",
                description = "Automatically delete items after a set period.",
                checked = autoEmptyTrash,
                onCheckedChange = {
                    autoEmptyTrash = it
                    EnginePrefs.setAutoEmptyTrash(context, it)
                }
            )

            SettingsClickItem(
                title = "Retention Period",
                value = "$trashDays days",
                onClick = { showTrashDaysDialog = true }
            )

            SettingsClickItem(
                title = "Size Limit",
                value = "$trashLimit% of disk space",
                onClick = { showTrashLimitDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader("Interface & Behavior", icon = Icons.Default.DisplaySettings)
            SettingsSwitchItem(
                title = "Show Hidden Items",
                description = "Display files and folders starting with a dot.",
                checked = showHidden,
                onCheckedChange = {
                    showHidden = it
                    EnginePrefs.setShowHidden(context, it)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader("Display & Themes", icon = Icons.Default.ColorLens)

            SettingsClickItem(
                title = "Theme",
                value = themeMode,
                onClick = { showThemeDialog = true }
            )

            SettingsClickItem(
                title = "View Mode",
                value = viewMode,
                onClick = { showViewModeDialog = true }
            )

            SettingsClickItem(
                title = "Item Size",
                value = "${(itemSizeMultiplier * 100).toInt()}%",
                onClick = { showItemSizeDialog = true }
            )

            SettingsClickItem(
                title = "Font Size",
                value = "${(fontSizeMultiplier * 100).toInt()}%",
                onClick = { showFontSizeDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader("Thumbnails", icon = Icons.Default.Archive)
            SettingsClickItem(
                title = "Clear Thumbnail Cache",
                value = "Delete all cached previews",
                onClick = { 
                    scope.launch {
                        val cleared = ThumbnailManager.clearCache(context)
                        val formatted = formatSize(cleared)
                        snackbarHostState.showSnackbar("Thumbnail cache cleared ($formatted)")
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader("Sidebar Customization", icon = Icons.AutoMirrored.Default.ViewSidebar)
            SettingsSwitchItem(
                title = "Show Recent Files",
                description = "Display the 'Recent' section in the sidebar.",
                checked = showRecent,
                onCheckedChange = {
                    showRecent = it
                    EnginePrefs.setShowRecentFiles(context, it)
                }
            )
        }
    }

    if (showFormatDialog) {
        val formats = listOf("ZIP", "7z", "TAR")
        SingleSelectDialog(
            title = "Default Format",
            options = formats,
            selectedOption = defaultFormat,
            onDismiss = { showFormatDialog = false },
            onSelect = {
                defaultFormat = it
                EnginePrefs.setDefaultArchiveFormat(context, it)
                showFormatDialog = false
            }
        )
    }

    if (showLevelDialog) {
        val levels = listOf("Fast", "Maximum")
        SingleSelectDialog(
            title = "Compression Level",
            options = levels,
            selectedOption = compressionLevel,
            onDismiss = { showLevelDialog = false },
            onSelect = {
                compressionLevel = it
                EnginePrefs.setCompressionLevel(context, it)
                showLevelDialog = false
            }
        )
    }

    if (showTrashDaysDialog) {
        val daysOptions = listOf("7", "14", "30", "60", "90")
        SingleSelectDialog(
            title = "Retention Period",
            options = daysOptions,
            selectedOption = trashDays.toString(),
            onDismiss = { showTrashDaysDialog = false },
            onSelect = {
                val days = it.toInt()
                trashDays = days
                EnginePrefs.setTrashRetentionDays(context, days)
                showTrashDaysDialog = false
            }
        )
    }

    if (showTrashLimitDialog) {
        val limitOptions = listOf("1", "2", "5", "10", "20")
        SingleSelectDialog(
            title = "Size Limit",
            options = limitOptions,
            selectedOption = trashLimit.toString(),
            onDismiss = { showTrashLimitDialog = false },
            onSelect = {
                val limit = it.toInt()
                trashLimit = limit
                EnginePrefs.setTrashSizeLimit(context, limit)
                showTrashLimitDialog = false
            }
        )
    }

    if (showViewModeDialog) {
        val viewModes = listOf("List", "Grid", "Small Icons", "Columns")
        SingleSelectDialog(
            title = "View Mode",
            options = viewModes,
            selectedOption = viewMode,
            onDismiss = { showViewModeDialog = false },
            onSelect = {
                viewMode = it
                EnginePrefs.setViewMode(context, it)
                showViewModeDialog = false
            }
        )
    }

    if (showThemeDialog) {
        val themes = listOf("Light", "Dark", "Super Black", "System")
        SingleSelectDialog(
            title = "Theme",
            options = themes,
            selectedOption = themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = {
                themeMode = it
                EnginePrefs.setThemeMode(context, it)
                showThemeDialog = false
            }
        )
    }

    if (showRefreshRateDialog) {
        val rates = listOf("100", "200", "500", "1000", "2000", "5000")
        SingleSelectDialog(
            title = "Auto-Refresh Rate",
            options = rates,
            selectedOption = refreshRate.toString(),
            onDismiss = { showRefreshRateDialog = false },
            onSelect = {
                val rate = it.toLong()
                refreshRate = rate
                EnginePrefs.setRefreshRate(context, rate)
                showRefreshRateDialog = false
            }
        )
    }

    if (showItemSizeDialog) {
        val multipliers = listOf("80%", "90%", "100%", "110%", "125%", "150%")
        SingleSelectDialog(
            title = "Item Size",
            options = multipliers,
            selectedOption = "${(itemSizeMultiplier * 100).toInt()}%",
            onDismiss = { showItemSizeDialog = false },
            onSelect = {
                val mult = it.removeSuffix("%").toFloat() / 100f
                itemSizeMultiplier = mult
                EnginePrefs.setItemSizeMultiplier(context, mult)
                showItemSizeDialog = false
            }
        )
    }

    if (showFontSizeDialog) {
        val multipliers = listOf("80%", "90%", "100%", "110%", "125%", "150%")
        SingleSelectDialog(
            title = "Font Size",
            options = multipliers,
            selectedOption = "${(fontSizeMultiplier * 100).toInt()}%",
            onDismiss = { showFontSizeDialog = false },
            onSelect = {
                val mult = it.removeSuffix("%").toFloat() / 100f
                fontSizeMultiplier = mult
                EnginePrefs.setFontSizeMultiplier(context, mult)
                showFontSizeDialog = false
            }
        )
    }
}

@Composable
fun SettingsClickItem(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(value) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

@Composable
fun SingleSelectDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = null,
                            modifier = Modifier.scale(0.8f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(option)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.scale(0.8f)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
    )
}

@Composable
fun SettingsRadioButtonItem(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.scale(0.8f)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}
