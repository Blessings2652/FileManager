package com.shizuku.filemanager.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateOf
import com.shizuku.filemanager.sys.ShellResult
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method
import kotlin.coroutines.resume

/**
 * Thin wrapper around Shizuku: binder lifecycle, permission state, and a
 * runShell() helper that executes commands as the `shell` user (UID 2000)
 * via Shizuku.newProcess() (reflection into the hidden ProcessBuilder path).
 *
 * Shell UID gets you read access to most of /sdcard, /storage, and a good
 * chunk of /data/system logs & world-readable app dirs — not full root, but
 * far more than the app sandbox. Write access outside app-owned dirs is
 * limited to what `shell` can touch (varies by OEM / SELinux policy).
 */
object ShizukuManager {

    const val REQUEST_CODE = 9001

    val isAvailable = mutableStateOf(false)
    val hasPermission = mutableStateOf(false)

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        isAvailable.value = true
        refreshPermission()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        isAvailable.value = false
        hasPermission.value = false
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE) {
                hasPermission.value = grantResult == PackageManager.PERMISSION_GRANTED
            }
        }

    fun init() {
        isAvailable.value = try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
        refreshPermission()
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
    }

    /**
     * Auto-detects if the Shizuku app is installed on the user's mobile device.
     */
    fun isAppInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: Throwable) {
            try {
                pm.getPackageInfo("rikka.app.shizuku", 0)
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    /**
     * Launches the installed Shizuku app so the user can start the Shizuku service.
     */
    fun launchShizukuApp(context: Context): Boolean {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            ?: pm.getLaunchIntentForPackage("rikka.app.shizuku")
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } else {
            false
        }
    }

    fun destroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }

    private fun refreshPermission() {
        hasPermission.value = try {
            if (Shizuku.isPreV11()) {
                false
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Throwable) {
            false
        }
    }

    fun requestPermission() {
        try {
            if (!Shizuku.isPreV11() && !hasPermission.value) {
                Shizuku.requestPermission(REQUEST_CODE)
            }
        } catch (e: Throwable) {
            // Shizuku not running
        }
    }

    // Shizuku.newProcess(String[], String[], String) is no longer a public API on
    // recent shizuku-api versions (it's still there internally, just access-restricted),
    // so it has to be invoked via reflection. Resolved lazily and cached.
    private val newProcessMethod: Method? by lazy {
        try {
            Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }
        } catch (e: Throwable) {
            null
        }
    }

    private fun newShizukuProcess(cmd: Array<String>): Process? {
        val method = newProcessMethod ?: return null
        return method.invoke(null, cmd, null, null) as? Process
    }

    /**
     * Runs a shell command as the shizuku (shell) user and returns combined output.
     * Uses `sh -c` so pipes / redirection in [command] work as expected.
     */
    suspend fun runShell(command: String): ShellResult = suspendCancellableCoroutine { cont ->
        try {
            val res = runShellSync(command)
            cont.resume(res)
        } catch (e: Throwable) {
            cont.resume(ShellResult(-1, "", e.message ?: "Shizuku exec failed"))
        }
    }

    /**
     * Synchronous version of runShell.
     */
    fun runShellSync(command: String): ShellResult {
        return try {
            val process = newShizukuProcess(arrayOf("sh", "-c", command))
                ?: throw IllegalStateException("Shizuku newProcess unavailable on this Shizuku version")
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val exit = process.waitFor()
            ShellResult(exit, stdout, stderr)
        } catch (e: Throwable) {
            ShellResult(-1, "", e.message ?: "Shizuku exec failed")
        }
    }
}
