package com.shizuku.filemanager.ui

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.shizuku.filemanager.fs.FileEntry
import com.shizuku.filemanager.fs.engine.EnginePrefs
import com.shizuku.filemanager.fs.engine.EngineType
import com.shizuku.filemanager.fs.engine.FileEngine
import com.shizuku.filemanager.sys.MediaPlaybackService
import com.shizuku.filemanager.sys.StorageScanner
import com.shizuku.filemanager.sys.ThumbnailManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    entry: FileEntry,
    engine: FileEngine,
    onBack: () -> Unit,
    onEdit: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentEntry by remember(entry.path) { mutableStateOf(entry) }
    var uri by remember(currentEntry.path) { mutableStateOf<Uri?>(null) }
    var isPreparing by remember(currentEntry.path) { mutableStateOf(true) }
    var siblingMediaItems by remember(currentEntry.path) { mutableStateOf<List<FileEntry>>(emptyList()) }

    LaunchedEffect(currentEntry.path) {
        isPreparing = true
        uri = withContext(Dispatchers.IO) {
            try {
                if (currentEntry.path.startsWith("http://") || currentEntry.path.startsWith("https://") || currentEntry.path.startsWith("content://")) {
                    currentEntry.path.toUri()
                } else if (engine.type == EngineType.STANDARD || File(currentEntry.path).exists()) {
                    Uri.fromFile(File(currentEntry.path))
                } else if (engine.type == EngineType.SAF) {
                    currentEntry.path.toUri()
                } else {
                    val cacheDir = File(context.cacheDir, "shared_media")
                    if (!cacheDir.exists()) cacheDir.mkdirs()

                    val tempFile = File(cacheDir, currentEntry.name)
                    val bytes = engine.readBytes(currentEntry.path).getOrThrow()
                    tempFile.writeBytes(bytes)

                    Uri.fromFile(tempFile)
                }
            } catch (_: Exception) {
                try {
                    val file = File(currentEntry.path)
                    if (file.exists()) Uri.fromFile(file) else currentEntry.path.toUri()
                } catch (_: Exception) {
                    null
                }
            }
        }

        withContext(Dispatchers.IO) {
            try {
                val parent = engine.parentPath(currentEntry.path)
                if (parent != null) {
                    val listResult = engine.list(parent)
                    if (listResult.isSuccess) {
                        siblingMediaItems = listResult.getOrDefault(emptyList()).filter {
                            it.isImage || it.isVideo || it.isAudio
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        isPreparing = false
    }

    val isImage = currentEntry.isImage
    val isVideo = currentEntry.isVideo
    val isAudio = currentEntry.isAudio

    var orientation by rememberSaveable { mutableIntStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }
    val activity = remember(context) { context.findActivity() }

    // Hide status bar and system bars in immersive fullscreen mode
    DisposableEffect(activity) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        }
        onDispose {
            val window = activity?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(isVideo) {
        if (isVideo) {
            activity?.window?.let { window ->
                val params = window.attributes
                params.preferredRefreshRate = 120f
                window.attributes = params
            }
        }
    }

    DisposableEffect(orientation) {
        if (orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            activity?.requestedOrientation = orientation
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(activity) {
        if (isVideo || isAudio) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var showSettings by remember { mutableStateOf(false) }

    var stableAudio by rememberSaveable { mutableStateOf(false) }
    var isCropMode by rememberSaveable { mutableStateOf(false) }
    var playbackSpeed by rememberSaveable { mutableFloatStateOf(1.0f) }
    var selectedQuality by rememberSaveable { mutableStateOf("Auto") }
    val qualities = remember { mutableStateListOf("Auto") }
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }

    ScreenScaffold(
        containerColor = Color.Black,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp)
            )
        }
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentEntry.path, siblingMediaItems) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (horizontalDragOffset < -90f) {
                                val idx = siblingMediaItems.indexOfFirst { it.path == currentEntry.path }
                                if (idx in 0 until siblingMediaItems.lastIndex) {
                                    currentEntry = siblingMediaItems[idx + 1]
                                }
                            } else if (horizontalDragOffset > 90f) {
                                val idx = siblingMediaItems.indexOfFirst { it.path == currentEntry.path }
                                if (idx > 0) {
                                    currentEntry = siblingMediaItems[idx - 1]
                                }
                            }
                            horizontalDragOffset = 0f
                        },
                        onDragCancel = { horizontalDragOffset = 0f },
                        onHorizontalDrag = { _, delta ->
                            horizontalDragOffset += delta
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (isPreparing || uri == null) {
                CircularProgressIndicator(color = Color.White)
            } else {
                val currentUri = uri!!
                when {
                    isImage -> {
                        var scale by remember { mutableFloatStateOf(1f) }
                        var offset by remember { mutableStateOf(Offset.Zero) }

                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = currentUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(1f, 5f)
                                            if (scale > 1f) {
                                                offset += pan
                                            } else {
                                                offset = Offset.Zero
                                            }
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                if (scale > 1f) {
                                                    scale = 1f
                                                    offset = Offset.Zero
                                                } else {
                                                    scale = 3f
                                                }
                                            }
                                        )
                                    }
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                            )

                            ModernTopBar(
                                title = cleanTitle(currentEntry.name),
                                navigationIcon = {
                                    IconButton(onClick = onBack) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                },
                                actions = {
                                    IconButton(onClick = onEdit) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                },
                                containerColor = Color.Black.copy(alpha = 0.4f),
                                contentColor = Color.White
                            )
                        }
                    }
                    isVideo || isAudio -> {
                        VideoPlayer(
                            uri = currentUri,
                            entry = currentEntry,
                            onBack = onBack,
                            snackbarHostState = snackbarHostState,
                            stableAudio = stableAudio,
                            onStableAudioToggle = { stableAudio = !stableAudio },
                            isCropMode = isCropMode,
                            onCropModeToggle = { isCropMode = !isCropMode },
                            onRotateToggle = {
                                orientation = if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                            },
                            playbackSpeed = playbackSpeed,
                            onQualitiesFound = {
                                qualities.clear()
                                qualities.add("Auto")
                                qualities.addAll(it)
                            },
                            onOpenSettings = { showSettings = true }
                        )
                    }
                    else -> {
                        Text("Unsupported file type", color = Color.White)
                    }
                }
            }

            if (siblingMediaItems.size > 1 && isImage) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp)
                        .height(68.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(siblingMediaItems) { item ->
                            val isSelected = item.path == currentEntry.path
                            MediaCarouselThumbnail(
                                item = item,
                                isSelected = isSelected,
                                onClick = { currentEntry = item }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        VideoSettingsDialog(
            playbackSpeed = playbackSpeed,
            onPlaybackSpeedChange = { playbackSpeed = it },
            stableAudio = stableAudio,
            onStableAudioToggle = { stableAudio = !stableAudio },
            qualities = qualities,
            selectedQuality = selectedQuality,
            onQualitySelected = { selectedQuality = it; showSettings = false },
            onDismiss = { showSettings = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoSettingsDialog(
    playbackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    stableAudio: Boolean,
    onStableAudioToggle: () -> Unit,
    qualities: List<String>,
    selectedQuality: String,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Media Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Playback Speed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            val labelText = if (playbackSpeed == 1.0f) "1.0x (Normal)" else "${String.format(Locale.US, "%.2f", playbackSpeed)}x"
                            Text(
                                text = labelText,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Slider(
                        value = playbackSpeed,
                        onValueChange = onPlaybackSpeedChange,
                        valueRange = 0.25f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    val speedPresets = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(speedPresets) { speed ->
                            val isSelected = abs(playbackSpeed - speed) < 0.05f
                            FilterChip(
                                selected = isSelected,
                                onClick = { onPlaybackSpeedChange(speed) },
                                label = {
                                    Text(if (speed == 1.0f) "1.0x Normal" else "${speed}x")
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Stable Volume (Audio Normalization) Option Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStableAudioToggle() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = if (stableAudio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Stable Volume",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Balances loud and quiet sounds automatically",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = stableAudio,
                        onCheckedChange = { onStableAudioToggle() }
                    )
                }
            }

            if (qualities.size > 1) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HighQuality, null, modifier = Modifier.padding(end = 12.dp))
                        Text("Quality", style = MaterialTheme.typography.bodyLarge)
                    }

                    qualities.forEach { quality ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onQualitySelected(quality) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = quality == selectedQuality,
                                onClick = { onQualitySelected(quality) }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(quality, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoPlayer(
    uri: Uri,
    entry: FileEntry,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    stableAudio: Boolean,
    onStableAudioToggle: () -> Unit,
    isCropMode: Boolean,
    onCropModeToggle: () -> Unit,
    onRotateToggle: () -> Unit,
    playbackSpeed: Float,
    onQualitiesFound: (List<String>) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = remember(context) { context.findActivity() }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    var gestureType by remember { mutableStateOf<String?>(null) }
    var gestureValue by remember { mutableFloatStateOf(0f) }
    var showGestureIndicator by remember { mutableStateOf(false) }

    var skipFeedback by remember { mutableStateOf<String?>(null) }

    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val exoPlayer = remember(context) {
        MediaPlaybackService.getOrCreatePlayer(context)
    }

    // Hide status bar and system bars during video playback when controls are hidden
    DisposableEffect(isControlsVisible) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isControlsVisible) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val window = activity?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(stableAudio) {
        exoPlayer.volume = if (stableAudio) 0.85f else 1.0f
    }

    LaunchedEffect(uri) {
        playbackError = null
        MediaPlaybackService.start(context)

        val title = cleanTitle(entry.name)
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(title)
            .setDisplayTitle(title)
            .setArtist("FileManager")
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(entry.path)
            .setMediaMetadata(mediaMetadata)
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        val savedPos = EnginePrefs.getMediaPosition(context, entry.path)
        if (savedPos > 3000L) {
            exoPlayer.seekTo(savedPos)
            currentPosition = savedPos
            scope.launch {
                val formatted = formatTime(savedPos)
                val result = snackbarHostState.showSnackbar(
                    message = "Resumed from $formatted",
                    actionLabel = "Start Over",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    exoPlayer.seekTo(0L)
                    EnginePrefs.clearMediaPosition(context, entry.path)
                }
            }
        }
        exoPlayer.playWhenReady = true
    }

    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    DisposableEffect(exoPlayer, entry.path) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.localizedMessage ?: "Playback failed"
                scope.launch {
                    snackbarHostState.showSnackbar("Playback error: ${error.localizedMessage}")
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                } else if (state == Player.STATE_ENDED) {
                    EnginePrefs.clearMediaPosition(context, entry.path)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onTracksChanged(tracks: Tracks) {
                val foundQualities = mutableListOf<String>()
                for (group in tracks.groups) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        if (format.height > 0) {
                            foundQualities.add("${format.height}p")
                        }
                    }
                }
                if (foundQualities.isNotEmpty()) {
                    onQualitiesFound(foundQualities.distinct().sortedByDescending {
                        it.removeSuffix("p").toIntOrNull() ?: 0
                    })
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (pos > 3000L && (dur <= 0L || pos < dur - 5000L)) {
                EnginePrefs.saveMediaPosition(context, entry.path, pos)
            } else {
                EnginePrefs.clearMediaPosition(context, entry.path)
            }
            exoPlayer.removeListener(listener)
            if (!exoPlayer.isPlaying) {
                MediaPlaybackService.stop(context)
            }
        }
    }

    LaunchedEffect(isPlaying, entry.path) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
            if (currentPosition > 3000L) {
                EnginePrefs.saveMediaPosition(context, entry.path, currentPosition)
            }
            delay(1000.milliseconds)
        }
    }

    LaunchedEffect(isControlsVisible, lastInteractionTime) {
        if (isControlsVisible && isPlaying) {
            delay(4000.milliseconds)
            if (System.currentTimeMillis() - lastInteractionTime >= 3800) {
                isControlsVisible = false
            }
        }
    }

    val onPlayPauseClick = {
        if (isPlaying) {
            exoPlayer.pause()
        } else {
            if (playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0)
            }
            exoPlayer.play()
        }
        lastInteractionTime = System.currentTimeMillis()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        isControlsVisible = !isControlsVisible
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onDoubleTap = { offset ->
                        val isRightSide = offset.x > size.width / 2
                        if (isRightSide) {
                            exoPlayer.seekTo(exoPlayer.currentPosition + 10000)
                            skipFeedback = "+10s"
                        } else {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                            skipFeedback = "-10s"
                        }
                        scope.launch {
                            delay(800.milliseconds)
                            skipFeedback = null
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { showGestureIndicator = true },
                    onDragEnd = { showGestureIndicator = false },
                    onDragCancel = { showGestureIndicator = false },
                    onVerticalDrag = { change, dragAmount ->
                        val isRightSide = change.position.x > size.width / 2
                        if (isRightSide) {
                            gestureType = "Volume"
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val delta = -dragAmount / 50f
                            val newVol = (currentVol + delta).coerceIn(0f, maxVol.toFloat())
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol.toInt(), 0)
                            gestureValue = newVol / maxVol.toFloat()
                        } else {
                            gestureType = "Brightness"
                            val currentActivity = context.findActivity()
                            val window = currentActivity?.window
                            if (window != null) {
                                val params = window.attributes
                                val currentBrightness = if (params.screenBrightness < 0) 0.5f else params.screenBrightness
                                val delta = -dragAmount / 500f
                                val newBrightness = (currentBrightness + delta).coerceIn(0.01f, 1.0f)
                                params.screenBrightness = newBrightness
                                window.attributes = params
                                gestureValue = newBrightness
                            }
                        }
                    }
                )
            }
    ) {
        if (entry.isAudio) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                Color.Black
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "musicDisc")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (isPlaying) 1.06f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Surface(
                        modifier = Modifier
                            .size(200.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shadowElevation = 16.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(96.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    Text(
                        text = cleanTitle(entry.name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(Modifier.height(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${entry.extension.uppercase()} AUDIO • ${StorageScanner.formatSize(entry.sizeBytes)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = if (isCropMode) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    playerView.resizeMode = if (isCropMode) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            ) {
                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    ModernTopBar(
                        title = cleanTitle(entry.name),
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = onOpenSettings) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.White)
                            }
                        },
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (playbackState == Player.STATE_BUFFERING) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(56.dp))
                    } else {
                        IconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier
                                .size(92.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            AnimatedContent(
                                targetState = Pair(isPlaying, playbackState == Player.STATE_ENDED),
                                transitionSpec = {
                                    scaleIn(tween(200)) togetherWith scaleOut(tween(200))
                                },
                                label = "PlayPause"
                            ) { (playing, ended) ->
                                Icon(
                                    imageVector = if (ended) Icons.Default.Replay else if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )

                            Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                                Slider(
                                    value = currentPosition.toFloat(),
                                    onValueChange = {
                                        currentPosition = it.toLong()
                                        exoPlayer.seekTo(it.toLong())
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color(0xFF007AFF),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                                    ),
                                    modifier = Modifier.height(12.dp)
                                )
                            }

                            Text(
                                text = formatTime(duration),
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Quick Action Tool: Crop/Fit Screen
                            IconButton(
                                onClick = {
                                    onCropModeToggle()
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CropFree,
                                    contentDescription = "Crop / Fit Screen",
                                    tint = if (isCropMode) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.75f)
                                )
                            }

                            // Center Playback Controls: -10s, Play/Pause, +10s
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    exoPlayer.seekTo(exoPlayer.currentPosition - 10000)
                                    lastInteractionTime = System.currentTimeMillis()
                                }) {
                                    Icon(Icons.Default.Replay10, null, tint = Color.White)
                                }

                                IconButton(onClick = onPlayPauseClick) {
                                    Icon(
                                        imageVector = if (playbackState == Player.STATE_ENDED) Icons.Default.Replay
                                                      else if (isPlaying) Icons.Default.PauseCircleFilled
                                                      else Icons.Default.PlayCircleFilled,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }

                                IconButton(onClick = {
                                    exoPlayer.seekTo(exoPlayer.currentPosition + 10000)
                                    lastInteractionTime = System.currentTimeMillis()
                                }) {
                                    Icon(Icons.Default.Forward10, null, tint = Color.White)
                                }
                            }

                            // Right Quick Action Tool: Screen Rotation
                            IconButton(
                                onClick = {
                                    onRotateToggle()
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = "Screen Rotation",
                                    tint = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }

        GestureIndicator(
            visible = showGestureIndicator,
            type = gestureType ?: "",
            value = gestureValue,
            modifier = Modifier.align(Alignment.Center)
        )

        AnimatedVisibility(
            visible = skipFeedback != null,
            enter = fadeIn() + scaleIn(initialScale = 0.5f),
            exit = fadeOut() + scaleOut(targetScale = 1.5f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (skipFeedback?.startsWith("+") == true) Icons.Default.Forward10 else Icons.Default.Replay10,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = skipFeedback ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    )
                }
            }
        }

        if (playbackError != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Unable to play media",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        playbackError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onBack) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureIndicator(
    visible: Boolean,
    type: String,
    value: Float,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (type == "Volume") Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.BrightnessMedium,
                    contentDescription = null,
                    tint = Color.White
                )
                Column {
                    Text(type, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { value },
                        modifier = Modifier.width(100.dp).height(6.dp).clip(CircleShape),
                        color = Color(0xFF007AFF),
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

private fun Context.findActivity(): ComponentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
private fun MediaCarouselThumbnail(
    item: FileEntry,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(item.path) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.path) {
        withContext(Dispatchers.IO) {
            bitmap = ThumbnailManager.loadThumbnail(context, item, EngineType.STANDARD)
        }
    }

    Surface(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        color = Color.White.copy(alpha = 0.1f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                bitmap != null -> {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    if (item.isVideo) {
                        Icon(
                            imageVector = Icons.Filled.PlayCircle,
                            contentDescription = "Video",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                item.isImage -> {
                    AsyncImage(
                        model = if (item.path.startsWith("content://")) item.path else File(item.path),
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Icon(
                        imageVector = if (item.isVideo) Icons.Default.Movie else Icons.Default.MusicNote,
                        contentDescription = item.name,
                        tint = if (isSelected) Color(0xFF00E5FF) else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
