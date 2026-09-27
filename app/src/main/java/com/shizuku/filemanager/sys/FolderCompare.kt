package com.shizuku.filemanager.sys

import com.shizuku.filemanager.fs.engine.FileEngine

/**
 * Recursive folder diff against a single FileEngine (comparing two
 * subtrees reachable by the same engine — e.g. two local folders, or
 * two folders inside the same SAF tree). Cross-engine compare would
 * need bytes from both sides fetched through their own engines; callers
 * that need that can call [compareTrees] twice and merge, since each
 * entry only needs its own engine to size/hash.
 */
object FolderCompare {

    enum class DiffType { ADDED, REMOVED, MODIFIED, IDENTICAL }

    data class DiffEntry(
        val relativePath: String,
        val type: DiffType,
        val leftSize: Long? = null,
        val rightSize: Long? = null,
        val leftModified: String? = null,
        val rightModified: String? = null,
    )

    /**
     * Compares [leftPath] and [rightPath] (both directories, both listable
     * by [engine]) and returns a flat diff. Modification detection is by
     * size + modified-timestamp string equality (cheap, no hashing) unless
     * [byContentHash] is true, in which case files up to [hashSizeLimit]
     * bytes are hashed for a byte-accurate comparison at the cost of
     * reading them fully.
     */
    suspend fun compareTrees(
        engine: FileEngine,
        leftPath: String,
        rightPath: String,
        byContentHash: Boolean = false,
        hashSizeLimit: Long = 20L * 1024 * 1024,
    ): List<DiffEntry> {
        val leftFiles = LinkedHashMap<String, com.shizuku.filemanager.fs.FileEntry>()
        val rightFiles = LinkedHashMap<String, com.shizuku.filemanager.fs.FileEntry>()
        walk(engine, leftPath, "", leftFiles)
        walk(engine, rightPath, "", rightFiles)

        val allKeys = (leftFiles.keys + rightFiles.keys).toSortedSet()
        val results = mutableListOf<DiffEntry>()

        for (key in allKeys) {
            val l = leftFiles[key]
            val r = rightFiles[key]
            when {
                l != null && r == null -> results.add(DiffEntry(key, DiffType.REMOVED, leftSize = l.sizeBytes, leftModified = l.modified))
                l == null && r != null -> results.add(DiffEntry(key, DiffType.ADDED, rightSize = r.sizeBytes, rightModified = r.modified))
                l != null && r != null -> {
                    val same = if (l.isDirectory || r.isDirectory) {
                        l.isDirectory == r.isDirectory
                    } else if (byContentHash && l.sizeBytes <= hashSizeLimit && r.sizeBytes <= hashSizeLimit) {
                        val lb = engine.readBytes(l.path).getOrNull()
                        val rb = engine.readBytes(r.path).getOrNull()
                        lb != null && rb != null && lb.contentEquals(rb)
                    } else {
                        l.sizeBytes == r.sizeBytes && l.modified == r.modified
                    }
                    results.add(
                        DiffEntry(
                            key,
                            if (same) DiffType.IDENTICAL else DiffType.MODIFIED,
                            l.sizeBytes, r.sizeBytes, l.modified, r.modified
                        )
                    )
                }
            }
        }
        return results
    }

    private suspend fun walk(
        engine: FileEngine,
        path: String,
        relativePrefix: String,
        out: MutableMap<String, com.shizuku.filemanager.fs.FileEntry>,
    ) {
        val listing = engine.list(path).getOrNull() ?: return
        for (entry in listing) {
            val rel = if (relativePrefix.isEmpty()) entry.name else "$relativePrefix/${entry.name}"
            out[rel] = entry
            if (entry.isDirectory && !entry.isSymlink) {
                walk(engine, entry.path, rel, out)
            }
        }
    }
}
