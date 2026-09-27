package com.shizuku.filemanager.sys

import android.content.Context
import androidx.core.content.edit
import com.shizuku.filemanager.fs.RootManager
import com.shizuku.filemanager.fs.engine.EngineType
import com.shizuku.filemanager.shizuku.ShizukuManager

/**
 * Runs a user-authored shell script against a set of selected file
 * paths. Reuses the same shell channels the rest of the app already has
 * (RootManager's `su -c`, ShizukuManager's shell UID) rather than
 * inventing a third execution path.
 *
 * Placeholders in the script body:
 *   {file}   -> replaced once per invocation (script runs once per file)
 *   {files}  -> replaced once with all paths, single-quoted and space separated (script runs once total)
 * A script using {file} runs N times; one using {files} (and not {file})
 * runs once. Using both is treated as {file} mode.
 */
object ScriptRunner {

    data class SavedScript(val name: String, val body: String)
    data class RunResult(val target: String, val shellResult: ShellResult)

    private const val PREFS = "script_runner_prefs"
    private const val KEY_SCRIPTS = "saved_scripts_v1"

    fun listSavedScripts(context: Context): List<SavedScript> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SCRIPTS, null) ?: return emptyList()
        return raw.split("\u0001").filter { it.isNotBlank() }.mapNotNull { entry ->
            val idx = entry.indexOf('\u0002')
            if (idx < 0) null else SavedScript(entry.substring(0, idx), entry.substring(idx + 1))
        }
    }

    fun saveScript(context: Context, script: SavedScript) {
        val existing = listSavedScripts(context).filterNot { it.name == script.name }
        val updated = existing + script
        val encoded = updated.joinToString("\u0001") { "${it.name}\u0002${it.body}" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY_SCRIPTS, encoded) }
    }

    fun deleteScript(context: Context, name: String) {
        val updated = listSavedScripts(context).filterNot { it.name == name }
        val encoded = updated.joinToString("\u0001") { "${it.name}\u0002${it.body}" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY_SCRIPTS, encoded) }
    }

    /**
     * Executes [scriptBody] against [targetPaths] using [engineType] to
     * decide the shell channel. Only ROOT and SHIZUKU are supported —
     * SAF/STANDARD have no arbitrary shell access, which is by design.
     */
    suspend fun run(engineType: EngineType, scriptBody: String, targetPaths: List<String>): Result<List<RunResult>> {
        if (engineType != EngineType.ROOT && engineType != EngineType.SHIZUKU) {
            return Result.failure(IllegalArgumentException("Scripts require Root or Shizuku access."))
        }
        val results = mutableListOf<RunResult>()
        if (scriptBody.contains("{files}") && !scriptBody.contains("{file}")) {
            val joined = targetPaths.joinToString(" ") { "'${it.replace("'", "'\\''")}'" }
            val command = scriptBody.replace("{files}", joined)
            results.add(RunResult("(all ${targetPaths.size} files)", exec(engineType, command)))
        } else {
            for (path in targetPaths) {
                val escaped = "'${path.replace("'", "'\\''")}'"
                val command = scriptBody.replace("{file}", escaped)
                results.add(RunResult(path, exec(engineType, command)))
            }
        }
        return Result.success(results)
    }

    private suspend fun exec(engineType: EngineType, command: String): ShellResult {
        return when (engineType) {
            EngineType.ROOT -> RootManager.runShell(command)
            EngineType.SHIZUKU -> ShizukuManager.runShell(command)
            else -> ShellResult(-1, "", "Unsupported engine")
        }
    }
}
