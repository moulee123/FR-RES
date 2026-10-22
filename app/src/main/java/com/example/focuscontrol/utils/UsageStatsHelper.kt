package com.example.focuscontrol.utils

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.focuscontrol.model.AppDetailAnalytics
import com.example.focuscontrol.model.AppUsageStat
import com.example.focuscontrol.model.CategoryUsageStat
import com.example.focuscontrol.model.ChartBarData
import com.example.focuscontrol.model.ChartType
import com.example.focuscontrol.model.PeriodUsageSummary
import com.example.focuscontrol.model.ReportingPeriod
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object UsageStatsHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
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

    private fun getDateFormatter() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private fun getDayLabelFormatter() = SimpleDateFormat("EEE", Locale.getDefault())
    private fun getDateDisplayFormatter() = SimpleDateFormat("MMM d", Locale.getDefault())
    private fun getMonthLabelFormatter() = SimpleDateFormat("MMM", Locale.getDefault())

    fun getPeriodBoundaries(
        period: ReportingPeriod,
        customStartMs: Long = 0L,
        customEndMs: Long = 0L
    ): Triple<Pair<Long, Long>, Pair<Long, Long>, ChartType> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        return when (period) {
            ReportingPeriod.TODAY -> {
                cal.timeInMillis = now
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val end = now

                // Previous: Yesterday same elapsed time
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val prevStart = cal.timeInMillis
                val prevEnd = prevStart + (end - start)
                Triple(Pair(start, end), Pair(prevStart, prevEnd), ChartType.HOURLY)
            }

            ReportingPeriod.YESTERDAY -> {
                cal.timeInMillis = now
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                val dayMs = 24 * 60 * 60 * 1000L
                Triple(Pair(start, end), Pair(start - dayMs, end - dayMs), ChartType.HOURLY)
            }

            ReportingPeriod.THIS_WEEK -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val end = now

                cal.add(Calendar.WEEK_OF_YEAR, -1)
                val prevStart = cal.timeInMillis
                val prevEnd = prevStart + (end - start)
                Triple(Pair(start, end), Pair(prevStart, prevEnd), ChartType.DAILY)
            }

            ReportingPeriod.LAST_WEEK -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.add(Calendar.WEEK_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                val weekMs = 7 * 24 * 60 * 60 * 1000L
                Triple(Pair(start, end), Pair(start - weekMs, end - weekMs), ChartType.DAILY)
            }

            ReportingPeriod.THIS_MONTH -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val end = now

                cal.add(Calendar.MONTH, -1)
                val prevStart = cal.timeInMillis
                val prevEnd = prevStart + (end - start)
                Triple(Pair(start, end), Pair(prevStart, prevEnd), ChartType.WEEKLY)
            }

            ReportingPeriod.LAST_MONTH -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                cal.timeInMillis = start
                cal.add(Calendar.MONTH, -1)
                val prevStart = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val prevEnd = cal.timeInMillis

                Triple(Pair(start, end), Pair(prevStart, prevEnd), ChartType.WEEKLY)
            }

            ReportingPeriod.THIS_YEAR -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val end = now

                cal.add(Calendar.YEAR, -1)
                val prevStart = cal.timeInMillis
                val prevEnd = prevStart + (end - start)
                Triple(Pair(start, end), Pair(prevStart, prevEnd), ChartType.MONTHLY)
            }

            ReportingPeriod.CUSTOM -> {
                val start = if (customStartMs > 0L) customStartMs else now - 7 * 24 * 60 * 60 * 1000L
                val end = if (customEndMs > 0L) customEndMs else now
                val duration = end - start
                val prevStart = start - duration
                val prevEnd = start
                val chartType = if (duration <= 2 * 24 * 60 * 60 * 1000L) {
                    ChartType.HOURLY
                } else if (duration <= 31 * 24 * 60 * 60 * 1000L) {
                    ChartType.DAILY
                } else {
                    ChartType.MONTHLY
                }
                Triple(Pair(start, end), Pair(prevStart, prevEnd), chartType)
            }
        }
    }

    private fun queryExactUsageEventsMap(
        context: Context,
        startTimeMs: Long,
        endTimeMs: Long,
        targetPackage: String? = null
    ): Map<String, Long> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usageStatsManager.queryEvents(startTimeMs, endTimeMs)
        val usageMap = mutableMapOf<String, Long>()
        val activeResumes = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (targetPackage != null && pkg != targetPackage) continue

            if (pkg == "com.android.systemui") continue

            val time = event.timeStamp
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    activeResumes[pkg] = maxOf(time, startTimeMs)
                }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val start = activeResumes.remove(pkg) ?: 0L
                    if (start > 0L && time > start) {
                        val sessionDuration = minOf(time - start, 4 * 60 * 60 * 1000L)
                        val existing = usageMap[pkg] ?: 0L
                        usageMap[pkg] = existing + sessionDuration
                    }
                }
            }
        }

        // Account for any app currently in foreground (resumed but not paused yet)
        val maxDurationMs = maxOf(0L, endTimeMs - startTimeMs)
        for ((pkg, resumeTime) in activeResumes) {
            if (resumeTime > 0L && endTimeMs > resumeTime) {
                val openDuration = minOf(endTimeMs - resumeTime, 2 * 60 * 60 * 1000L)
                val existing = usageMap[pkg] ?: 0L
                usageMap[pkg] = existing + openDuration
            }
        }

        // Cap single app usage for this period window
        val cappedMap = mutableMapOf<String, Long>()
        for ((pkg, totalMs) in usageMap) {
            val capped = minOf(totalMs, maxDurationMs)
            if (capped > 0) {
                cappedMap[pkg] = capped
            }
        }
        return cappedMap
    }

    fun getUsageForRange(
        context: Context,
        startTimeMs: Long,
        endTimeMs: Long,
        targetPackage: String? = null
    ): Map<String, Long> {
        val now = System.currentTimeMillis()
        val maxPeriodMs = maxOf(60_000L, endTimeMs - startTimeMs)

        val calToday = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStartMs = calToday.timeInMillis
        val todayStr = getDateFormatter().format(calToday.time)

        // Special handling for TODAY:
        // Use exact UsageEvents between local midnight and now to guarantee 100% accurate, un-duplicated today's screen time!
        val isTodayQuery = (startTimeMs >= todayStartMs && endTimeMs <= now + 60_000L)

        if (isTodayQuery) {
            val exactTodayMap = queryExactUsageEventsMap(context, todayStartMs, now, targetPackage)
            
            // Check if cached summary for today in DailyUsageStore contains an inflated/corrupted value (> 24h or invalid)
            val cachedTodaySummary = DailyUsageStore.getDailySummary(context, todayStr)
            val cachedTotal = cachedTodaySummary?.values?.sum() ?: 0L
            val elapsedTodayMs = maxOf(60_000L, now - todayStartMs)

            if (cachedTotal > elapsedTodayMs || cachedTotal > 24 * 60 * 60 * 1000L) {
                // Invalidate & rebuild corrupted today's summary from clean UsageEvents!
                DailyUsageStore.removeSummary(context, todayStr)
                DailyUsageStore.saveDailySummary(context, todayStr, exactTodayMap)
            } else {
                DailyUsageStore.reconcileAndSave(context, todayStr, exactTodayMap)
            }
            return exactTodayMap
        }

        // Past Days / Multi-Day Queries:
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTimeMs,
            endTimeMs
        )

        val usageMap = mutableMapOf<String, Long>()
        if (stats != null && stats.isNotEmpty()) {
            val packageStatsGroup = stats.groupBy { it.packageName }
            for ((pkg, statList) in packageStatsGroup) {
                if (targetPackage != null && pkg != targetPackage) continue

                if (pkg == "com.android.systemui") continue

                // Take maximum totalTimeInForeground among relevant stats (do not sum cumulative daily buckets!)
                val relevantStats = statList.filter { it.lastTimeUsed >= startTimeMs }
                val bestTime = if (relevantStats.isNotEmpty()) {
                    relevantStats.maxOf { it.totalTimeInForeground }
                } else {
                    statList.maxOf { it.totalTimeInForeground }
                }

                val cappedTime = minOf(bestTime, maxPeriodMs)
                if (cappedTime > 0) {
                    usageMap[pkg] = cappedTime
                }
            }
        }

        // Reconcile with saved daily summaries for historical dates
        val cal = Calendar.getInstance()
        cal.timeInMillis = startTimeMs
        val startStr = getDateFormatter().format(cal.time)
        cal.timeInMillis = endTimeMs
        val endStr = getDateFormatter().format(cal.time)

        val summaries = DailyUsageStore.getSummariesForDateRange(context, startStr, endStr)
        for ((_, map) in summaries) {
            for ((pkg, timeMs) in map) {
                if (targetPackage != null && pkg != targetPackage) continue
                val existing = usageMap[pkg] ?: 0L
                val capped = minOf(maxOf(existing, timeMs), maxPeriodMs)
                if (capped > 0) {
                    usageMap[pkg] = capped
                }
            }
        }

        return usageMap
    }

    fun getPeriodUsageSummary(
        context: Context,
        period: ReportingPeriod,
        customStartMs: Long = 0L,
        customEndMs: Long = 0L,
        selectedBarStartMs: Long = 0L,
        selectedBarEndMs: Long = 0L
    ): PeriodUsageSummary {
        val (currentPair, prevPair, chartType) = getPeriodBoundaries(period, customStartMs, customEndMs)
        val (startTime, endTime) = currentPair
        val (prevStart, prevEnd) = prevPair

        val activeStart = if (selectedBarStartMs > 0L) selectedBarStartMs else startTime
        val activeEnd = if (selectedBarEndMs > 0L) selectedBarEndMs else endTime

        val rawUsageMap = getUsageForRange(context, activeStart, activeEnd)
        val prevUsageMap = getUsageForRange(context, prevStart, prevEnd)

        val pm = context.packageManager
        val statsList = mutableListOf<AppUsageStat>()
        val categoryTotals = mutableMapOf<String, Long>()
        var totalMs = 0L

        for ((pkg, timeMs) in rawUsageMap) {
            if (timeMs > 30_000L) { // Filter noise (< 30s)
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val name = pm.getApplicationLabel(appInfo).toString()
                    val iconDrawable = pm.getApplicationIcon(appInfo)
                    val categoryName = getAppCategoryName(appInfo, name)

                    statsList.add(
                        AppUsageStat(
                            packageName = pkg,
                            appName = name,
                            timeForegroundMs = timeMs,
                            icon = iconDrawable.toBitmap().asImageBitmap(),
                            category = categoryName
                        )
                    )
                    totalMs += timeMs
                    categoryTotals[categoryName] = (categoryTotals[categoryName] ?: 0L) + timeMs
                } catch (_: PackageManager.NameNotFoundException) {
                    // ignore uninstalled/unresolvable apps
                }
            }
        }

        val sortedAppStats = statsList.sortedByDescending { it.timeForegroundMs }
        val prevTotalMs = prevUsageMap.values.sum()

        // Category breakdown
        val categoryStats = categoryTotals.map { (cat, catTime) ->
            CategoryUsageStat(
                categoryName = cat,
                timeMs = catTime,
                percentage = if (totalMs > 0) (catTime.toFloat() / totalMs.toFloat()) else 0f
            )
        }.sortedByDescending { it.timeMs }

        // Compute days count for daily average
        val daysDiff = maxOf(1L, (endTime - startTime) / (24 * 60 * 60 * 1000L))
        val dailyAvg = totalMs / daysDiff

        // Chart Bars Generation
        val chartBars = generateChartBars(context, startTime, endTime, chartType, selectedBarStartMs, null)

        // Peak Hour calculation
        val peakHour = computePeakHour(context, startTime, endTime)

        val calStart = Calendar.getInstance().apply { timeInMillis = startTime }
        val calEnd = Calendar.getInstance().apply { timeInMillis = endTime }

        return PeriodUsageSummary(
            period = period,
            startDateStr = getDateDisplayFormatter().format(calStart.time),
            endDateStr = getDateDisplayFormatter().format(calEnd.time),
            totalTimeMs = totalMs,
            prevPeriodTotalTimeMs = prevTotalMs,
            appCount = sortedAppStats.size,
            mostUsedApp = sortedAppStats.firstOrNull(),
            dailyAverageMs = dailyAvg,
            peakHour = peakHour,
            appStats = sortedAppStats,
            categoryStats = categoryStats,
            chartType = chartType,
            chartBars = chartBars
        )
    }

    fun getAppDetailAnalytics(
        context: Context,
        packageName: String,
        period: ReportingPeriod,
        customStartMs: Long = 0L,
        customEndMs: Long = 0L
    ): AppDetailAnalytics {
        val (currentPair, prevPair, chartType) = getPeriodBoundaries(period, customStartMs, customEndMs)
        val (startTime, endTime) = currentPair
        val (prevStart, prevEnd) = prevPair

        val currentUsageMap = getUsageForRange(context, startTime, endTime, targetPackage = packageName)
        val prevUsageMap = getUsageForRange(context, prevStart, prevEnd, targetPackage = packageName)

        val totalTimeMs = currentUsageMap[packageName] ?: 0L
        val prevTotalMs = prevUsageMap[packageName] ?: 0L

        val pm = context.packageManager
        var appName = packageName
        var iconBitmap = null as ImageBitmap?
        var categoryName = "Other"

        try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            appName = pm.getApplicationLabel(appInfo).toString()
            iconBitmap = pm.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
            categoryName = getAppCategoryName(appInfo, appName)
        } catch (_: PackageManager.NameNotFoundException) {
            // fallback
        }

        val eventsData = queryAppEventsData(context, packageName, startTime, endTime)
        val chartBars = generateChartBars(context, startTime, endTime, chartType, 0L, packageName)

        return AppDetailAnalytics(
            packageName = packageName,
            appName = appName,
            icon = iconBitmap,
            categoryName = categoryName,
            period = period,
            totalTimeMs = totalTimeMs,
            prevPeriodTotalTimeMs = prevTotalMs,
            estimatedSessions = eventsData.launchCount,
            firstRecordedTimeMs = eventsData.firstTimestamp,
            lastRecordedTimeMs = eventsData.lastTimestamp,
            hourlyDistribution = eventsData.hourlyMs,
            chartType = chartType,
            chartBars = chartBars
        )
    }

    private data class EventsQueryResult(
        val launchCount: Int,
        val firstTimestamp: Long,
        val lastTimestamp: Long,
        val hourlyMs: List<Long>
    )

    private fun queryAppEventsData(
        context: Context,
        packageName: String,
        startTimeMs: Long,
        endTimeMs: Long
    ): EventsQueryResult {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usageStatsManager.queryEvents(startTimeMs, endTimeMs)

        var launchCount = 0
        var firstTime = 0L
        var lastTime = 0L
        val hourlyMsArray = LongArray(24)

        var lastResumeTime = 0L
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName == packageName) {
                val time = event.timeStamp
                if (firstTime == 0L) firstTime = time
                lastTime = time

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        launchCount++
                        lastResumeTime = time
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                        if (lastResumeTime > 0L && time > lastResumeTime) {
                            val duration = minOf(time - lastResumeTime, 3 * 60 * 60 * 1000L)
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = lastResumeTime
                            val hour = cal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
                            hourlyMsArray[hour] += duration
                            lastResumeTime = 0L
                        }
                    }
                }
            }
        }

        return EventsQueryResult(launchCount, firstTime, lastTime, hourlyMsArray.toList())
    }

    private fun querySystemHourlyEventsData(
        context: Context,
        startTimeMs: Long,
        endTimeMs: Long,
        targetPackage: String? = null
    ): LongArray {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usageStatsManager.queryEvents(startTimeMs, endTimeMs)
        val hourlyMsArray = LongArray(24)
        val activeResumes = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (targetPackage != null && pkg != targetPackage) continue

            if (pkg == "com.android.systemui") continue

            val time = event.timeStamp
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    activeResumes[pkg] = time
                }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val start = activeResumes.remove(pkg) ?: 0L
                    if (start > 0L && time > start) {
                        val duration = minOf(time - start, 3 * 60 * 60 * 1000L)
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = start
                        val hour = cal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
                        hourlyMsArray[hour] += duration
                    }
                }
            }
        }
        return hourlyMsArray
    }

    private fun computePeakHour(context: Context, startTimeMs: Long, endTimeMs: Long): Int {
        val hourlyTotalMs = querySystemHourlyEventsData(context, startTimeMs, endTimeMs)
        var maxHour = 14 // default 2 PM
        var maxMs = -1L
        for (h in 0..23) {
            if (hourlyTotalMs[h] > maxMs) {
                maxMs = hourlyTotalMs[h]
                maxHour = h
            }
        }
        return maxHour
    }

    private fun generateChartBars(
        context: Context,
        startTimeMs: Long,
        endTimeMs: Long,
        chartType: ChartType,
        selectedStartMs: Long,
        targetPackage: String?
    ): List<ChartBarData> {
        val bars = mutableListOf<ChartBarData>()
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()

        when (chartType) {
            ChartType.HOURLY -> {
                cal.timeInMillis = startTimeMs
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                val hourlyMsArray = querySystemHourlyEventsData(context, startTimeMs, endTimeMs, targetPackage)

                for (h in 0..23) {
                    val hStart = cal.timeInMillis
                    cal.add(Calendar.HOUR_OF_DAY, 1)
                    val hEnd = cal.timeInMillis

                    val isFutureHour = hStart > now
                    val valueMs = if (isFutureHour) 0L else hourlyMsArray[h]
                    val label = String.format(Locale.getDefault(), "%02d:00", h)

                    bars.add(
                        ChartBarData(
                            label = label,
                            sublabel = String.format(Locale.getDefault(), "%02d:00 - %02d:00", h, (h + 1) % 24),
                            valueMs = valueMs,
                            startTimeMs = hStart,
                            endTimeMs = hEnd,
                            isSelected = (selectedStartMs in hStart until hEnd),
                            isFuture = isFutureHour
                        )
                    )
                }
            }

            ChartType.DAILY -> {
                cal.timeInMillis = startTimeMs
                while (cal.timeInMillis < endTimeMs) {
                    val dStart = cal.timeInMillis
                    val dayLabel = getDayLabelFormatter().format(cal.time)
                    val dateSublabel = getDateDisplayFormatter().format(cal.time)

                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    val dEnd = minOf(endTimeMs, cal.timeInMillis)

                    val dayMap = getUsageForRange(context, dStart, dEnd, targetPackage)
                    val valMs = if (targetPackage != null) (dayMap[targetPackage] ?: 0L) else dayMap.values.sum()

                    bars.add(
                        ChartBarData(
                            label = dayLabel,
                            sublabel = dateSublabel,
                            valueMs = valMs,
                            startTimeMs = dStart,
                            endTimeMs = dEnd,
                            isSelected = (selectedStartMs in dStart until dEnd)
                        )
                    )
                }
            }

            ChartType.WEEKLY -> {
                cal.timeInMillis = startTimeMs
                var weekNum = 1
                while (cal.timeInMillis < endTimeMs) {
                    val wStart = cal.timeInMillis
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                    val wEnd = minOf(endTimeMs, cal.timeInMillis)

                    val weekMap = getUsageForRange(context, wStart, wEnd, targetPackage)
                    val valMs = if (targetPackage != null) (weekMap[targetPackage] ?: 0L) else weekMap.values.sum()

                    bars.add(
                        ChartBarData(
                            label = "W$weekNum",
                            sublabel = "Week $weekNum",
                            valueMs = valMs,
                            startTimeMs = wStart,
                            endTimeMs = wEnd,
                            isSelected = (selectedStartMs in wStart until wEnd)
                        )
                    )
                    weekNum++
                }
            }

            ChartType.MONTHLY -> {
                cal.timeInMillis = startTimeMs
                while (cal.timeInMillis < endTimeMs) {
                    val mStart = cal.timeInMillis
                    val monthLabel = getMonthLabelFormatter().format(cal.time)

                    cal.add(Calendar.MONTH, 1)
                    val mEnd = minOf(endTimeMs, cal.timeInMillis)

                    val monthMap = getUsageForRange(context, mStart, mEnd, targetPackage)
                    val valMs = if (targetPackage != null) (monthMap[targetPackage] ?: 0L) else monthMap.values.sum()

                    bars.add(
                        ChartBarData(
                            label = monthLabel,
                            sublabel = monthLabel,
                            valueMs = valMs,
                            startTimeMs = mStart,
                            endTimeMs = mEnd,
                            isSelected = (selectedStartMs in mStart until mEnd)
                        )
                    )
                }
            }
        }

        return bars
    }

    private fun getAppCategoryName(appInfo: ApplicationInfo, appName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> return "Games"
                ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO -> return "Entertainment & Media"
                ApplicationInfo.CATEGORY_SOCIAL -> return "Social Media"
                ApplicationInfo.CATEGORY_PRODUCTIVITY, ApplicationInfo.CATEGORY_ACCESSIBILITY -> return "Productivity"
                ApplicationInfo.CATEGORY_NEWS -> return "News & Reading"
                ApplicationInfo.CATEGORY_MAPS, ApplicationInfo.CATEGORY_IMAGE -> return "Utilities & Travel"
                else -> { /* proceed to fallback */ }
            }
        }

        val lowerPkg = appInfo.packageName.lowercase(Locale.ROOT)
        val lowerName = appName.lowercase(Locale.ROOT)

        return when {
            lowerPkg.contains("game") || lowerPkg.contains("pubg") || lowerPkg.contains("clash") -> "Games"
            lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerPkg.contains("spotify") || lowerPkg.contains("tiktok") -> "Entertainment & Media"
            lowerPkg.contains("instagram") || lowerPkg.contains("whatsapp") || lowerPkg.contains("facebook") || lowerPkg.contains("twitter") || lowerPkg.contains("telegram") -> "Social Media"
            lowerPkg.contains("chrome") || lowerPkg.contains("browser") || lowerPkg.contains("doc") || lowerPkg.contains("drive") || lowerPkg.contains("mail") || lowerPkg.contains("notes") -> "Productivity"
            lowerName.contains("map") || lowerName.contains("uber") || lowerName.contains("weather") || lowerName.contains("file") -> "Utilities & Travel"
            else -> "Other"
        }
    }
}
