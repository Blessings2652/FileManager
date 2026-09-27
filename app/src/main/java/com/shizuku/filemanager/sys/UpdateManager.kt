package com.shizuku.filemanager.sys

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object UpdateManager {
    private val client = OkHttpClient()

    data class UpdateInfo(
        val latestVersion: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val isUpdateAvailable: Boolean
    )

    suspend fun checkForUpdates(currentVersion: String): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/theblacksheep/filemanager/releases/latest")
                .header("User-Agent", "FileManager-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.success(UpdateInfo(currentVersion, "", "https://appteka.store", false))
                }
                val body = response.body.string()
                if (body.isBlank()) {
                    return@withContext Result.failure(Exception("Empty response"))
                }
                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "v1.0").removePrefix("v")
                val bodyText = json.optString("body", "No release notes available.")
                val htmlUrl = json.optString("html_url", "https://appteka.store")

                val isAvailable = compareVersions(tagName, currentVersion) > 0
                Result.success(UpdateInfo(tagName, bodyText, htmlUrl, isAvailable))
            }
        } catch (e: Exception) {
            Result.success(UpdateInfo(currentVersion, "", "https://appteka.store", false))
        }
    }

    private fun compareVersions(latest: String, current: String): Int {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l != c) return l.compareTo(c)
        }
        return 0
    }
}
