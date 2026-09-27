package com.shizuku.filemanager.permissions

import com.shizuku.filemanager.fs.RootManager
import com.shizuku.filemanager.shizuku.ShizukuManager
import com.shizuku.filemanager.sys.ShellResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class PermissionsManager {

    private suspend fun runShell(command: String): ShellResult {
        if (RootManager.isAvailable.value) {
            val res = RootManager.runShell(command)
            if (res.exitCode != -1) return res
        }
        if (ShizukuManager.isAvailable.value && ShizukuManager.hasPermission.value) {
            val res = ShizukuManager.runShell(command)
            if (res.exitCode != -1) return res
        }
        return runNormalShell(command)
    }

    private suspend fun runNormalShell(command: String): ShellResult = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val exit = process.waitFor()
            ShellResult(exit, stdout, stderr)
        } catch (e: Exception) {
            ShellResult(-1, "", e.message ?: "sh exec failed")
        }
    }

    suspend fun readPermissions(path: String): FilePermissions? = withContext(Dispatchers.IO) {
        if (path.startsWith("content://")) return@withContext null

        val result = runShell("stat -c \"%a %U %G\" \"$path\"")
        if (result.exitCode != 0) return@withContext null

        val output = result.stdout.trim()
        val parts = output.split(Regex("\\s+"))
        if (parts.size < 3) return@withContext null

        val octalStr = parts[0].padStart(3, '0')
        val len = octalStr.length
        if (len < 3) return@withContext null

        val owner = PermissionTriple.fromInt(octalStr[len - 3].digitToIntOrNull() ?: 0)
        val group = PermissionTriple.fromInt(octalStr[len - 2].digitToIntOrNull() ?: 0)
        val other = PermissionTriple.fromInt(octalStr[len - 1].digitToIntOrNull() ?: 0)

        FilePermissions(
            owner = owner,
            group = group,
            other = other,
            ownerUser = parts[1],
            ownerGroup = parts[2]
        )
    }

    suspend fun applyPermissions(
        path: String,
        permissions: FilePermissions,
        recursive: Boolean
    ): PermissionResult = withContext(Dispatchers.IO) {
        val rFlag = if (recursive) "-R " else ""
        
        val chmodResult = runShell("chmod ${rFlag}${permissions.octalString} \"$path\"")
        if (chmodResult.exitCode != 0) {
            return@withContext PermissionResult.Failure("chmod failed: ${chmodResult.stderr}")
        }

        val chownResult = runShell("chown ${rFlag}${permissions.ownerUser}:${permissions.ownerGroup} \"$path\"")
        if (chownResult.exitCode != 0) {
            return@withContext PermissionResult.Failure("chown failed: ${chownResult.stderr}")
        }

        PermissionResult.Success
    }
}
