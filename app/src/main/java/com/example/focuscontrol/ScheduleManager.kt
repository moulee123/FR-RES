package com.example.focuscontrol

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.focuscontrol.model.StudySchedule
import java.util.Calendar

object ScheduleManager {
    fun rescheduleAll(context: Context, schedules: List<StudySchedule>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, ScheduleReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        var earliestNextTransitionMs = Long.MAX_VALUE
        val now = Calendar.getInstance()
        val currentMs = now.timeInMillis
        val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)

        for (schedule in schedules.filter { it.isEnabled }) {
            val parts = schedule.startTime.split(":")
            if (parts.size != 2) continue
            val startHour = parts[0].toIntOrNull() ?: continue
            val startMinute = parts[1].toIntOrNull() ?: continue

            for (dayStr in schedule.weekdays) {
                val targetDayOfWeek = when (dayStr) {
                    "Sun" -> Calendar.SUNDAY
                    "Mon" -> Calendar.MONDAY
                    "Tue" -> Calendar.TUESDAY
                    "Wed" -> Calendar.WEDNESDAY
                    "Thu" -> Calendar.THURSDAY
                    "Fri" -> Calendar.FRIDAY
                    "Sat" -> Calendar.SATURDAY
                    else -> -1
                }
                if (targetDayOfWeek == -1) continue

                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, startHour)
                cal.set(Calendar.MINUTE, startMinute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                var daysDiff = targetDayOfWeek - currentDayOfWeek
                if (daysDiff < 0) daysDiff += 7
                cal.add(Calendar.DAY_OF_YEAR, daysDiff)

                val baseStartMs = cal.timeInMillis

                for (weekOffset in 0..1) {
                    val startMs = baseStartMs + weekOffset * 7 * 24 * 60 * 60 * 1000L
                    var offsetMs = 0L

                    for (cycle in schedule.cycles) {
                        val studyStartMs = startMs + offsetMs
                        val studyEndMs = studyStartMs + cycle.studyDurationMinutes * 60 * 1000L
                        val warningMs = studyEndMs - 60_000L // 1 minute warning
                        val breakEndMs = studyEndMs + cycle.breakDurationMinutes * 60 * 1000L

                        val timestamps = listOf(studyStartMs, warningMs, studyEndMs, breakEndMs)
                        for (ts in timestamps) {
                            if (ts > currentMs && ts < earliestNextTransitionMs) {
                                earliestNextTransitionMs = ts
                            }
                        }

                        offsetMs += (cycle.studyDurationMinutes + cycle.breakDurationMinutes) * 60 * 1000L
                    }
                }
            }
        }

        if (earliestNextTransitionMs != Long.MAX_VALUE) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            earliestNextTransitionMs,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            earliestNextTransitionMs,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        earliestNextTransitionMs,
                        pendingIntent
                    )
                }
            } catch (_: SecurityException) {
                // Fallback if permission denied
            }
        }
    }

    fun scheduleTravelModeCheck(context: Context, triggerTimeMs: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ScheduleReceiver.ACTION_CHECK_TRAVEL_MODE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 5001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        if (triggerTimeMs > System.currentTimeMillis()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                }
            } catch (_: SecurityException) {
                // Fallback if permission denied
            }
        }
    }
}
