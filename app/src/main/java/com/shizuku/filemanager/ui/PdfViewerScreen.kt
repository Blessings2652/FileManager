package com.shizuku.filemanager.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shizuku.filemanager.db.DocumentAnnotation
import com.shizuku.filemanager.fs.FileEntry
import com.shizuku.filemanager.fs.engine.EngineType
import com.shizuku.filemanager.fs.engine.FileEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    entry: FileEntry,
    engine: FileEngine,
    onBack: () -> Unit,
    viewModel: PdfViewerViewModel = viewModel(),
) {
    val context = LocalContext.current
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var isLoading by remember { mutableStateOf(value = true) }
    var pageCount by remember { mutableIntStateOf(0) }
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }

    val annotations by viewModel.annotations.collectAsState()
    val searchResults by viewModel.searchResult.collectAsState()
    val isTtsActive by viewModel.isTtsActive.collectAsState()
    
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    var showNoteDialog by remember { mutableStateOf(false) }
    var selectedPageIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(entry.path) {
        isLoading = true
        viewModel.loadAnnotations(entry.path)
        withContext(Dispatchers.IO) {
            try {
                val file = if (engine.type == EngineType.STANDARD) {
                    File(entry.path)
                } else {
                    val cacheDir = File(context.cacheDir, "pdf_cache")
                    if (!cacheDir.exists()) cacheDir.mkdirs()
                    val tempFile = File(cacheDir, entry.name)
                    
                    if (engine.type == EngineType.SAF) {
                        context.contentResolver.openInputStream(entry.path.toUri())?.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } else {
                        val bytes = engine.readBytes(entry.path).getOrThrow()
                        tempFile.writeBytes(bytes)
                    }
                    tempFile
                }
                
                if (file.exists()) {
                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val r = PdfRenderer(pfd)
                    renderer = r
                    pageCount = r.pageCount
                    pdfFile = file
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isLoading = false
    }

    DisposableEffect(Unit) {
        onDispose {
            renderer?.close()
        }
    }

    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    ScreenScaffold(
        topBar = {
            if (isSearchActive) {
                ModernTopBar(
                    titleContent = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { 
                                searchQuery = it
                                viewModel.search(pdfFile?.absolutePath ?: "", it)
                            },
                            placeholder = { Text("Search document...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { isSearchActive = false; searchQuery = "" }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                )
            } else {
                ModernTopBar(
                    title = cleanTitle(entry.name),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, "Search")
                        }
                        IconButton(
                            onClick = {
                                if (isTtsActive) viewModel.stopSpeaking()
                                else viewModel.speakPage(pdfFile?.absolutePath ?: "", scrollState.firstVisibleItemIndex)
                            }
                        ) {
                            Icon(
                                imageVector = if (isTtsActive) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "TTS"
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedPageIndex = scrollState.firstVisibleItemIndex
                    showNoteDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.AutoMirrored.Filled.NoteAdd, "Add Note")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (pageCount > 0 && (renderer != null)) {
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                val modifier = Modifier
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
                                    scale = 2.5f
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

                LazyColumn(
                    modifier = modifier,
                    state = scrollState,
                    userScrollEnabled = scale == 1f,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pageCount) { index ->
                        PdfPageWithExtras(
                            renderer!!, 
                            index, 
                            annotations.filter { it.pageIndex == index },
                            searchResults.any { it.pageIndex == index },
                            onDeleteAnnotation = { viewModel.deleteAnnotation(it) },
                            zoomScale = scale
                        )
                    }
                }
                
                // Search Results Overlay
                if (isSearchActive && searchResults.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .align(Alignment.BottomCenter)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(searchResults.size) { index ->
                                val res = searchResults[index]
                                Text(
                                    "Found on Page ${res.pageIndex + 1}",
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clickable {
                                            scope.launch { scrollState.animateScrollToItem(res.pageIndex) }
                                        },
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNoteDialog) {
        var noteContent by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Add Note to Page ${selectedPageIndex + 1}") },
            text = {
                TextField(
                    value = noteContent,
                    onValueChange = { noteContent = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your note...") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (noteContent.isNotBlank()) {
                        viewModel.addAnnotation(
                            DocumentAnnotation(
                                path = entry.path,
                                pageIndex = selectedPageIndex,
                                content = noteContent,
                                type = "NOTE"
                            )
                        )
                    }
                    showNoteDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PdfPageWithExtras(
    renderer: PdfRenderer, 
    index: Int, 
    pageAnnotations: List<DocumentAnnotation>,
    isSearchMatch: Boolean,
    onDeleteAnnotation: (DocumentAnnotation) -> Unit,
    zoomScale: Float
) {
    Box(contentAlignment = Alignment.TopStart) {
        PdfPage(renderer, index, zoomScale)
        
        Column(modifier = Modifier.padding(8.dp)) {
            if (isSearchMatch) {
                Surface(
                    color = Color.Yellow.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text("Search Match", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 10.sp)
                }
            }
            
            pageAnnotations.forEach { annotation ->
                Surface(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .widthIn(max = 200.dp)
                        .clickable { /* Could show options */ },
                    color = Color(0xFFFFF9C4),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Note, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(annotation.content, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onDeleteAnnotation(annotation) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfPage(renderer: PdfRenderer, index: Int, zoomScale: Float) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentRenderScale by remember { mutableFloatStateOf(0f) }

    // Calculate a target scale to avoid constant re-rendering during pinch.
    // We use discrete steps: 1.5x (default), 3x, and 5x.
    val targetScale = when {
        zoomScale > 3.5f -> 5f
        zoomScale > 1.8f -> 3f
        else -> 1.5f
    }

    LaunchedEffect(index, targetScale) {
        if (targetScale != currentRenderScale) {
            withContext(Dispatchers.IO) {
                try {
                    val page = renderer.openPage(index)
                    val width = (page.width * targetScale).toInt()
                    val height = (page.height * targetScale).toInt()
                    
                    // Safety check for massive bitmaps to avoid OOM
                    if (width * height < 10_000_000) { 
                        val b = createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        page.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap = b
                        currentRenderScale = targetScale
                    }
                    page.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(4.dp)
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    }
}
