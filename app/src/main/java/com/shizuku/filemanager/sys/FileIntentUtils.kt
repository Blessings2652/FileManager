package com.shizuku.filemanager.sys

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.shizuku.filemanager.fs.FileEntry
import com.shizuku.filemanager.fs.engine.EngineType
import com.shizuku.filemanager.fs.engine.FileEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FileIntentUtils {
    suspend fun openWith(context: Context, entry: FileEntry, engine: FileEngine) {
        val uri = withContext(Dispatchers.IO) {
            try {
                if (engine.type == EngineType.SAF) {
                    Uri.parse(entry.path)
                } else if (engine.type == EngineType.STANDARD) {
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", File(entry.path))
                } else {
                    // Root or Shizuku: Copy to cache
                    val cacheDir = File(context.cacheDir, "shared_files")
                    if (!cacheDir.exists()) cacheDir.mkdirs()
                    
                    val tempFile = File(cacheDir, entry.name)
                    val bytes = engine.readBytes(entry.path).getOrThrow()
                    tempFile.writeBytes(bytes)
                    
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
                }
            } catch (e: Exception) {
                null
            }
        }

        if (uri == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to prepare file for sharing", Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        val extension = entry.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: when {
            entry.isImage -> "image/*"
            entry.isVideo -> "video/*"
            entry.isAudio -> "audio/*"
            entry.isText -> "text/plain"
            entry.isArchive -> "application/zip"
            else -> "*/*"
        }
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(intent, "Open with")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        withContext(Dispatchers.Main) {
            try {
                context.startActivity(chooser)
            } catch (e: Exception) {
                Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun shareFile(context: Context, entry: FileEntry, engine: FileEngine) {
        val uri = withContext(Dispatchers.IO) {
            try {
                if (engine.type == EngineType.SAF) {
                    Uri.parse(entry.path)
                } else if (engine.type == EngineType.STANDARD) {
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", File(entry.path))
                } else {
                    // Root or Shizuku: Copy to cache
                    val cacheDir = File(context.cacheDir, "shared_files")
                    if (!cacheDir.exists()) cacheDir.mkdirs()

                    val tempFile = File(cacheDir, entry.name)
                    val bytes = engine.readBytes(entry.path).getOrThrow()
                    tempFile.writeBytes(bytes)

                    FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
                }
            } catch (e: Exception) {
                null
            }
        }

        if (uri == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to prepare file for sharing", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val extension = entry.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: when {
            entry.isImage -> "image/*"
            entry.isVideo -> "video/*"
            entry.isAudio -> "audio/*"
            entry.isText -> "text/plain"
            entry.isArchive -> "application/zip"
            else -> "*/*"
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share file")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        withContext(Dispatchers.Main) {
            try {
                context.startActivity(chooser)
            } catch (e: Exception) {
                Toast.makeText(context, "No app found to share this file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun shareFiles(context: Context, entries: List<FileEntry>, engine: FileEngine) {
        val filesOnly = entries.filter { !it.isDirectory }
        if (filesOnly.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "No files selected to share", Toast.LENGTH_SHORT).show()
            }
            return
        }
        val uris = withContext(Dispatchers.IO) {
            filesOnly.mapNotNull { entry ->
                try {
                    if (engine.type == EngineType.SAF) {
                        Uri.parse(entry.path)
                    } else if (engine.type == EngineType.STANDARD) {
                        FileProvider.getUriForFile(context, "${context.packageName}.provider", File(entry.path))
                    } else {
                        val cacheDir = File(context.cacheDir, "shared_files")
                        if (!cacheDir.exists()) cacheDir.mkdirs()

                        val tempFile = File(cacheDir, entry.name)
                        val bytes = engine.readBytes(entry.path).getOrThrow()
                        tempFile.writeBytes(bytes)

                        FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }

        if (uris.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to prepare files for sharing", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share files")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        withContext(Dispatchers.Main) {
            try {
                context.startActivity(chooser)
            } catch (e: Exception) {
                Toast.makeText(context, "No app found to share these files", Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun editWith(context: Context, entry: FileEntry, engine: FileEngine) {
        val uri = withContext(Dispatchers.IO) {
            try {
                if (engine.type == EngineType.SAF) {
                    Uri.parse(entry.path)
                } else if (engine.type == EngineType.STANDARD) {
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", File(entry.path))
                } else {
                    val cacheDir = File(context.cacheDir, "shared_files")
                    if (!cacheDir.exists()) cacheDir.mkdirs()
                    val tempFile = File(cacheDir, entry.name)
                    val bytes = engine.readBytes(entry.path).getOrThrow()
                    tempFile.writeBytes(bytes)
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
                }
            } catch (e: Exception) {
                null
            }
        }

        if (uri == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to prepare file for editing", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val extension = entry.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/*"

        val intent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Edit photo with")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        withContext(Dispatchers.Main) {
            try {
                context.startActivity(chooser)
            } catch (e: Exception) {
                try {
                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val viewChooser = Intent.createChooser(viewIntent, "Open with")
                    viewChooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(viewChooser)
                } catch (ex: Exception) {
                    Toast.makeText(context, "No app found to edit this photo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    suspend fun installApk(context: Context, entry: FileEntry, engine: FileEngine) {
        val uri = withContext(Dispatchers.IO) {
            try {
                if (engine.type == EngineType.SAF) {
                    Uri.parse(entry.path)
                } else if (engine.type == EngineType.STANDARD) {
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", File(entry.path))
                } else {
                    val cacheDir = File(context.cacheDir, "shared_apks")
                    if (!cacheDir.exists()) cacheDir.mkdirs()

                    val tempFile = File(cacheDir, entry.name)
                    val bytes = engine.readBytes(entry.path).getOrThrow()
                    tempFile.writeBytes(bytes)

                    FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
                }
            } catch (e: Exception) {
                null
            }
        }

        if (uri == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to prepare APK for installation", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        withContext(Dispatchers.Main) {
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to launch Android Package Installer", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
