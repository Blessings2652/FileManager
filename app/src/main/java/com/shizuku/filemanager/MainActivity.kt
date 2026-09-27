package com.shizuku.filemanager

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import android.os.Bundle
import android.provider.OpenableColumns
import java.io.File
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import com.shizuku.filemanager.sys.FileIntentUtils
import com.shizuku.filemanager.fs.BookmarksManager
import com.shizuku.filemanager.fs.FileEntry

import com.shizuku.filemanager.fs.RootManager
import com.shizuku.filemanager.fs.engine.EnginePrefs
import com.shizuku.filemanager.fs.engine.EngineType
import com.shizuku.filemanager.fs.engine.FileEngine
import com.shizuku.filemanager.fs.engine.RootFileEngine
import com.shizuku.filemanager.fs.engine.SafFileEngine
import com.shizuku.filemanager.fs.engine.ShizukuFileEngine
import com.shizuku.filemanager.fs.engine.StandardFileEngine
import com.shizuku.filemanager.shizuku.ShizukuManager
import com.shizuku.filemanager.sys.AppInfo

import com.shizuku.filemanager.ui.AppDetailsScreen
import com.shizuku.filemanager.ui.AppListScreen
import com.shizuku.filemanager.ui.CodeEditorScreen
import com.shizuku.filemanager.ui.EngineSelectionScreen
import com.shizuku.filemanager.ui.FileBrowserScreen
import com.shizuku.filemanager.ui.FileIntegrityScreen
import com.shizuku.filemanager.ui.FileRepairScreen

import com.shizuku.filemanager.ui.InternalInstallerScreen
import com.shizuku.filemanager.ui.MediaViewerScreen
import com.shizuku.filemanager.ui.MetadataEditorScreen
import com.shizuku.filemanager.ui.ModernDrawerHeader
import com.shizuku.filemanager.ui.PdfViewerScreen
import com.shizuku.filemanager.ui.RootGateScreen
import com.shizuku.filemanager.ui.SafPickerScreen
import com.shizuku.filemanager.ui.SafeFolderScreen
import com.shizuku.filemanager.ui.SettingsScreen
import com.shizuku.filemanager.ui.ShizukuGateScreen
import com.shizuku.filemanager.ui.StandardGateScreen
import com.shizuku.filemanager.ui.StorageAnalyzerScreen
import com.shizuku.filemanager.ui.StorageCleanerScreen
import com.shizuku.filemanager.ui.TimelineScreen
import com.shizuku.filemanager.ui.TrashScreen
import com.shizuku.filemanager.ui.VolumeInspectorScreen
import com.shizuku.filemanager.ui.ZipViewerScreen
import com.shizuku.filemanager.ui.isStandardPermissionGranted
import com.shizuku.filemanager.ui.theme.FileManagerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.shizuku.filemanager.ui.AboutAppScreen

sealed class NavScreen {
    data class Browser(
        val initialPath: String? = null, 
        val lockedRoot: String? = null, 
        val engineOverride: EngineType? = null,
        val isDocumentMode: Boolean = false
    ) : NavScreen()
    object Apps : NavScreen()
    data class AppDetails(val app: AppInfo) : NavScreen()
    object Volumes : NavScreen()
    object Settings : NavScreen()
    object AboutApp : NavScreen()
    object Trash : NavScreen()
    object SafeFolder : NavScreen()
    data class CodeEditor(val entry: FileEntry, val engine: FileEngine? = null) : NavScreen()
    data class JsonViewer(val entry: FileEntry, val engine: FileEngine? = null) : NavScreen()
    data class MediaViewer(val entry: FileEntry, val engineType: EngineType? = null) : NavScreen()
    data class ZipViewer(val path: String) : NavScreen()
    data class PdfViewer(val entry: FileEntry, val engineType: EngineType? = null) : NavScreen()

    data class InstallApk(val apkPath: String) : NavScreen()
    object StorageCleaner : NavScreen()
    object StorageAnalyzer : NavScreen()
    data class MetadataEditor(val filePath: String) : NavScreen()
    data class FileIntegrity(val filePath: String) : NavScreen()
    object Timeline : NavScreen()

    data class FileRepair(val filePath: String) : NavScreen()
    object Onboarding : NavScreen()
    object Bookmarks : NavScreen()
    data class BatchRename(val entries: List<FileEntry>, val engine: FileEngine) : NavScreen()
    data class GlobalSearch(val startPath: String, val engine: FileEngine) : NavScreen()
    object PartitionManager : NavScreen()
}

class MainActivity : FragmentActivity() {
    private val incomingIntentState = MutableStateFlow<Intent?>(null)

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingIntentState.value = intent
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var themeMode by remember { mutableStateOf(EnginePrefs.getThemeMode(context)) }
            var accentColorArgb by remember { mutableStateOf(EnginePrefs.getAccentColor(context)) }

            LaunchedEffect(Unit) {
                EnginePrefs.settingsChanged.collect {
                    themeMode = EnginePrefs.getThemeMode(context)
                    accentColorArgb = EnginePrefs.getAccentColor(context)
                }
            }

            val darkTheme = when (themeMode) {
                "Dark", "Super Black" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }
            val superBlack = themeMode == "Super Black"

            SideEffect {
                val windowController = WindowCompat.getInsetsController(window, window.decorView)
                windowController.isAppearanceLightStatusBars = !darkTheme
            }

            FileManagerTheme(darkTheme = darkTheme, superBlack = superBlack, accentColorArgb = accentColorArgb) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(incomingIntentState = incomingIntentState)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingIntentState.value = intent
    }
}

@UnstableApi
@Composable
private fun AppRoot(incomingIntentState: kotlinx.coroutines.flow.StateFlow<Intent?>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locale = LocalLocale.current.platformLocale

    var chosenEngine by remember { mutableStateOf<EngineType?>(EnginePrefs.getSavedEngine(context) ?: EnginePrefs.detectBestEngine(context)) }
    var activeEngine by remember { mutableStateOf<FileEngine?>(null) }
    var showExitConfirmation by remember { mutableStateOf(value = false) }

    LaunchedEffect(Unit) {
        if (EnginePrefs.getSavedEngine(context) == null) {
            chosenEngine?.let { EnginePrefs.saveEngine(context, it) }
        }
    }
    
    val initialScreen = remember(chosenEngine) {
        if (chosenEngine == EngineType.ROOT) {
            NavScreen.Browser(initialPath = android.os.Environment.getExternalStorageDirectory().absolutePath)
        } else {
            NavScreen.Browser()
        }
    }
    var currentScreen by remember(initialScreen) { mutableStateOf<NavScreen>(initialScreen) }

    // Autoload SAF engine if selected
    LaunchedEffect(chosenEngine) {
        if (chosenEngine == EngineType.SAF && activeEngine == null) {
            val savedUri = EnginePrefs.getSafTreeUri(context)
            if (savedUri != null) {
                try {
                    activeEngine = SafFileEngine(context.applicationContext, savedUri.toUri())
                } catch (_: Throwable) {
                    EnginePrefs.clearSafTreeUri(context)
                }
            }
        }
        
        // Start watcher for downloads
        val intent = Intent(context, com.shizuku.filemanager.sys.FolderWatcherService::class.java).apply {
            putExtra("path", android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).absolutePath)
        }
        context.startForegroundService(intent)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)



    val database = remember { com.shizuku.filemanager.db.TagDatabase.getDatabase(context.applicationContext) }

    LaunchedEffect(Unit) {
        com.shizuku.filemanager.fs.FileClipboard.restore(context)
        com.shizuku.filemanager.fs.BookmarksManager.refresh(context)
    }

    var isFirstLaunch by remember { mutableStateOf(EnginePrefs.isFirstLaunch(context)) }

    if (isFirstLaunch) {
        com.shizuku.filemanager.ui.OnboardingScreen(onFinished = {
            EnginePrefs.setFirstLaunchCompleted(context)
            isFirstLaunch = false
        })
        return
    }

    val onOpenFile: (FileEntry, FileEngine) -> Unit = { entry, engine ->
        if (entry.permissions == "INTEGRITY") {
            currentScreen = NavScreen.FileIntegrity(entry.path)
        } else if (entry.permissions == "METADATA") {
            currentScreen = NavScreen.MetadataEditor(entry.path)
        } else if (entry.isJson) {
            currentScreen = NavScreen.JsonViewer(entry, engine)
        } else if (entry.permissions == "TEXT_EDIT" || entry.isText) {
            currentScreen = NavScreen.CodeEditor(entry, engine)
        } else if (entry.isPdf) {
            currentScreen = NavScreen.PdfViewer(entry, engine.type)
        } else if (entry.isImage || entry.isVideo || entry.isAudio) {
            currentScreen = NavScreen.MediaViewer(entry, engine.type)
        } else if (entry.permissions == "VIEW_ZIP") {
            currentScreen = NavScreen.ZipViewer(entry.path)
        } else if (entry.isArchive) {
            // ZIP files should only be opened via 'View Contents' or by extracting them.
            // If they try to 'Open' it without the explicit flag, we do nothing or could show a message.
            Toast.makeText(context, "Archive must be extracted or viewed via 'View Contents'", Toast.LENGTH_SHORT).show()
        } else if (entry.isApk) {
            val limit50Mb = 50L * 1024L * 1024L
            if (entry.sizeBytes > limit50Mb) {
                scope.launch {
                    FileIntentUtils.installApk(context, entry, engine)
                }
            } else {
                scope.launch {
                    if (engine.type == EngineType.STANDARD) {
                        currentScreen = NavScreen.InstallApk(entry.path)
                    } else {
                        // Copy to cache first if using Shizuku/Root/SAF
                        withContext(Dispatchers.IO) {
                            try {
                                val tempFile = File(context.cacheDir, "temp_install.apk")
                                val bytes = engine.readBytes(entry.path).getOrThrow()
                                tempFile.writeBytes(bytes)
                                withContext(Dispatchers.Main) {
                                    currentScreen = NavScreen.InstallApk(tempFile.absolutePath)
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Failed to prepare APK: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Fallback: Open with external app
            scope.launch {
                com.shizuku.filemanager.sys.FileIntentUtils.openWith(context, entry, engine)
            }
        }
    }



    BackHandler(enabled = true) {
        if (showExitConfirmation) {
            showExitConfirmation = false
        } else if (currentScreen !is NavScreen.Browser || (currentScreen as NavScreen.Browser).initialPath != null) {
            currentScreen = NavScreen.Browser()
        } else {
            showExitConfirmation = true
        }
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Exit App") },
            text = { Text("Are you sure you want to exit the app?") },
            confirmButton = {
                TextButton(onClick = { (context as? android.app.Activity)?.finish() }) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val incomingIntent by incomingIntentState.collectAsState()

    // Handle incoming intents (e.g., Open with..., Share with profile)
    LaunchedEffect(incomingIntent) {
        val intent = incomingIntent ?: return@LaunchedEffect
        val action = intent.action ?: return@LaunchedEffect

        if (action == Intent.ACTION_VIEW || action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE || action == "android.intent.action.INSTALL_PACKAGE") {
            scope.launch(Dispatchers.IO) {
                try {
                    val sharedDir = File(context.cacheDir, "shared_files").apply { if (!exists()) mkdirs() }

                    if (action == Intent.ACTION_SEND_MULTIPLE) {
                        val uris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                        if (!uris.isNullOrEmpty()) {
                            var count = 0
                            uris.forEach { uri ->
                                if (copyUriToDirectory(context, uri, sharedDir) != null) count++
                            }
                            if (count > 0) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "$count shared file(s) saved to Shared Files", Toast.LENGTH_SHORT).show()
                                    currentScreen = NavScreen.Browser(initialPath = sharedDir.absolutePath)
                                }
                            }
                        }
                    } else {
                        val uri: Uri? = if (action == Intent.ACTION_SEND) {
                            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: intent.data
                        } else {
                            intent.data ?: IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                        }

                        if (uri != null) {
                            val destFile = copyUriToDirectory(context, uri, sharedDir)
                            if (destFile != null) {
                                withContext(Dispatchers.Main) {
                                    val entry = destFile.toFileEntry(locale)
                                    val limit50Mb = 50L * 1024L * 1024L
                                    if (entry.isApk || action == "android.intent.action.INSTALL_PACKAGE") {
                                        if (destFile.length() > limit50Mb) {
                                            scope.launch {
                                                val activeEng = activeEngine ?: StandardFileEngine(context.applicationContext)
                                                FileIntentUtils.installApk(context, entry, activeEng)
                                            }
                                        } else {
                                            currentScreen = NavScreen.InstallApk(destFile.absolutePath)
                                        }
                                    } else if (entry.isPdf) {
                                        currentScreen = NavScreen.PdfViewer(entry, activeEngine?.type)
                                    } else if (entry.isImage || entry.isVideo || entry.isAudio) {
                                        currentScreen = NavScreen.MediaViewer(entry, activeEngine?.type)
                                    } else if (entry.isJson) {
                                        currentScreen = NavScreen.JsonViewer(entry, activeEngine)
                                    } else if (entry.isText || entry.isKotlin) {
                                        currentScreen = NavScreen.CodeEditor(entry, activeEngine)
                                    } else if (entry.isArchive) {
                                        currentScreen = NavScreen.ZipViewer(destFile.absolutePath)
                                    } else {
                                        Toast.makeText(context, "Shared file saved: ${destFile.name}", Toast.LENGTH_SHORT).show()
                                        currentScreen = NavScreen.Browser(initialPath = sharedDir.absolutePath)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val safTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            EnginePrefs.saveSafTreeUri(context, uri.toString())
            activeEngine = SafFileEngine(context.applicationContext, uri)
        }
    }

    fun switchEngine(newType: EngineType? = null, navigateHome: Boolean = true) {
        if (newType == null) {
            EnginePrefs.clear(context)
        } else {
            EnginePrefs.saveEngine(context, newType)
        }
        chosenEngine = newType
        activeEngine = null
        if (navigateHome) {
            currentScreen = if (newType == EngineType.ROOT) {
                NavScreen.Browser(initialPath = android.os.Environment.getExternalStorageDirectory().absolutePath)
            } else {
                NavScreen.Browser()
            }
        }
    }

    if (chosenEngine == null) {
        EngineSelectionScreen(onSelect = { picked ->
            EnginePrefs.saveEngine(context, picked)
            chosenEngine = picked
        })
        return
    }

    LaunchedEffect(currentScreen) {
        if (currentScreen !is NavScreen.Browser) {
            drawerState.snapTo(DrawerValue.Closed)
        } else if (drawerState.isOpen) {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentScreen is NavScreen.Browser,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.widthIn(max = 320.dp),
                drawerShape = RectangleShape,
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                ModernDrawerHeader(appName = stringResource(R.string.app_name))
                
                Spacer(Modifier.height(12.dp))
                val internalPath = android.os.Environment.getExternalStorageDirectory().absolutePath
                
                val drawerItemModifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                
                val drawerItemColors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (chosenEngine == EngineType.ROOT) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Storage, null) },
                            label = { Text("Internal Storage", fontWeight = FontWeight.Bold) },
                            selected = currentScreen is NavScreen.Browser && (currentScreen as NavScreen.Browser).initialPath == internalPath,
                            onClick = {
                                currentScreen = NavScreen.Browser(initialPath = internalPath)
                                scope.launch { drawerState.close() }
                            },
                            modifier = drawerItemModifier,
                            colors = drawerItemColors,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Apps, null) },
                        label = { Text("Apps", fontWeight = FontWeight.Bold) },
                        selected = currentScreen is NavScreen.Apps,
                        onClick = { 
                            currentScreen = NavScreen.Apps
                            scope.launch { drawerState.close() }
                        },
                        modifier = drawerItemModifier,
                        colors = drawerItemColors,
                        shape = RoundedCornerShape(16.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AutoFixHigh, null) },
                        label = { Text("Storage Cleaner", fontWeight = FontWeight.Bold) },
                        selected = currentScreen is NavScreen.StorageCleaner,
                        onClick = { 
                            currentScreen = NavScreen.StorageCleaner
                            scope.launch { drawerState.close() }
                        },
                        modifier = drawerItemModifier,
                        colors = drawerItemColors,
                        shape = RoundedCornerShape(16.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Storage, null) },
                        label = { Text("Storage Analyzer", fontWeight = FontWeight.Bold) },
                        selected = currentScreen is NavScreen.StorageAnalyzer,
                        onClick = { 
                            currentScreen = NavScreen.StorageAnalyzer
                            scope.launch { drawerState.close() }
                        },
                        modifier = drawerItemModifier,
                        colors = drawerItemColors,
                        shape = RoundedCornerShape(16.dp)
                    )
                    /*
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.InstallMobile, null) },
                        label = { Text("Install APK", fontWeight = FontWeight.Bold) },
                        selected = currentScreen is NavScreen.InstallApk,
                        onClick = { 
                            currentScreen = NavScreen.InstallApk("")
                            scope.launch { drawerState.close() }
                        },
                        modifier = drawerItemModifier,
                        colors = drawerItemColors,
                        shape = RoundedCornerShape(16.dp)
                    )
                    */
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 28.dp))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Delete, null) },
                        label = { Text("Trash Bin", fontWeight = FontWeight.Bold) },
                        selected = currentScreen is NavScreen.Trash,
                        onClick = { 
                            currentScreen = NavScreen.Trash
                            scope.launch { drawerState.close() }
                        },
                        modifier = drawerItemModifier,
                        colors = drawerItemColors,
                        shape = RoundedCornerShape(16.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Lock, null) },
                        label = { Text("Safe Folder", fontWeight = FontWeight.Bold) },
                        selected = currentScreen is NavScreen.SafeFolder,
                        onClick = { 
                            currentScreen = NavScreen.SafeFolder
                            scope.launch { drawerState.close() }
                        },
                        modifier = drawerItemModifier,
                        colors = drawerItemColors,
                        shape = RoundedCornerShape(16.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.History, null) },
                        label = { Text("Timeline", fontWeight = FontWeight.Bold) },
                        selected = currentScreen is NavScreen.Timeline,
                        onClick = { 
                            currentScreen = NavScreen.Timeline
                            scope.launch { drawerState.close() }
                        },
                        modifier = drawerItemModifier,
                        colors = drawerItemColors,
                        shape = RoundedCornerShape(16.dp)
                    )

                    if (activeEngine != null) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Search, null) },
                            label = { Text("Search everywhere", fontWeight = FontWeight.Bold) },
                            selected = currentScreen is NavScreen.GlobalSearch,
                            onClick = {
                                currentScreen = NavScreen.GlobalSearch(startPath = activeEngine!!.rootPath, engine = activeEngine!!)
                                scope.launch { drawerState.close() }
                            },
                            modifier = drawerItemModifier,
                            colors = drawerItemColors,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Star, null) },
                        label = { Text("Bookmarks", fontWeight = FontWeight.Bold) },
                        selected = currentScreen is NavScreen.Bookmarks,
                        onClick = {
                            currentScreen = NavScreen.Bookmarks
                            scope.launch { drawerState.close() }
                        },
                        modifier = drawerItemModifier,
                        colors = drawerItemColors,
                        shape = RoundedCornerShape(16.dp)
                    )
                    val bookmarks by BookmarksManager.bookmarks.collectAsState()
                    LaunchedEffect(Unit) {
                        BookmarksManager.refresh(context)
                    }
                    if (bookmarks.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 28.dp))
                        bookmarks.forEach { bookmark ->
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary) },
                                label = { Text(bookmark.label, fontWeight = FontWeight.SemiBold) },
                                selected = currentScreen is NavScreen.Browser && (currentScreen as NavScreen.Browser).initialPath == bookmark.path,
                                onClick = {
                                    currentScreen = NavScreen.Browser(initialPath = bookmark.path, engineOverride = EngineType.valueOf(bookmark.engineType))
                                    scope.launch { drawerState.close() }
                                },
                                modifier = drawerItemModifier,
                                colors = drawerItemColors,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                    if (chosenEngine == EngineType.ROOT) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Storage, null) },
                            label = { Text("Partitions & Mounts", fontWeight = FontWeight.Bold) },
                            selected = currentScreen is NavScreen.PartitionManager,
                            onClick = {
                                currentScreen = NavScreen.PartitionManager
                                scope.launch { drawerState.close() }
                            },
                            modifier = drawerItemModifier,
                            colors = drawerItemColors,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("Settings", fontWeight = FontWeight.Bold) },
                        selected = currentScreen is NavScreen.Settings,
                        onClick = { 
                            currentScreen = NavScreen.Settings
                            scope.launch { drawerState.close() }
                        },
                        modifier = drawerItemModifier,
                        colors = drawerItemColors,
                        shape = RoundedCornerShape(16.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Info, null) },
                        label = { Text("About App", fontWeight = FontWeight.Bold) },
                        selected = currentScreen is NavScreen.AboutApp,
                        onClick = { 
                            currentScreen = NavScreen.AboutApp
                            scope.launch { drawerState.close() }
                        },
                        modifier = drawerItemModifier,
                        colors = drawerItemColors,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }
    )
{
        val onOpenDrawer = { 
            scope.launch { drawerState.open() }
            Unit 
        }


        when (val screen = currentScreen) {
            is NavScreen.Browser -> {
                BrowserContent(
                    engineType = screen.engineOverride ?: chosenEngine!!,
                    activeEngine = activeEngine,
                    onOpenDrawer = onOpenDrawer,
                    onSwitchEngine = ::switchEngine,
                    initialPath = screen.initialPath,
                    lockedRoot = screen.lockedRoot,
                    isDocumentMode = screen.isDocumentMode,
                    onSafOpen = { safTreeLauncher.launch(null) },
                    onOpenFile = onOpenFile,
                    database = database
                )
            }
            NavScreen.Apps -> AppListScreen(
                onBack = { currentScreen = NavScreen.Browser() },
                onAppDetails = { app ->
                    currentScreen = NavScreen.AppDetails(app)
                }
            )
            is NavScreen.AppDetails -> AppDetailsScreen(
                app = screen.app,
                onBack = { currentScreen = NavScreen.Apps }
            )
            NavScreen.Volumes -> VolumeInspectorScreen(
                onBack = { currentScreen = NavScreen.Browser() }
            )
            NavScreen.Settings -> SettingsScreen(
                onBack = { currentScreen = NavScreen.Browser() },
                onSwitchEngine = { type -> switchEngine(type, navigateHome = false) }
            )
            NavScreen.SafeFolder -> SafeFolderScreen(
                engineType = chosenEngine!!,
                activeEngine = activeEngine,
                onBack = { currentScreen = NavScreen.Browser() },
                onSafOpen = { safTreeLauncher.launch(null) },
                onSwitchEngine = ::switchEngine,
                onOpenFile = onOpenFile
            )
            NavScreen.Trash -> TrashContent(
                engineType = chosenEngine!!,
                activeEngine = activeEngine,
                onBack = { currentScreen = NavScreen.Browser() },
                onSafOpen = { safTreeLauncher.launch(null) },
                onSwitchEngine = ::switchEngine,
                onOpenFile = onOpenFile
            )
            is NavScreen.CodeEditor -> {
                val engine = screen.engine ?: activeEngine ?: StandardFileEngine(context.applicationContext)
                CodeEditorScreen(
                    entry = screen.entry,
                    engine = engine,
                    onBack = { currentScreen = NavScreen.Browser() }
                )
            }
            is NavScreen.JsonViewer -> {
                val engine = screen.engine ?: activeEngine ?: StandardFileEngine(context.applicationContext)
                com.shizuku.filemanager.ui.JsonViewerScreen(
                    entry = screen.entry,
                    engine = engine,
                    onBack = { currentScreen = NavScreen.Browser() }
                )
            }
            is NavScreen.MediaViewer -> {
                val engine = activeEngine ?: StandardFileEngine(context.applicationContext)
                MediaViewerScreen(
                    entry = screen.entry,
                    engine = engine,
                    onBack = { currentScreen = NavScreen.Browser() },
                    onEdit = {
                        scope.launch {
                            FileIntentUtils.editWith(context, screen.entry, engine)
                        }
                    }
                )
            }
            is NavScreen.ZipViewer -> ZipViewerScreen(
                zipPath = screen.path,
                onBack = { currentScreen = NavScreen.Browser() },
                onOpenFile = { file ->
                    val entry = FileEntry(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = false,
                        isSymlink = false,
                        sizeBytes = file.length(),
                        permissions = "r",
                        owner = "",
                        group = "",
                        modified = ""
                    )
                    onOpenFile(entry, StandardFileEngine(context.applicationContext))
                }
            )
            is NavScreen.PdfViewer -> {
                val engine = activeEngine ?: StandardFileEngine(context.applicationContext)
                PdfViewerScreen(
                    entry = screen.entry,
                    engine = engine,
                    onBack = { currentScreen = NavScreen.Browser() }
                )
            }

            is NavScreen.InstallApk -> InternalInstallerScreen(
                apkPath = screen.apkPath,
                onBack = { currentScreen = NavScreen.Browser() }
            )
            NavScreen.StorageCleaner -> StorageCleanerScreen(
                onBack = { currentScreen = NavScreen.Browser() },
                onOpenFile = { entry -> onOpenFile(entry, activeEngine ?: StandardFileEngine(context.applicationContext)) }
            )
            NavScreen.StorageAnalyzer -> StorageAnalyzerScreen(
                onBack = { currentScreen = NavScreen.Browser() },
                onOpenFile = { entry -> onOpenFile(entry, activeEngine ?: StandardFileEngine(context.applicationContext)) },
                onNavigate = { screen -> currentScreen = screen }
            )
            is NavScreen.MetadataEditor -> MetadataEditorScreen(
                filePath = screen.filePath,
                onBack = { currentScreen = NavScreen.Browser() }
            )
            is NavScreen.FileIntegrity -> FileIntegrityScreen(
                filePath = screen.filePath,
                onBack = { currentScreen = NavScreen.Browser() }
            )
            NavScreen.Timeline -> TimelineScreen(
                onBack = { currentScreen = NavScreen.Browser() }
            )

            is NavScreen.FileRepair -> {
                val file = File(screen.filePath)
                FileRepairScreen(
                    entry = file.toFileEntry(locale),
                    onBack = { currentScreen = NavScreen.Browser() }
                )
            }
            NavScreen.Onboarding -> {
                com.shizuku.filemanager.ui.OnboardingScreen(onFinished = {
                    EnginePrefs.setFirstLaunchCompleted(context)
                    isFirstLaunch = false
                })
            }
            NavScreen.Bookmarks -> com.shizuku.filemanager.ui.BookmarksScreen(
                onBack = { currentScreen = NavScreen.Browser() },
                onOpenBookmark = { path, engineType ->
                    currentScreen = NavScreen.Browser(initialPath = path, engineOverride = engineType)
                }
            )
            is NavScreen.BatchRename -> com.shizuku.filemanager.ui.BatchRenameScreen(
                entries = screen.entries,
                engine = screen.engine,
                onBack = { currentScreen = NavScreen.Browser() },
                onConfirm = { plans ->
                    withContext(Dispatchers.IO) {
                        for (plan in plans) {
                            screen.engine.rename(plan.original, plan.newName)
                        }
                    }
                    currentScreen = NavScreen.Browser()
                }
            )
            is NavScreen.GlobalSearch -> com.shizuku.filemanager.ui.GlobalSearchScreen(
                engine = screen.engine,
                startPath = screen.startPath,
                onBack = { currentScreen = NavScreen.Browser() },
                onOpenResult = { entry, parentPath ->
                    if (entry.isDirectory) {
                        currentScreen = NavScreen.Browser(initialPath = entry.path, engineOverride = screen.engine.type)
                    } else {
                        currentScreen = NavScreen.Browser(initialPath = parentPath, engineOverride = screen.engine.type)
                    }
                }
            )
            NavScreen.PartitionManager -> com.shizuku.filemanager.ui.PartitionManagerScreen(
                onBack = { currentScreen = NavScreen.Browser() }
            )
            NavScreen.AboutApp -> AboutAppScreen(
                onBack = { currentScreen = NavScreen.Browser() }
            )
        }
    }
}

@Composable
private fun TrashContent(
    engineType: EngineType,
    activeEngine: FileEngine?,
    onBack: () -> Unit,
    onSafOpen: () -> Unit,
    onSwitchEngine: (EngineType?) -> Unit,
    onOpenFile: (FileEntry, FileEngine) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val engine = when (engineType) {
        EngineType.SHIZUKU -> remember { ShizukuFileEngine() }
        EngineType.STANDARD -> remember { StandardFileEngine(context.applicationContext) }
        EngineType.ROOT -> remember { RootFileEngine() }
        EngineType.SAF -> activeEngine
    }

    if (engine == null && engineType == EngineType.SAF) {
        SafPickerScreen(onPickFolder = onSafOpen, onPickDifferentEngine = { onSwitchEngine(null) })
        return
    }

    engine?.let {
        TrashScreen(
            engine = it,
            onBack = onBack,
            onOpenFile = onOpenFile
        )
    }
}

@Composable
private fun BrowserContent(
    engineType: EngineType,
    activeEngine: FileEngine?,
    onOpenDrawer: () -> Unit,
    onSwitchEngine: (EngineType?) -> Unit,
    initialPath: String?,
    lockedRoot: String? = null,
    isDocumentMode: Boolean = false,
    onSafOpen: () -> Unit,
    onOpenFile: (FileEntry, FileEngine) -> Unit,
    database: com.shizuku.filemanager.db.TagDatabase? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    when (engineType) {
        EngineType.SHIZUKU -> {
            val isAvailable by ShizukuManager.isAvailable
            val hasPermission by ShizukuManager.hasPermission
            if (isAvailable && hasPermission) {
                FileBrowserScreen(
                    engine = remember { ShizukuFileEngine() },
                    onOpenDrawer = onOpenDrawer,
                    initialPath = initialPath,
                    onOpenFile = onOpenFile,
                    lockedRoot = lockedRoot,
                    isDocumentMode = isDocumentMode,
                    database = database,
                    onSwitchEngine = { onSwitchEngine(it) }
                )
            } else {
                ShizukuGateScreen(
                    isAvailable = isAvailable,
                    hasPermission = hasPermission,
                    onRequestPermission = { ShizukuManager.requestPermission() },
                    onPickDifferentEngine = { onSwitchEngine(null) }
                )
            }
        }

        EngineType.STANDARD -> {
            var hasPermission by remember { mutableStateOf(isStandardPermissionGranted(context)) }
            val lifecycleOwner = LocalLifecycleOwner.current
            
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        hasPermission = isStandardPermissionGranted(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            if (hasPermission) {
                FileBrowserScreen(
                    engine = remember { StandardFileEngine(context.applicationContext) },
                    onOpenDrawer = onOpenDrawer,
                    initialPath = initialPath,
                    onOpenFile = onOpenFile,
                    lockedRoot = lockedRoot,
                    isDocumentMode = isDocumentMode,
                    database = database,
                    onSwitchEngine = { onSwitchEngine(it) }
                )
            } else {
                StandardGateScreen(
                    onPickDifferentEngine = { onSwitchEngine(null) }
                )
            }
        }

        EngineType.ROOT -> {
            val checked by RootManager.checked
            val granted by RootManager.isAvailable
            LaunchedEffect(Unit) {
                if (!checked) RootManager.checkAccess()
            }
            if (checked && granted) {
                FileBrowserScreen(
                    engine = remember { RootFileEngine() },
                    onOpenDrawer = onOpenDrawer,
                    initialPath = initialPath,
                    onOpenFile = onOpenFile,
                    lockedRoot = lockedRoot,
                    isDocumentMode = isDocumentMode,
                    database = database,
                    onSwitchEngine = { onSwitchEngine(it) }
                )
            } else {
                RootGateScreen(
                    checking = !checked,
                    granted = granted,
                    onRetry = { scope.launch { RootManager.checkAccess() } },
                    onPickDifferentEngine = { onSwitchEngine(null) }
                )
            }
        }

        EngineType.SAF -> {
            if (activeEngine != null) {
                FileBrowserScreen(
                    engine = activeEngine,
                    onOpenDrawer = onOpenDrawer,
                    initialPath = initialPath,
                    onOpenFile = onOpenFile,
                    lockedRoot = lockedRoot,
                    isDocumentMode = isDocumentMode,
                    database = database,
                    onSwitchEngine = { onSwitchEngine(it) }
                )
            } else {
                SafPickerScreen(
                    onPickFolder = onSafOpen,
                    onPickDifferentEngine = { onSwitchEngine(null) }
                )
            }
        }
    }
}

private fun copyUriToDirectory(context: Context, uri: Uri, targetDir: File): File? {
    return try {
        var fileName = "shared_${System.currentTimeMillis()}"
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1) {
                        val name = cursor.getString(nameIdx)
                        if (!name.isNullOrEmpty()) fileName = name
                    }
                }
            }
        } else if (uri.scheme == "file") {
            uri.lastPathSegment?.let { if (it.isNotEmpty()) fileName = it }
        }

        val destFile = File(targetDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        destFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun File.toFileEntry(locale: java.util.Locale = java.util.Locale.getDefault()): FileEntry {
    return FileEntry(
        name = name,
        path = absolutePath,
        isDirectory = isDirectory,
        isSymlink = false,
        sizeBytes = length(),
        permissions = (if (canRead()) "r" else "-") + (if (canWrite()) "w" else "-") + (if (canExecute()) "x" else "-"),
        owner = "",
        group = "",
        modified = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", locale).format(java.util.Date(lastModified()))
    )
}
