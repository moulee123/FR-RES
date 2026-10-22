package com.example.focuscontrol.model

import androidx.compose.ui.graphics.ImageBitmap

enum class ReportingPeriod(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    LAST_WEEK("Last Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom")
}

data class AppUsageStat(
    val packageName: String,
    val appName: String,
    val timeForegroundMs: Long,
    val icon: ImageBitmap?,
    val category: String = "Other",
    val launchCount: Int = 0
)

data class ChartBarData(
    val label: String,
    val sublabel: String = "",
    val valueMs: Long,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val isSelected: Boolean = false,
    val isFuture: Boolean = false
)

enum class ChartType {
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY
}

data class CategoryUsageStat(
    val categoryName: String,
    val timeMs: Long,
    val percentage: Float
)

data class PeriodUsageSummary(
    val period: ReportingPeriod,
    val startDateStr: String,
    val endDateStr: String,
    val totalTimeMs: Long,
    val prevPeriodTotalTimeMs: Long,
    val appCount: Int,
    val mostUsedApp: AppUsageStat?,
    val dailyAverageMs: Long,
    val peakHour: Int, // 0..23
    val appStats: List<AppUsageStat>,
    val categoryStats: List<CategoryUsageStat>,
    val chartType: ChartType,
    val chartBars: List<ChartBarData>,
    val isHistoryPartial: Boolean = false
)

data class AppDetailAnalytics(
    val packageName: String,
    val appName: String,
    val icon: ImageBitmap?,
    val categoryName: String,
    val period: ReportingPeriod,
    val totalTimeMs: Long,
    val prevPeriodTotalTimeMs: Long,
    val estimatedSessions: Int,
    val firstRecordedTimeMs: Long,
    val lastRecordedTimeMs: Long,
    val hourlyDistribution: List<Long>, // 24 entries
    val chartType: ChartType,
    val chartBars: List<ChartBarData>
)
