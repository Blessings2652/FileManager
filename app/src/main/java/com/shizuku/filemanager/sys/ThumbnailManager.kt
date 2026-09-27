package com.shizuku.filemanager.sys

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import com.shizuku.filemanager.fs.FileEntry
import com.shizuku.filemanager.fs.engine.EngineType
import kotlinx.coroutines.*
import java.io.File

object ThumbnailManager {

    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val memoryCache: LruCache<String, Bitmap> by lazy {
        val maxMemoryKB = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSizeKB = (maxMemoryKB / 8).coerceIn(8192, 32768) // Bounded strictly between 8MB and 32MB
        object : LruCache<String, Bitmap>(cacheSizeKB) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return (bitmap.byteCount / 1024).coerceAtLeast(1)
            }

            override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
                if (evicted && oldValue != newValue && !oldValue.isRecycled) {
                    try {
                        oldValue.recycle()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun isThumbnailSupported(entry: FileEntry): Boolean {
        return entry.isImage || entry.isVideo || entry.isAudio || entry.isPdf || (entry.isDocument && entry.extension.lowercase() == "pdf")
    }

    fun isImage(entry: FileEntry): Boolean {
        return isThumbnailSupported(entry)
    }

    private fun getCacheDir(context: Context) = File(context.cacheDir, "thumbnails").apply { if (!exists()) mkdirs() }

    private fun getCacheFile(context: Context, path: String, modified: String): File {
        val hash = "${path}_$modified".hashCode().toString(16)
        return File(getCacheDir(context), "thumb_$hash.jpg")
    }

    suspend fun clearCache(context: Context): Long = withContext(Dispatchers.IO) {
        memoryCache.evictAll()
        val dir = getCacheDir(context)
        var totalDeleted = 0L
        dir.listFiles()?.forEach { 
            totalDeleted += it.length()
            it.delete()
        }
        totalDeleted
    }
    
    fun preloadThumbnails(context: Context, entries: List<FileEntry>, engineType: EngineType) {
        preloadScope.launch {
            entries.filter { isThumbnailSupported(it) }.take(30).forEach { entry ->
                val cacheKey = "${entry.path}_${entry.modified}"
                if (memoryCache.get(cacheKey) == null) {
                    loadThumbnail(context, entry, engineType)
                }
            }
        }
    }

    suspend fun loadThumbnail(context: Context, entry: FileEntry, engineType: EngineType): Bitmap? = withContext(Dispatchers.IO) {
        if (!isThumbnailSupported(entry)) return@withContext null

        val cacheKey = "${entry.path}_${entry.modified}"
        memoryCache.get(cacheKey)?.let {
            if (!it.isRecycled) return@withContext it else memoryCache.remove(cacheKey)
        }

        val cacheFile = getCacheFile(context, entry.path, entry.modified)
        if (cacheFile.exists()) {
            try {
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath, options)
                if (bitmap != null && !bitmap.isRecycled) {
                    memoryCache.put(cacheKey, bitmap)
                    return@withContext bitmap
                }
            } catch (_: Throwable) {
                cacheFile.delete()
            }
        }

        try {
            val bitmap = when {
                entry.isPdf -> renderPdfThumbnail(context, entry, engineType)
                entry.isVideo -> renderVideoThumbnail(context, entry, engineType)
                entry.isAudio -> renderAudioThumbnail(context, entry, engineType)
                else -> decodeImageFile(entry, engineType, context)
            }

            if (bitmap != null && !bitmap.isRecycled) {
                saveToCache(cacheFile, bitmap)
                memoryCache.put(cacheKey, bitmap)
            }
            bitmap
        } catch (e: OutOfMemoryError) {
            // Free memory cache immediately on OOM
            memoryCache.evictAll()
            System.gc()
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeImageFile(entry: FileEntry, engineType: EngineType, context: Context): Bitmap? {
        return when (engineType) {
            EngineType.STANDARD -> {
                val file = File(entry.path)
                if (file.exists() && file.canRead()) {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(entry.path, options)
                    options.inSampleSize = calculateInSampleSize(options, 200, 200)
                    options.inJustDecodeBounds = false
                    options.inPreferredConfig = Bitmap.Config.RGB_565 // 16-bit RGB saves 50% RAM
                    BitmapFactory.decodeFile(entry.path, options)
                } else null
            }
            EngineType.SAF -> {
                try {
                    val uri = Uri.parse(entry.path)
                    val raw = DocumentsContract.getDocumentThumbnail(context.contentResolver, uri, Point(200, 200), null)
                    raw?.let { scaleAndRecycleIfNeeded(it, 200, 200) }
                } catch (_: Exception) { null }
            }
            EngineType.SHIZUKU, EngineType.ROOT -> {
                if (entry.path.startsWith("/sdcard") || entry.path.startsWith("/storage/emulated/0")) {
                    val file = File(entry.path)
                    if (file.exists() && file.canRead()) {
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeFile(entry.path, options)
                        options.inSampleSize = calculateInSampleSize(options, 200, 200)
                        options.inJustDecodeBounds = false
                        options.inPreferredConfig = Bitmap.Config.RGB_565
                        BitmapFactory.decodeFile(entry.path, options)
                    } else null
                } else null
            }
        }
    }

    private fun saveToCache(file: File, bitmap: Bitmap) {
        try {
            file.outputStream().use { 
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, it)
            }
        } catch (_: Exception) {}
    }

    private fun renderPdfThumbnail(context: Context, entry: FileEntry, engineType: EngineType): Bitmap? {
        return try {
            val file = if (engineType == EngineType.STANDARD) {
                File(entry.path)
            } else {
                if (entry.path.startsWith("/sdcard") || entry.path.startsWith("/storage/emulated/0")) {
                    File(entry.path)
                } else null
            }

            if (file != null && file.exists()) {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                if (renderer.pageCount > 0) {
                    val page = renderer.openPage(0)
                    val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    renderer.close()
                    pfd.close()
                    bitmap
                } else {
                    renderer.close()
                    pfd.close()
                    null
                }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun renderAudioThumbnail(context: Context, entry: FileEntry, engineType: EngineType): Bitmap? {
        return try {
            val mmr = MediaMetadataRetriever()
            if (entry.path.startsWith("content://")) {
                mmr.setDataSource(context, Uri.parse(entry.path))
            } else {
                mmr.setDataSource(entry.path)
            }
            val art = mmr.embeddedPicture
            mmr.release()
            if (art != null) {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(art, 0, art.size, options)
                options.inSampleSize = calculateInSampleSize(options, 200, 200)
                options.inJustDecodeBounds = false
                options.inPreferredConfig = Bitmap.Config.RGB_565
                val decoded = BitmapFactory.decodeByteArray(art, 0, art.size, options)
                if (decoded != null) scaleAndRecycleIfNeeded(decoded, 200, 200) else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun renderVideoThumbnail(context: Context, entry: FileEntry, engineType: EngineType): Bitmap? {
        var rawBitmap: Bitmap? = null
        try {
            val mmr = MediaMetadataRetriever()
            if (entry.path.startsWith("content://")) {
                mmr.setDataSource(context, Uri.parse(entry.path))
            } else {
                val file = File(entry.path)
                if (file.exists() && file.canRead()) {
                    mmr.setDataSource(entry.path)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                rawBitmap = try {
                    mmr.getScaledFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 200, 200)
                } catch (_: Exception) { null }
            }

            if (rawBitmap == null) {
                rawBitmap = mmr.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: mmr.getFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: mmr.frameAtTime
            }
            mmr.release()
        } catch (_: Exception) {
            rawBitmap = null
        }

        if (rawBitmap != null) {
            return scaleAndRecycleIfNeeded(rawBitmap, 200, 200)
        }

        val sysBitmap = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val file = File(entry.path)
                if (file.exists()) {
                    ThumbnailUtils.createVideoThumbnail(file, Size(200, 200), null)
                } else {
                    val uri = Uri.parse(entry.path)
                    context.contentResolver.loadThumbnail(uri, Size(200, 200), null)
                }
            } else {
                @Suppress("DEPRECATION")
                ThumbnailUtils.createVideoThumbnail(entry.path, MediaStore.Video.Thumbnails.MINI_KIND)
            }
        } catch (_: Exception) {
            null
        }

        return if (sysBitmap != null) scaleAndRecycleIfNeeded(sysBitmap, 200, 200) else null
    }

    private fun scaleAndRecycleIfNeeded(source: Bitmap, maxW: Int, maxH: Int): Bitmap {
        if (source.width <= maxW && source.height <= maxH) return source
        val aspect = source.width.toFloat() / source.height.toFloat()
        val targetW = if (source.width > source.height) maxW else (maxH * aspect).toInt().coerceAtLeast(1)
        val targetH = if (source.width > source.height) (maxW / aspect).toInt().coerceAtLeast(1) else maxH
        val scaled = Bitmap.createScaledBitmap(source, targetW, targetH, true)
        if (scaled != source && !source.isRecycled) {
            source.recycle()
        }
        return scaled
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
