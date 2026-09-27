package com.shizuku.filemanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shizuku.filemanager.fs.BrowserTab

/**
 * Chrome-style tab strip shown above the file browser when more than one
 * tab is open (or always, if the caller prefers). Wire into MainActivity's
 * Browser branch: render this above FileBrowserScreen, and on tab switch
 * swap `currentScreen`'s initialPath to the tab's stored path — see
 * SUMMARY.md "Wiring tabs" for the exact integration.
 */
@Composable
fun BrowserTabsBar(
    tabs: List<BrowserTab>,
    activeTabId: String?,
    onSelect: (BrowserTab) -> Unit,
    onClose: (BrowserTab) -> Unit,
    onNewTab: () -> Unit,
) {
    if (tabs.size <= 1) return
    Surface(tonalElevation = 2.dp) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(tabs, key = { it.id }) { tab ->
                FilterChip(
                    selected = tab.id == activeTabId,
                    onClick = { onSelect(tab) },
                    label = { Text(tab.label, maxLines = 1) },
                    trailingIcon = {
                        if (tabs.size > 1) {
                            IconButton(onClick = { onClose(tab) }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Close tab", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                )
            }
            item {
                IconButton(onClick = onNewTab) {
                    Icon(Icons.Filled.Add, contentDescription = "New tab")
                }
            }
        }
    }
}
