package com.shizuku.filemanager.fs

import com.shizuku.filemanager.fs.engine.FileEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield

/**
 * Recursive search under a starting directory, unlike FileBrowserViewModel's
 * existing single-folder filename filter. Streams results as a Flow so the
 * UI can show matches incrementally on a large tree instead of blocking
 * until the whole subtree is walked.
 */
object GlobalSearch {

    data class Options(
        val query: String,
        val matchContent: Boolean = false,
        val caseSensitive: Boolean = false,
        val extensions: Set<String> = emptySet(), // empty = any
        val minSizeBytes: Long? = null,
        val maxSizeBytes: Long? = null,
        val maxDepth: Int = 30,
        /** Content search only scans files up to this size, to avoid reading huge binaries. */
        val contentScanLimitBytes: Long = 2L * 1024 * 1024,
    )

    data class Match(val entry: FileEntry, val parentPath: String, val matchedInContent: Boolean)

    fun search(engine: FileEngine, startPath: String, options: Options): Flow<Match> = flow {
        if (options.query.isBlank()) return@flow
        val needle = if (options.caseSensitive) options.query else options.query.lowercase()
        walk(engine, startPath, options, needle, depth = 0, emit = { emit(it) })
    }

    private suspend fun walk(
        engine: FileEngine,
        path: String,
        options: Options,
        needle: String,
        depth: Int,
        emit: suspend (Match) -> Unit,
    ) {
        if (depth > options.maxDepth) return
        val listing = engine.list(path).getOrNull() ?: return
        for (entry in listing) {
            yield() // cooperative cancellation point for a "stop search" button
            val nameForMatch = if (options.caseSensitive) entry.name else entry.name.lowercase()
            val nameMatches = nameForMatch.contains(needle)

            val passesFilters = (options.extensions.isEmpty() || options.extensions.contains(entry.extension.lowercase())) &&
                (options.minSizeBytes == null || entry.sizeBytes >= options.minSizeBytes) &&
                (options.maxSizeBytes == null || entry.sizeBytes <= options.maxSizeBytes)

            if (!entry.isDirectory && passesFilters) {
                if (nameMatches) {
                    emit(Match(entry, path, matchedInContent = false))
                } else if (options.matchContent && entry.isText && entry.sizeBytes in 1..options.contentScanLimitBytes) {
                    val text = engine.readText(entry.path).getOrNull()
                    val haystack = if (options.caseSensitive) text else text?.lowercase()
                    if (haystack != null && haystack.contains(needle)) {
                        emit(Match(entry, path, matchedInContent = true))
                    }
                }
            } else if (entry.isDirectory && !entry.isSymlink) {
                if (nameMatches && passesFilters) emit(Match(entry, path, matchedInContent = false))
                walk(engine, entry.path, options, needle, depth + 1, emit)
            }
        }
    }
}
