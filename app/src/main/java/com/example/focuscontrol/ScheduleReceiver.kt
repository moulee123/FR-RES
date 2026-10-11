package com.example.focuscontrol

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.focuscontrol.model.StudySchedule
import java.util.Calendar

data class ActiveScheduleInfo(
    val isStudyActive: Boolean,
    val isBreakActive: Boolean,
    val currentStudyEndMs: Long,
    val currentBreakDurationMins: Int,
    val blockedAppCount: Int
)

class ScheduleReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "FRERES_BREAK_DEBUG"
        const val ACTION_EXTEND_STUDY = "com.example.focuscontrol.ACTION_EXTEND_STUDY"
        const val ACTION_START_BREAK = "com.example.focuscontrol.ACTION_START_BREAK"
        const val ACTION_CHECK_TRAVEL_MODE = "com.example.focuscontrol.ACTION_CHECK_TRAVEL_MODE"
        const val EXTRA_BREAK_MINS = "extra_break_mins"

        fun evaluateActiveScheduleInfo(context: Context, schedules: List<StudySchedule>, nowMs: Long): ActiveScheduleInfo {
            var isStudy = false
            var isBreak = false
            var studyEndMs = 0L
            var breakMins = 5
            var blockedCount = 0

            for (schedule in schedules.filter { it.isEnabled }) {
                val parts = schedule.startTime.split(":")
                if (parts.size != 2) continue
                val startHour = parts[0].toIntOrNull() ?: continue
                val startMin = parts[1].toIntOrNull() ?: continue

                for (dayOffset in -1..1) {
                    val checkCal = Calendar.getInstance().apply {
                        timeInMillis = nowMs
                        add(Calendar.DAY_OF_YEAR, dayOffset)
                    }
                    val dayOfWeek = checkCal.get(Calendar.DAY_OF_WEEK)
                    val dayStr = when (dayOfWeek) {
                        Calendar.SUNDAY -> "Sun"
                        Calendar.MONDAY -> "Mon"
                        Calendar.TUESDAY -> "Tue"
                        Calendar.WEDNESDAY -> "Wed"
                        Calendar.THURSDAY -> "Thu"
                        Calendar.FRIDAY -> "Fri"
                        Calendar.SATURDAY -> "Sat"
                        else -> ""
                    }

                    if (!schedule.weekdays.contains(dayStr)) continue

                    checkCal.set(Calendar.HOUR_OF_DAY, startHour)
                    checkCal.set(Calendar.MINUTE, startMin)
                    checkCal.set(Calendar.SECOND, 0)
                    checkCal.set(Calendar.MILLISECOND, 0)

                    val scheduleStartMs = checkCal.timeInMillis
                    var offsetMs = 0L

                    for (cycle in schedule.cycles) {
                        val cycleStudyStartMs = scheduleStartMs + offsetMs
                        val cycleStudyEndMs = cycleStudyStartMs + cycle.studyDurationMinutes * 60 * 1000L
                        val cycleBreakEndMs = cycleStudyEndMs + cycle.breakDurationMinutes * 60 * 1000L

                        if (nowMs in cycleStudyStartMs until cycleStudyEndMs) {
                            isStudy = true
                            isBreak = false
                            studyEndMs = cycleStudyEndMs
                            breakMins = cycle.breakDurationMinutes
                            blockedCount = schedule.blockedAppCount
                        } else if (nowMs in cycleStudyEndMs until cycleBreakEndMs) {
                            isStudy = true
                            isBreak = true
                            studyEndMs = cycleStudyEndMs
                            breakMins = cycle.breakDurationMinutes
                            blockedCount = schedule.blockedAppCount
                        }

                        offsetMs += (cycle.studyDurationMinutes + cycle.breakDurationMinutes) * 60 * 1000L
                    }
                }
            }

            return ActiveScheduleInfo(
                isStudyActive = isStudy,
                isBreakActive = isBreak,
                currentStudyEndMs = studyEndMs,
                currentBreakDurationMins = breakMins,
                blockedAppCount = blockedCount
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = FocusPreferences(context)
        val action = intent.action
        Log.d(TAG, "ScheduleReceiver onReceive() - Action: $action, Time: ${System.currentTimeMillis()}")

        if (action == ACTION_EXTEND_STUDY) {
            Log.d(TAG, "ScheduleReceiver: Handling ACTION_EXTEND_STUDY (+5m Extension)")
            prefs.addStudyExtension(5)
            ScheduleManager.rescheduleAll(context, prefs.studySchedules)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(2002)
            return
        }

        if (action == ACTION_START_BREAK) {
            val breakMins = intent.getIntExtra(EXTRA_BREAK_MINS, 5)
            Log.d(TAG, "ScheduleReceiver: Handling ACTION_START_BREAK ($breakMins mins)")
            prefs.startStudyModeBreak(breakMins)
            ScheduleManager.rescheduleAll(context, prefs.studySchedules)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(2002)
            return
        }

        val now = System.currentTimeMillis()

        // Handle Boot Completed / System Restart with 5-minute post-boot grace period
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d(TAG, "ScheduleReceiver: Boot/Package Replaced event received. Evaluating post-boot grace period.")
            if (prefs.isTravelModeEnabled) {
                if (prefs.isTravelConfirmationOverdue()) {
                    // Apply 5-minute post-boot grace period before issuing overdue alert
                    Log.d(TAG, "ScheduleReceiver: Travel Mode confirmation overdue post-boot. Applying 5-minute grace period.")
                    ScheduleManager.scheduleTravelModeCheck(context, now + 5 * 60 * 1000L)
                } else {
                    val timeSinceConfirm = now - prefs.lastTravelConfirmationTimeMs
                    val remainingValidityMs = maxOf(60_000L, (2 * 60 * 60 * 1000L) - timeSinceConfirm)
                    ScheduleManager.scheduleTravelModeCheck(context, now + remainingValidityMs)
                }
            }
        }

        // Handle Travel Mode 2-Hour Confirmation Check Alarm
        if (action == ACTION_CHECK_TRAVEL_MODE) {
            val isOverdue = prefs.isTravelConfirmationOverdue()
            Log.d(TAG, "ScheduleReceiver: Travel Mode Check Alarm fired. Overdue: $isOverdue")

            if (isOverdue && prefs.isTravelModeEnabled) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val alertIntent = Intent(context, MainActivity::class.java)
                val alertPendingIntent = PendingIntent.getActivity(
                    context, 5002, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                val alertBuilder = NotificationCompat.Builder(context, "freres_break_reminder_v2")
                    .setSmallIcon(R.drawable.ic_dialog_alert)
                    .setContentTitle("Travel Mode Confirmation Overdue 📍")
                    .setContentText("Your 2-hour location verification for Travel Mode has expired. Tap to verify.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setSound(defaultSoundUri)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(alertPendingIntent)

                notificationManager.notify(5003, alertBuilder.build())
                Log.d(TAG, "ScheduleReceiver: Posted Travel Mode Overdue alert notification 5003")
            }
            return
        }
        val scheduleInfo = evaluateActiveScheduleInfo(context, prefs.studySchedules, now)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "freres_break_reminder_v2"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val generalChannel = NotificationChannel(
                "focus_channel",
                "Focus Schedule Transitions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(generalChannel)

            val breakReminderChannel = NotificationChannel(
                channelId,
                "Break Reminders & Full Screen Displays",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(breakReminderChannel)
        }

        val activityIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (scheduleInfo.isStudyActive) {
            prefs.isStudyModeEnabled = true
            FocusBlockerService.triggerImmediateCheck()

            val timeRemainingMs = scheduleInfo.currentStudyEndMs - now
            val userHasNotStartedBreakYet = prefs.studyModeBreakEndTimeMs < now
            val isAtBreakTransition = timeRemainingMs in -180_000L..120_000L

            if (isAtBreakTransition && userHasNotStartedBreakYet) {
                Log.d(TAG, "ScheduleReceiver: Study-end transition entered! Time remaining: ${timeRemainingMs}ms")
                prefs.lastBreakNotificationTimeMs = now

                // 1. Physically wake up device screen using PowerManager WakeLock
                try {
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    @Suppress("DEPRECATION")
                    val wakeLock = powerManager.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                        "FRERES:ScreenWakeLock"
                    )
                    wakeLock.acquire(5000L)
                    Log.d(TAG, "ScheduleReceiver: Acquired 5s WakeLock")
                } catch (e: Exception) {
                    Log.e(TAG, "ScheduleReceiver: WakeLock failed", e)
                }

                // 2. Trigger Lock Screen / Notification Break Reminder
                val lockScreenIntent = Intent(context, ReadOnlyLockScreenActivity::class.java).apply {
                    putExtra(ReadOnlyLockScreenActivity.EXTRA_STUDY_END_MS, scheduleInfo.currentStudyEndMs)
                    putExtra(ReadOnlyLockScreenActivity.EXTRA_BLOCKED_COUNT, scheduleInfo.blockedAppCount)
                    putExtra(ReadOnlyLockScreenActivity.EXTRA_BREAK_MINS, scheduleInfo.currentBreakDurationMins)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                val lockScreenPendingIntent = PendingIntent.getActivity(
                    context, 2001, lockScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val extendIntent = Intent(context, ScheduleReceiver::class.java).apply {
                    this.action = ACTION_EXTEND_STUDY
                }
                val extendPendingIntent = PendingIntent.getBroadcast(
                    context, 3001, extendIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val breakIntent = Intent(context, ScheduleReceiver::class.java).apply {
                    this.action = ACTION_START_BREAK
                    putExtra(EXTRA_BREAK_MINS, scheduleInfo.currentBreakDurationMins)
                }
                val breakPendingIntent = PendingIntent.getBroadcast(
                    context, 3002, breakIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_dialog_alert)
                    .setContentTitle("Study Interval Ended • Break Due")
                    .setContentText("Tap an action below or unlock to choose.")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .addAction(0, "+5m Study Extension", extendPendingIntent)
                    .addAction(0, "Start My Break", breakPendingIntent)

                val canUseFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    notificationManager.canUseFullScreenIntent()
                } else {
                    true
                }
                Log.d(TAG, "ScheduleReceiver: fullScreenIntent allowed? $canUseFullScreen, Channel: $channelId, Importance: ${NotificationManager.IMPORTANCE_HIGH}")

                if (canUseFullScreen) {
                    prefs.lastFullScreenAttemptTimeMs = now
                    builder.setFullScreenIntent(lockScreenPendingIntent, true)
                    Log.d(TAG, "ScheduleReceiver: Attached fullScreenIntent targeting ReadOnlyLockScreenActivity")
                }

                // If "Appear on Top" (SYSTEM_ALERT_WINDOW) is granted, also launch Activity directly over active app
                if (Settings.canDrawOverlays(context)) {
                    try {
                        context.startActivity(lockScreenIntent)
                        Log.d(TAG, "ScheduleReceiver: Direct startActivity succeeded using SYSTEM_ALERT_WINDOW permission!")
                    } catch (e: Exception) {
                        Log.e(TAG, "ScheduleReceiver: Direct startActivity failed", e)
                    }
                }

                notificationManager.notify(2002, builder.build())
                Log.d(TAG, "ScheduleReceiver: Break notification ID 2002 posted successfully!")
            } else if (scheduleInfo.isBreakActive && !userHasNotStartedBreakYet) {
                val builder = NotificationCompat.Builder(context, "focus_channel")
                    .setSmallIcon(R.drawable.ic_dialog_info)
                    .setContentTitle("Focus Break Started")
                    .setContentText("Your scheduled break has begun. Apps are accessible.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                notificationManager.notify(1001, builder.build())
                notificationManager.cancel(2002)
            } else {
                prefs.endStudyModeBreak()
                val builder = NotificationCompat.Builder(context, "focus_channel")
                    .setSmallIcon(R.drawable.ic_dialog_alert)
                    .setContentTitle("Study Session Started")
                    .setContentText("Focus apps are now restricted.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                notificationManager.notify(1001, builder.build())
            }
        } else {
            // Schedule ended
            if (prefs.isStudyModeEnabled) {
                prefs.isStudyModeEnabled = false
                val builder = NotificationCompat.Builder(context, "focus_channel")
                    .setSmallIcon(R.drawable.ic_dialog_info)
                    .setContentTitle("Study Session Ended")
                    .setContentText("All apps are now accessible.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                notificationManager.notify(1001, builder.build())
            }
            notificationManager.cancel(2002)
        }

        ScheduleManager.rescheduleAll(context, prefs.studySchedules)
        Log.d(TAG, "ScheduleReceiver: Rescheduled next alarm successfully!")
    }
}
