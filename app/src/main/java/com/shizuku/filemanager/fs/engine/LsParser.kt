package com.shizuku.filemanager.fs.engine

import com.shizuku.filemanager.fs.FileEntry

/** Parsing for `ls -la --full-time` output, shared by the Shizuku and Root engines. */
internal object LsParser {

    fun parseListing(stdout: String, parentPath: String): List<FileEntry> =
        stdout.lineSequence()
            .drop(1) // "total N" header line
            .mapNotNull { line -> parseLine(line, parentPath) }
            .filter { it.name != "." && it.name != ".." }
            .filter { 
                // Hide redundant/virtual paths in /storage that lead to the same places
                // But keep 'emulated' as it is the primary entry point for internal storage
                if (parentPath == "/storage") {
                    it.name != "self"
                } else true
            }
            .sortedWith(compareByDescending<FileEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
            .toList()

    private fun parseLine(line: String, parentPath: String): FileEntry? {
        if (line.isBlank()) return null
        val parts = line.trim().split(Regex("\\s+"), limit = 9)
        if (parts.size < 9) return null

        val perms = parts[0]
        if (perms.isEmpty()) return null
        val owner = parts[2]
        val group = parts[3]
        val size = parts[4].toLongOrNull() ?: 0L
        val date = parts[5]
        val time = parts[6].substringBefore('.')
        var rawName = parts[8]
        val isSymlink = perms.startsWith("l")
        if (isSymlink && rawName.contains(" -> ")) {
            rawName = rawName.substringBefore(" -> ")
        }
        val isDir = perms.startsWith("d")
        val fullPath = if (parentPath.endsWith("/")) "$parentPath$rawName" else "$parentPath/$rawName"

        return FileEntry(
            name = rawName,
            path = fullPath,
            isDirectory = isDir,
            isSymlink = isSymlink,
            sizeBytes = size,
            permissions = perms,
            owner = owner,
            group = group,
            modified = "$date $time"
        )
    }

    fun quote(path: String) = "'" + path.replace("'", "'\\''") + "'"
}
