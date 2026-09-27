package com.shizuku.filemanager.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shizuku.filemanager.db.Bookmark
import com.shizuku.filemanager.fs.BookmarksManager
import com.shizuku.filemanager.fs.engine.EngineType
import kotlinx.coroutines.launch

/**
 * Standalone bookmarks manager (also mirrored, condensed, in the nav
 * drawer). Lets the user prune or jump to any pinned folder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onBack: () -> Unit,
    onOpenBookmark: (path: String, engineType: EngineType) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bookmarks by BookmarksManager.bookmarks.collectAsState()

    LaunchedEffect(Unit) { BookmarksManager.refresh(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bookmarks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (bookmarks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No bookmarks yet. Long-press a folder and choose \"Bookmark\".")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(bookmarks, key = { it.path + it.engineType }) { bookmark: Bookmark ->
                    ListItem(
                        headlineContent = { Text(bookmark.label) },
                        supportingContent = { Text(bookmark.path, maxLines = 1) },
                        leadingContent = { Icon(Icons.Filled.Folder, contentDescription = null) },
                        trailingContent = {
                            IconButton(onClick = {
                                scope.launch {
                                    BookmarksManager.remove(context, bookmark.path, EngineType.valueOf(bookmark.engineType))
                                }
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove bookmark")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable {
                            onOpenBookmark(bookmark.path, EngineType.valueOf(bookmark.engineType))
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
