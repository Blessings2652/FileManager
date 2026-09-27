package com.shizuku.filemanager.sys

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class MountPoint(
    val device: String,
    val mountPoint: String,
    val fsType: String,
    val options: String
)

object VolumeManager {
    suspend fun getMountPoints(): List<MountPoint> = withContext(Dispatchers.IO) {
        try {
            val mounts = File("/proc/mounts")
            if (!mounts.exists()) return@withContext emptyList()
            
            mounts.readLines().mapNotNull { line ->
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 4) {
                    val mountPoint = parts[1]
                    // Filter out virtual/redundant mounts to keep the list clean
                    if (mountPoint.startsWith("/mnt/runtime") || 
                        mountPoint.startsWith("/mnt/user") ||
                        mountPoint == "/storage/emulated" ||
                        mountPoint == "/storage/self"
                    ) {
                        return@mapNotNull null
                    }
                    
                    MountPoint(
                        device = parts[0],
                        mountPoint = mountPoint,
                        fsType = parts[2],
                        options = parts[3]
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
