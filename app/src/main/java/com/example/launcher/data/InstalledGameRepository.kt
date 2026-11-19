package com.example.launcher.data

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Process
import com.example.launcher.domain.model.Game
import com.example.launcher.domain.repository.GameRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledGameRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : GameRepository {

    private val packageManager: PackageManager get() = context.packageManager

    override suspend fun getGames(): List<Game> = withContext(Dispatchers.IO) {
        val hasUsage = hasUsageAccess()
        val usage = if (hasUsage) loadUsageTotals() else emptyMap()

        queryLauncherApps()
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            // Hanya aplikasi yang dikategorikan sebagai game (app sendiri dikecualikan)
            .filter { it.packageName != context.packageName && it.category == ApplicationInfo.CATEGORY_GAME }
            .map { info ->
                val stats = usage[info.packageName]
                Game(
                    id = info.packageName,
                    title = info.loadLabel(packageManager).toString(),
                    packageName = info.packageName,
                    lastPlayedAt = stats?.lastTimeUsed?.takeIf { it > 0L },
                    hoursPlayed = if (hasUsage) ((stats?.totalTimeInForeground ?: 0L) / MS_PER_HOUR).toInt() else null,
                )
            }
            .sortedWith(compareByDescending<Game> { it.lastPlayedAt ?: 0L }.thenBy { it.title.lowercase() })
    }

    override suspend fun getMonthlyActivity(packageName: String): List<Float?> = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val monthStart = today.withDayOfMonth(1)

        val hoursByDate: Map<LocalDate, Double> = if (hasUsageAccess()) {
            val manager = context.getSystemService(UsageStatsManager::class.java)
            manager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                monthStart.atStartOfDay(zone).toInstant().toEpochMilli(),
                System.currentTimeMillis(),
            )
                .filter { it.packageName == packageName }
                .groupBy { Instant.ofEpochMilli(it.firstTimeStamp).atZone(zone).toLocalDate() }
                .mapValues { (_, list) -> list.sumOf { it.totalTimeInForeground } / MS_PER_HOUR_DOUBLE }
        } else {
            emptyMap()
        }

        val leadingBlanks = List(monthStart.dayOfWeek.value - 1) { null } // Senin = 1
        val days = (1..monthStart.lengthOfMonth()).map { day ->
            val date = monthStart.withDayOfMonth(day)
            if (date.isAfter(today)) null else (hoursByDate[date] ?: 0.0).toFloat()
        }
        leadingBlanks + days
    }

    override fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun loadUsageTotals(): Map<String, UsageStats> {
        val manager = context.getSystemService(UsageStatsManager::class.java)
        val end = System.currentTimeMillis()
        return manager.queryAndAggregateUsageStats(end - ONE_YEAR_MS, end)
    }

    private fun queryLauncherApps(): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
    }

    private companion object {
        const val MS_PER_HOUR = 3_600_000L
        const val MS_PER_HOUR_DOUBLE = 3_600_000.0
        const val ONE_YEAR_MS = 365L * 24 * MS_PER_HOUR
    }
}
