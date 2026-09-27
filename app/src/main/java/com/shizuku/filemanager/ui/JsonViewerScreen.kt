package com.shizuku.filemanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shizuku.filemanager.fs.FileEntry
import com.shizuku.filemanager.fs.engine.FileEngine
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonViewerScreen(
    entry: FileEntry,
    engine: FileEngine,
    onBack: () -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 5 }
    }

    LaunchedEffect(entry.path) {
        engine.readText(entry.path).onSuccess {
            rawText = it
            isLoading = false
        }.onFailure {
            snackbarHostState.showSnackbar("Error: ${it.message}")
            isLoading = false
        }
    }

    ScreenScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            if (isSearchActive) {
                ModernTopBar(
                    titleContent = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search JSON...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { isSearchActive = false; searchQuery = "" }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    }
                )
            } else {
                ModernTopBar(
                    title = entry.name,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, null)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("JSON", rawText)))
                                snackbarHostState.showSnackbar("Copied to clipboard")
                            }
                        }) {
                            Icon(Icons.Default.ContentCopy, null)
                        }
                    }
                )
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val formattedLines = remember(rawText) {
                try {
                    val tokener = JSONTokener(rawText)
                    val obj = tokener.nextValue()
                    val pretty = when (obj) {
                        is JSONObject -> obj.toString(4)
                        is JSONArray -> obj.toString(4)
                        else -> rawText
                    }
                    pretty.split("\n")
                } catch (e: Exception) {
                    rawText.split("\n")
                }
            }

            val filteredLines = if (searchQuery.isBlank()) {
                formattedLines
            } else {
                formattedLines.filter { it.contains(searchQuery, ignoreCase = true) }
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ) {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(filteredLines) { line ->
                            JsonLine(line, searchQuery)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JsonLine(line: String, query: String) {
    val annotatedString = buildAnnotatedString {
        val trimmed = line.trimStart()
        val indent = line.length - trimmed.length
        append(" ".repeat(indent))

        val keyMatch = "^\"(.*?)\":".toRegex().find(trimmed)
        if (keyMatch != null) {
            val key = keyMatch.groupValues[1]
            val rest = trimmed.substring(keyMatch.range.last + 1)
            
            withStyle(style = SpanStyle(color = Color(0xFFBB86FC), fontWeight = FontWeight.Bold)) {
                append("\"$key\"")
            }
            append(":")
            
            val valueTrimmed = rest.trim()
            if (valueTrimmed.startsWith("\"")) {
                withStyle(style = SpanStyle(color = Color(0xFF81C784))) {
                    append(rest)
                }
            } else if (valueTrimmed.startsWith("true") || valueTrimmed.startsWith("false")) {
                withStyle(style = SpanStyle(color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold)) {
                    append(rest)
                }
            } else if (valueTrimmed.all { it.isDigit() || it == '.' || it == '-' || it == ',' || it == ' ' || it == 'n' || it == 'u' || it == 'l' }) {
                withStyle(style = SpanStyle(color = Color(0xFFFFB74D))) {
                    append(rest)
                }
            } else {
                append(rest)
            }
        } else {
            append(trimmed)
        }
    }

    val finalString = if (query.isNotBlank() && annotatedString.text.contains(query, ignoreCase = true)) {
        buildAnnotatedString {
            val text = annotatedString.text
            var lastIndex = 0
            val pattern = query.toRegex(RegexOption.IGNORE_CASE)
            pattern.findAll(text).forEach { match ->
                append(text.substring(lastIndex, match.range.first))
                withStyle(style = SpanStyle(background = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))) {
                    append(text.substring(match.range.first, match.range.last + 1))
                }
                lastIndex = match.range.last + 1
            }
            append(text.substring(lastIndex))
        }
    } else {
        annotatedString
    }

    Text(
        text = finalString,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.padding(vertical = 1.dp)
    )
}
