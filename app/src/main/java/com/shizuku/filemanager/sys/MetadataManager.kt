package com.shizuku.filemanager.sys

import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class FileMetadata(
    val fileName: String,
    val filePath: String,
    val tags: Map<String, String>,
    val isEditable: Boolean
)

object MetadataManager {

    suspend fun getMetadata(file: File): FileMetadata = withContext(Dispatchers.IO) {
        val tags = mutableMapOf<String, String>()
        val extension = file.extension.lowercase()
        
        if (extension in listOf("jpg", "jpeg", "png", "webp")) {
            try {
                val exif = ExifInterface(file.absolutePath)
                val attributes = listOf(
                    ExifInterface.TAG_DATETIME,
                    ExifInterface.TAG_MAKE,
                    ExifInterface.TAG_MODEL,
                    ExifInterface.TAG_IMAGE_WIDTH,
                    ExifInterface.TAG_IMAGE_LENGTH,
                    ExifInterface.TAG_GPS_LATITUDE,
                    ExifInterface.TAG_GPS_LONGITUDE,
                    ExifInterface.TAG_EXPOSURE_TIME,
                    ExifInterface.TAG_F_NUMBER,
                    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                    ExifInterface.TAG_SOFTWARE,
                    ExifInterface.TAG_USER_COMMENT
                )
                attributes.forEach { attr ->
                    exif.getAttribute(attr)?.let { tags[attr] = it }
                }
            } catch (e: Exception) {
                tags["Error"] = "Could not read Exif: ${e.message}"
            }
        } else if (extension in listOf("mp3", "wav", "m4a", "flac")) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                val metadataKeys = mapOf(
                    "Title" to android.media.MediaMetadataRetriever.METADATA_KEY_TITLE,
                    "Artist" to android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST,
                    "Album" to android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM,
                    "Genre" to android.media.MediaMetadataRetriever.METADATA_KEY_GENRE,
                    "Year" to android.media.MediaMetadataRetriever.METADATA_KEY_YEAR,
                    "Duration" to android.media.MediaMetadataRetriever.METADATA_KEY_DURATION,
                    "Bitrate" to android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE
                )
                metadataKeys.forEach { (name, key) ->
                    retriever.extractMetadata(key)?.let { tags[name] = it }
                }
            } catch (e: Exception) {
                tags["Error"] = "Could not read Audio Metadata: ${e.message}"
            } finally {
                retriever.release()
            }
        } else {
            tags["Size"] = "${file.length()} bytes"
            tags["Last Modified"] = java.util.Date(file.lastModified()).toString()
        }

        FileMetadata(file.name, file.absolutePath, tags, extension in listOf("jpg", "jpeg"))
    }

    suspend fun updateMetadata(filePath: String, newTags: Map<String, String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            val extension = file.extension.lowercase()
            if (extension in listOf("jpg", "jpeg")) {
                val exif = ExifInterface(filePath)
                newTags.forEach { (tag, value) ->
                    exif.setAttribute(tag, value)
                }
                exif.saveAttributes()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Metadata editing not supported for this file type"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
