package com.shizuku.filemanager.fs

import android.content.Context
import java.io.File

class FileManagerAgent(
    val context: Context,
    val apiKey: String,
    val baseDirectory: File
) {
    var onOpenFileRequested: ((String) -> Unit)? = null
    var onNavigateRequested: ((String) -> Unit)? = null
}
