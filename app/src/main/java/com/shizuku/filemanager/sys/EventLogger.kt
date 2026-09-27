package com.shizuku.filemanager.sys

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileEvent(
    val fileName: String,
    val path: String,
    val type: String, // NEW, MODIFIED, DELETED
    val timestamp: Long
)

object EventLogger {
    private const val LOG_FILE = "file_events.json"

    private val _eventsFlow = MutableSharedFlow<FileEvent>(extraBufferCapacity = 64)
    val eventsFlow: SharedFlow<FileEvent> = _eventsFlow

    suspend fun logEvent(context: Context, event: FileEvent) = withContext(Dispatchers.IO) {
        _eventsFlow.tryEmit(event)
        val events = getEvents(context).toMutableList()
        events.add(0, event)
        if (events.size > 500) events.removeAt(events.size - 1)
        saveEvents(context, events)
    }

    suspend fun getEvents(context: Context): List<FileEvent> = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, LOG_FILE)
        if (!file.exists()) return@withContext emptyList()
        try {
            val json = JSONArray(file.readText())
            val events = mutableListOf<FileEvent>()
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                events.add(FileEvent(
                    fileName = obj.getString("name"),
                    path = obj.getString("path"),
                    type = obj.getString("type"),
                    timestamp = obj.getLong("time")
                ))
            }
            events
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun saveEvents(context: Context, events: List<FileEvent>) = withContext(Dispatchers.IO) {
        val json = JSONArray()
        events.forEach { event ->
            json.put(JSONObject().apply {
                put("name", event.fileName)
                put("path", event.path)
                put("type", event.type)
                put("time", event.timestamp)
            })
        }
        File(context.filesDir, LOG_FILE).writeText(json.toString())
    }

    suspend fun clearEvents(context: Context) = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, LOG_FILE)
        if (file.exists()) {
            file.delete()
        }
    }
}
