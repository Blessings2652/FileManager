package com.shizuku.filemanager.sys

import com.shizuku.filemanager.fs.FileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

object FolderCategoryDetector {

    /**
     * Instantly returns a category label based on the folder's name or path.
     */
    fun getFolderLabelByName(folderName: String, path: String): String {
        val name = folderName.lowercase(Locale.ROOT)
        val p = path.lowercase(Locale.ROOT)

        return when {
            name == "dcim" || name == "camera" || name == "pictures" || name == "photos" ||
                    name == "gallery" || name == "screenshots" || name == "snapchat" || name == "instagram" ||
                    p.contains("dcim") || p.contains("pictures") -> "Photos & Videos"

            name == "download" || name == "downloads" -> "Downloads"

            name == "music" || name == "audio" || name == "podcasts" || name == "ringers" ||
                    name == "ringtones" || name == "alarms" || name == "notifications" || name == "spotify" -> "Music & Audio"

            name == "movies" || name == "videos" || name == "movies & tv" || name == "screenrecorder" || name == "vlc" -> "Videos & Movies"

            name == "documents" || name == "docs" || name == "pdf" || name == "books" || name == "ebooks" || name == "kindle" -> "Documents & PDFs"

            name == "android" || name == "data" || name == "obb" -> "System Data"

            name == "telegram" || name == "whatsapp" || name == "signal" || name == "messenger" || name == "viber" -> "Chat Media"

            name == "backup" || name == "backups" || name == "twrp" -> "Backups"

            name == "zip" || name == "archives" || name == "extracted" -> "Archives"

            name == "code" || name == "projects" || name == "github" || name == "src" || name == "workspace" -> "Source Code"

            name == "apk" || name == "apks" || name == "apps" -> "APKs & Apps"

            name == ".safe" || name.contains("vault") || name.contains("private") -> "Encrypted Vault"

            name == ".trash" || name == "trash" -> "Recycle Bin"

            name == "recordings" || name == "voice" -> "Voice Recordings"

            else -> ""
        }
    }

    /**
     * Scans folder contents on disk asynchronously to determine item counts and dominant content type.
     */
    suspend fun getFolderLabel(entry: FileEntry): String = withContext(Dispatchers.IO) {
        if (!entry.isDirectory) return@withContext ""

        val nameLabel = getFolderLabelByName(entry.name, entry.path)

        try {
            val file = File(entry.path)
            if (file.exists() && file.isDirectory) {
                val files = file.listFiles()
                if (files == null || files.isEmpty()) {
                    return@withContext if (nameLabel.isNotEmpty()) "$nameLabel (Empty)" else "Empty"
                }

                val totalCount = files.size
                if (nameLabel.isNotEmpty()) {
                    return@withContext "$nameLabel ($totalCount)"
                }

                var imgCount = 0
                var vidCount = 0
                var audCount = 0
                var docCount = 0
                var archiveCount = 0
                var apkCount = 0
                var folderCount = 0

                for (f in files) {
                    if (f.isDirectory) {
                        folderCount++
                    } else {
                        val ext = f.extension.lowercase(Locale.ROOT)
                        when {
                            listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif").contains(ext) -> imgCount++
                            listOf("mp4", "mkv", "webm", "avi", "3gp", "mov", "flv", "wmv").contains(ext) -> vidCount++
                            listOf("mp3", "wav", "ogg", "flac", "m4a", "aac", "opus", "wma").contains(ext) -> audCount++
                            listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md").contains(ext) -> docCount++
                            listOf("zip", "rar", "7z", "tar", "gz", "bz2").contains(ext) -> archiveCount++
                            ext == "apk" -> apkCount++
                        }
                    }
                }

                val fileCount = totalCount - folderCount
                return@withContext when {
                    imgCount > 0 && imgCount >= fileCount * 0.4 -> "Photos & Images ($totalCount)"
                    vidCount > 0 && vidCount >= fileCount * 0.4 -> "Videos ($totalCount)"
                    audCount > 0 && audCount >= fileCount * 0.4 -> "Audio & Music ($totalCount)"
                    docCount > 0 && docCount >= fileCount * 0.4 -> "Documents ($totalCount)"
                    apkCount > 0 && apkCount >= fileCount * 0.4 -> "APKs & Apps ($totalCount)"
                    archiveCount > 0 && archiveCount >= fileCount * 0.4 -> "Archives ($totalCount)"
                    folderCount == totalCount -> "$folderCount subfolders"
                    else -> "$totalCount items"
                }
            }
        } catch (_: Exception) {}

        return@withContext nameLabel.ifEmpty { "Folder" }
    }
}
