package com.shizuku.filemanager.sys

import com.shizuku.filemanager.fs.RootManager

/**
 * Root-only block device / mount browser. Useful on a rooted HyperOS
 * device for inspecting partitions (e.g. before/after A/B slot flips,
 * checking what's mounted where) without leaving the app for a
 * terminal emulator.
 */
object PartitionManager {

    data class BlockDevice(val name: String, val majorMinor: String, val sizeBlocks: Long, val path: String)
    data class MountPoint(val device: String, val mountPath: String, val fsType: String, val options: String)

    suspend fun listBlockDevices(): Result<List<BlockDevice>> {
        val result = RootManager.runShell("cat /proc/partitions")
        if (result.exitCode != 0) return Result.failure(Exception(result.stderr.ifBlank { "Failed to read /proc/partitions" }))
        val devices = result.stdout.lines().drop(1).mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 4) return@mapNotNull null
            val majorMinor = "${parts[0]}:${parts[1]}"
            val blocks = parts[2].toLongOrNull() ?: return@mapNotNull null
            val name = parts[3]
            BlockDevice(name, majorMinor, blocks, "/dev/block/$name")
        }
        return Result.success(devices)
    }

    suspend fun listMounts(): Result<List<MountPoint>> {
        val result = RootManager.runShell("cat /proc/mounts")
        if (result.exitCode != 0) return Result.failure(Exception(result.stderr.ifBlank { "Failed to read /proc/mounts" }))
        val mounts = result.stdout.lines().mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 4) return@mapNotNull null
            MountPoint(device = parts[0], mountPath = parts[1], fsType = parts[2], options = parts[3])
        }
        return Result.success(mounts)
    }

    /** df -h equivalent for a specific mount path, for a quick free-space check next to the mount list. */
    suspend fun diskFree(mountPath: String): Result<String> {
        val result = RootManager.runShell("df -h '$mountPath'")
        return if (result.exitCode == 0) Result.success(result.stdout) else Result.failure(Exception(result.stderr))
    }

    /**
     * Remounts [mountPath] read-write or read-only. Genuinely dangerous on
     * system/vendor partitions on a locked-down A/B device — callers must
     * gate this behind an explicit confirmation dialog, never a single tap.
     */
    suspend fun remount(mountPath: String, readWrite: Boolean): Result<Unit> {
        val mode = if (readWrite) "rw" else "ro"
        val result = RootManager.runShell("mount -o remount,$mode '$mountPath'")
        return if (result.exitCode == 0) Result.success(Unit) else Result.failure(Exception(result.stderr.ifBlank { "remount failed" }))
    }
}
