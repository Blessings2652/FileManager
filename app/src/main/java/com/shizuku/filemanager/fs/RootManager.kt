package com.shizuku.filemanager.fs

import androidx.compose.runtime.mutableStateOf
import com.shizuku.filemanager.sys.ShellResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Root access via `su -c "<command>"`, spawned per call with Runtime.exec.
 * Simple and reliable across root managers (Magisk, KernelSU, etc.) at the
 * cost of one su invocation per command — fine for a file browser, which
 * isn't calling this in a tight loop.
 */
object RootManager {

    val checked = mutableStateOf(false)
    val isAvailable = mutableStateOf(false)

    fun isSuBinaryPresent(): Boolean {
        val paths = System.getenv("PATH")?.split(":") ?: emptyList()
        for (path in paths) {
            if (java.io.File(path, "su").exists()) return true
        }
        // Common fallback locations
        val commonPaths = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/xbin/su", "/data/local/bin/su", "/data/local/su")
        return commonPaths.any { java.io.File(it).exists() }
    }

    /** Triggers the root grant prompt (if not already granted) and records whether su worked. */
    suspend fun checkAccess(): Boolean = withContext(Dispatchers.IO) {
        val result = runShell("id")
        val ok = result.exitCode == 0 && result.stdout.contains("uid=0")
        isAvailable.value = ok
        checked.value = true
        ok
    }

    suspend fun runShell(command: String): ShellResult = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val exit = process.waitFor()
            ShellResult(exit, stdout, stderr)
        } catch (e: Throwable) {
            ShellResult(-1, "", e.message ?: "su exec failed (root unavailable?)")
        }
    }
}
