package com.shizuku.filemanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shizuku.filemanager.fs.FileEntry
import com.shizuku.filemanager.fs.engine.EnginePrefs
import com.shizuku.filemanager.fs.engine.FileEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    entry: FileEntry,
    engine: FileEngine,
    onBack: () -> Unit
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    
    // Search/Replace state
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val searchHighlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val visualTransformation = remember(entry.name, searchQuery, searchHighlightColor) {
        CodeVisualTransformation(entry.name, searchQuery, searchHighlightColor)
    }

    val resultsCount = remember(textFieldValue.text, searchQuery) {
        if (searchQuery.isEmpty()) 0
        else {
            var count = 0
            var index = textFieldValue.text.indexOf(searchQuery, ignoreCase = true)
            while (index != -1) {
                count++
                index = textFieldValue.text.indexOf(searchQuery, index + searchQuery.length, ignoreCase = true)
                if (count >= 1000) break
            }
            count
        }
    }

    LaunchedEffect(entry.path) {
        if (entry.sizeBytes > 5 * 1024 * 1024) { // 5MB limit
            snackbarHostState.showSnackbar("File too large to edit safely (>5MB)")
            isLoading = false
            return@LaunchedEffect
        }
        engine.readText(entry.path).onSuccess {
            textFieldValue = TextFieldValue(it)
            isLoading = false
        }.onFailure {
            snackbarHostState.showSnackbar("Error reading file: ${it.message}")
            isLoading = false
        }
    }

    ScreenScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                ModernTopBar(
                    title = entry.name,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                            Icon(
                                if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Toggle Search",
                                tint = if (isSearchVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        if (entry.name.endsWith(".json", true) || entry.name.endsWith(".xml", true)) {
                            IconButton(onClick = {
                                try {
                                    val currentText = textFieldValue.text
                                    val newText = if (entry.name.endsWith(".json", true)) {
                                        org.json.JSONObject(currentText).toString(4)
                                    } else {
                                        currentText.replace("><", ">\n<")
                                    }
                                    textFieldValue = textFieldValue.copy(text = newText)
                                } catch (e: Exception) {
                                    scope.launch { snackbarHostState.showSnackbar("Format failed: ${e.message}") }
                                }
                            }) {
                                Icon(Icons.Default.Code, contentDescription = "Format")
                            }
                        }
                        
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).padding(4.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            IconButton(onClick = {
                                scope.launch {
                                    isSaving = true
                                    engine.writeText(entry.path, textFieldValue.text).onSuccess {
                                        snackbarHostState.showSnackbar("Saved successfully")
                                    }.onFailure {
                                        snackbarHostState.showSnackbar("Save failed: ${it.message}")
                                    }
                                    isSaving = false
                                }
                            }) {
                                Icon(Icons.Default.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )
                
                if (isSearchVisible) {
                    SearchReplaceBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        replaceQuery = replaceQuery,
                        onReplaceQueryChange = { replaceQuery = it },
                        onReplaceNext = {
                            val cursor = textFieldValue.selection.start
                            val code = textFieldValue.text
                            var index = code.indexOf(searchQuery, cursor, ignoreCase = true)
                            if (index == -1) {
                                index = code.indexOf(searchQuery, 0, ignoreCase = true)
                            }
                            if (index != -1) {
                                val newText = code.substring(0, index) + replaceQuery + code.substring(index + searchQuery.length)
                                textFieldValue = textFieldValue.copy(
                                    text = newText,
                                    selection = TextRange(index + replaceQuery.length)
                                )
                            }
                        },
                        onReplaceAll = {
                            val newText = textFieldValue.text.replace(searchQuery, replaceQuery, ignoreCase = true)
                            textFieldValue = textFieldValue.copy(text = newText)
                        },
                        resultsCount = resultsCount
                    )
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val context = LocalContext.current
            val fontSizeMultiplier = remember { EnginePrefs.getFontSizeMultiplier(context) }
            
            BasicTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp * fontSizeMultiplier,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = visualTransformation
            )
        }
    }
}

@Composable
fun SearchReplaceBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    replaceQuery: String,
    onReplaceQueryChange: (String) -> Unit,
    onReplaceNext: () -> Unit,
    onReplaceAll: () -> Unit,
    resultsCount: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            Text(
                                text = if (resultsCount >= 1000) "999+" else "$resultsCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = onReplaceQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Replace with", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = RoundedCornerShape(8.dp)
                )
                Button(
                    onClick = onReplaceNext,
                    enabled = resultsCount > 0,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Replace")
                }
                FilledTonalButton(
                    onClick = onReplaceAll,
                    enabled = resultsCount > 0,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("All")
                }
            }
        }
    }
}

class CodeVisualTransformation(
    private val entryName: String,
    private val searchQuery: String,
    private val searchHighlightColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val code = text.text
        val isKotlin = entryName.endsWith(".kt") || entryName.endsWith(".kts")
        
        val annotated = if (isKotlin) {
            highlightKotlin(code)
        } else {
            AnnotatedString(code)
        }

        if (searchQuery.isEmpty()) {
            return TransformedText(annotated, OffsetMapping.Identity)
        }

        val builder = AnnotatedString.Builder(annotated)
        var index = code.indexOf(searchQuery, ignoreCase = true)
        var count = 0
        while (index != -1 && count < 1000) {
            builder.addStyle(
                SpanStyle(background = searchHighlightColor),
                index,
                index + searchQuery.length
            )
            index = code.indexOf(searchQuery, index + searchQuery.length, ignoreCase = true)
            count++
        }
        
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private fun highlightKotlin(code: String): AnnotatedString {
        val keywords = listOf(
            "package", "import", "class", "interface", "fun", "val", "var",
            "if", "else", "when", "for", "while", "do", "return", "break", "continue",
            "throw", "try", "catch", "finally", "object", "companion", "typealias",
            "this", "super", "in", "is", "as", "null", "true", "false",
            "public", "private", "protected", "internal", "override", "abstract",
            "final", "open", "sealed", "data", "inline", "noinline", "crossinline",
            "out", "in", "reified", "suspend", "tailrec", "operator", "infix",
            "external", "annotation", "enum", "const", "lateinit", "vararg"
        )

        return buildAnnotatedString {
            var lastIndex = 0
            val pattern = Regex(
                "(\".*?\")|('.*?')|(//.*)|(/\\*.*?\\*/)|(\\b(${keywords.joinToString("|")})\\b)|(\\d+)",
                RegexOption.DOT_MATCHES_ALL
            )
            
            pattern.findAll(code).forEach { result ->
                append(code.substring(lastIndex, result.range.first))
                
                val style = when {
                    result.groupValues[1].isNotEmpty() || result.groupValues[2].isNotEmpty() -> 
                        SpanStyle(color = Color(0xFF6A8759)) // Strings
                    result.groupValues[3].isNotEmpty() || result.groupValues[4].isNotEmpty() -> 
                        SpanStyle(color = Color(0xFF808080)) // Comments
                    result.groupValues[5].isNotEmpty() -> 
                        SpanStyle(color = Color(0xFFCC7832), fontWeight = FontWeight.Bold) // Keywords
                    result.groupValues[7].isNotEmpty() -> 
                        SpanStyle(color = Color(0xFF6897BB)) // Numbers
                    else -> SpanStyle()
                }
                
                withStyle(style) {
                    append(result.value)
                }
                lastIndex = result.range.last + 1
            }
            append(code.substring(lastIndex))
        }
    }
}
