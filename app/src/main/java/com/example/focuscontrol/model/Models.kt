package com.example.focuscontrol.model

import androidx.compose.ui.graphics.ImageBitmap
import java.util.Calendar

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: ImageBitmap?,
)

data class StudyCycle(
    val studyDurationMinutes: Int,
    val breakDurationMinutes: Int
)

data class StudySchedule(
    val id: String,
    val name: String,
    val startTime: String,
    val studyDurationMinutes: Int,
    val breakDurationMinutes: Int,
    val weekdays: List<String>,
    val isEnabled: Boolean,
    val blockedAppCount: Int,
    val cycles: List<StudyCycle> = emptyList()
)

data class AppAllowanceRule(
    val packageName: String,
    val appName: String,
    val intervalMinutes: Int,
    val allowedMinutes: Int,
    val remainingMinutes: Int,
    val timeToResetText: String,
    val isEnabled: Boolean,
    val intervalStartTimeMs: Long = 0L,
    val usedForegroundTimeMs: Long = 0L,
    val originalIntervalMinutes: Int = intervalMinutes,
    val originalAllowedMinutes: Int = allowedMinutes,
    val studyModePausedTimeMs: Long = 0L // To track when study mode started pausing this rule
) {
    fun isRuleActive(currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        if (!isEnabled) return false
        val intervalMs = intervalMinutes * 60 * 1000L
        val effectiveStart = if (intervalStartTimeMs == 0L) getFixedIntervalStart(currentTimeMs, intervalMinutes) else intervalStartTimeMs
        val isExpired = (currentTimeMs - effectiveStart) >= intervalMs
        return !isExpired && usedForegroundTimeMs > 0L
    }

    fun canBeWeakenedBy(newAllowedMinutes: Int, newIntervalMinutes: Int, newIsEnabled: Boolean, currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        if (!isRuleActive(currentTimeMs)) return true // Inactive/expired rule can be edited freely
        // If active, loosening edits are prevented during active interval:
        if (!newIsEnabled) return false // Cannot disable active rule
        if (newAllowedMinutes > allowedMinutes) return false // Cannot increase allowance minutes
        if (newIntervalMinutes < intervalMinutes) return false // Cannot shorten reset interval
        return true
    }
}

fun getFixedIntervalStart(currentTime: Long, intervalMinutes: Int): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = currentTime
    calendar[Calendar.HOUR_OF_DAY] = 0
    calendar[Calendar.MINUTE] = 0
    calendar[Calendar.SECOND] = 0
    calendar[Calendar.MILLISECOND] = 0
    val midnight = calendar.timeInMillis
    val intervalMs = intervalMinutes * 60 * 1000L
    if (intervalMs == 0L) return currentTime
    val elapsed = maxOf(0L, currentTime - midnight)
    return midnight + ((elapsed / intervalMs) * intervalMs)
}
