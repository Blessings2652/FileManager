package com.shizuku.filemanager.sys.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.concurrent.TimeUnit

/**
 * Data model representing aggregated usage information for a single app
 * over a given query window.
 */
data class AppUsageInfo(
    val packageName: String,
    val totalForegroundTimeMs: Long,
    val launchCount: Int,
    val lastTimeUsedMs: Long,
    val lastTimeVisibleMs: Long,
    val firstTimeStampMs: Long,
    val lastTimeStampMs: Long
) {
    val totalForegroundTimeMinutes: Long
        get() = TimeUnit.MILLISECONDS.toMinutes(totalForegroundTimeMs)

    val totalForegroundTimeFormatted: String
        get() {
            val totalMinutes = totalForegroundTimeMinutes
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
}

enum class UsageInterval {
    TODAY,
    LAST_24_HOURS,
    LAST_7_DAYS,
    LAST_30_DAYS;

    fun toRangeMs(): Pair<Long, Long> {
        val end = System.currentTimeMillis()
        val start = when (this) {
            TODAY -> {
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            LAST_24_HOURS -> end - TimeUnit.HOURS.toMillis(24)
            LAST_7_DAYS -> end - TimeUnit.DAYS.toMillis(7)
            LAST_30_DAYS -> end - TimeUnit.DAYS.toMillis(30)
        }
        return start to end
    }
}

/**
 * Wraps [UsageStatsManager] to provide per-app usage stats.
 */
class AppUsageRepository(private val context: Context) {

    private val usageStatsManager: UsageStatsManager
        get() = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun queryUsage(
        interval: UsageInterval = UsageInterval.TODAY,
        packageFilter: Set<String>? = null
    ): List<AppUsageInfo> {
        val (startTime, endTime) = interval.toRangeMs()
        return queryUsage(startTime, endTime, packageFilter)
    }

    fun queryUsage(
        startTime: Long,
        endTime: Long,
        packageFilter: Set<String>? = null
    ): List<AppUsageInfo> {
        if (!hasUsageAccess()) return emptyList()

        val statsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        val launchCounts = countLaunches(startTime, endTime, packageFilter)

        val results = mutableListOf<AppUsageInfo>()

        for ((packageName, stats) in statsMap) {
            if (packageFilter != null && packageName !in packageFilter) continue
            if (stats.totalTimeInForeground <= 0L && (launchCounts[packageName] ?: 0) == 0) continue

            results.add(
                AppUsageInfo(
                    packageName = packageName,
                    totalForegroundTimeMs = stats.totalTimeInForeground,
                    launchCount = launchCounts[packageName] ?: 0,
                    lastTimeUsedMs = stats.lastTimeUsed,
                    lastTimeVisibleMs = if (android.os.Build.VERSION.SDK_INT >= 29) stats.lastTimeVisible else stats.lastTimeUsed,
                    firstTimeStampMs = stats.firstTimeStamp,
                    lastTimeStampMs = stats.lastTimeStamp
                )
            )
        }

        return results.sortedByDescending { it.totalForegroundTimeMs }
    }

    private fun countLaunches(
        startTime: Long,
        endTime: Long,
        packageFilter: Set<String>?
    ): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        val events: UsageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            val isForegroundEvent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            } else {
                @Suppress("DEPRECATION")
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            }

            if (!isForegroundEvent) continue

            val pkg = event.packageName ?: continue
            if (packageFilter != null && pkg !in packageFilter) continue

            counts[pkg] = (counts[pkg] ?: 0) + 1
        }

        return counts
    }
}
