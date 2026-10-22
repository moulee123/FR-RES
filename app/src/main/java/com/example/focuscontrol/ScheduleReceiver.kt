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
import com.example.focuscontrol.utils.LocationVerificationHelper
import android.location.Location
import java.util.Calendar
import java.util.Locale

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
        const val ACTION_START_STUDYING = "com.example.focuscontrol.ACTION_START_STUDYING"
        const val ACTION_END_STUDY_MODE = "com.example.focuscontrol.ACTION_END_STUDY_MODE"
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

        if (action == ACTION_START_STUDYING) {
            Log.d(TAG, "ScheduleReceiver: Handling ACTION_START_STUDYING")
            prefs.endStudyModeBreak()
            prefs.isStudyModeEnabled = true
            ScheduleManager.rescheduleAll(context, prefs.studySchedules)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(2002)
            return
        }

        if (action == ACTION_END_STUDY_MODE) {
            Log.d(TAG, "ScheduleReceiver: Handling ACTION_END_STUDY_MODE")
            prefs.endStudyModeBreak()
            prefs.isStudyModeEnabled = false
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
                    Log.d(TAG, "ScheduleReceiver: Travel Mode confirmation overdue post-boot. Applying 5-minute grace period.")
                    ScheduleManager.scheduleTravelModeCheck(context, now + 5 * 60 * 1000L)
                } else {
                    val timeSinceConfirm = now - prefs.lastTravelConfirmationTimeMs
                    val remainingValidityMs = maxOf(60_000L, (2 * 60 * 60 * 1000L) - timeSinceConfirm)
                    ScheduleManager.scheduleTravelModeCheck(context, now + remainingValidityMs)
                }
            }
        }

        // Handle Travel Mode 2-Hour Confirmation Check Alarm (Part 4 & Part 5)
        if (action == ACTION_CHECK_TRAVEL_MODE) {
            if (!prefs.isTravelModeActive() || !prefs.isHomeVerified) {
                return
            }

            Log.d(TAG, "ScheduleReceiver: Travel Mode Check Alarm fired. Executing background location verification.")

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (LocationVerificationHelper.isVpnActive(context)) {
                val alertIntent = Intent(context, MainActivity::class.java)
                val alertPendingIntent = PendingIntent.getActivity(
                    context, 5002, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val alertBuilder = NotificationCompat.Builder(context, "freres_break_reminder_v2")
                    .setSmallIcon(R.drawable.ic_dialog_alert)
                    .setContentTitle("VPN Detected 📍")
                    .setContentText("VPN detected. Turn off your VPN and verify your current device location to continue.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(alertPendingIntent)

                notificationManager.notify(5003, alertBuilder.build())
                ScheduleManager.scheduleTravelModeCheck(context, now + 15 * 60 * 1000L)
                return
            }

            LocationVerificationHelper.getCurrentLocationFix(context) { location: Location?, statusMsg: String ->
                if (location == null) {
                    val alertIntent = Intent(context, MainActivity::class.java)
                    val alertPendingIntent = PendingIntent.getActivity(
                        context, 5002, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val alertBuilder = NotificationCompat.Builder(context, "freres_break_reminder_v2")
                        .setSmallIcon(R.drawable.ic_dialog_alert)
                        .setContentTitle("Travel Check Needed 📍")
                        .setContentText("Open FRÈRES to complete location verification ($statusMsg).")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(alertPendingIntent)

                    notificationManager.notify(5003, alertBuilder.build())
                    ScheduleManager.scheduleTravelModeCheck(context, now + 15 * 60 * 1000L)
                    return@getCurrentLocationFix
                }

                if (LocationVerificationHelper.isLocationMocked(location)) {
                    val alertIntent = Intent(context, MainActivity::class.java)
                    val alertPendingIntent = PendingIntent.getActivity(
                        context, 5002, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val alertBuilder = NotificationCompat.Builder(context, "freres_break_reminder_v2")
                        .setSmallIcon(R.drawable.ic_dialog_alert)
                        .setContentTitle("Mocked Location Detected 📍")
                        .setContentText("Mocked location detected. Please disable location spoofing apps to proceed.")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(alertPendingIntent)

                    notificationManager.notify(5003, alertBuilder.build())
                    return@getCurrentLocationFix
                }

                val distanceMeters = LocationVerificationHelper.calculateDistanceMeters(
                    location.latitude, location.longitude,
                    prefs.homeLatitude, prefs.homeLongitude
                )

                if (distanceMeters <= LocationVerificationHelper.HOME_ARRIVAL_RADIUS_METERS) {
                    // Part 4: Home Arrival Detected!
                    prefs.stopTravelMode()
                    ScheduleManager.rescheduleAll(context, prefs.studySchedules)

                    val alertIntent = Intent(context, MainActivity::class.java)
                    val alertPendingIntent = PendingIntent.getActivity(
                        context, 5002, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val alertBuilder = NotificationCompat.Builder(context, "freres_break_reminder_v2")
                        .setSmallIcon(R.drawable.ic_dialog_alert)
                        .setContentTitle("Welcome Home! 🏠")
                        .setContentText("You are within 200m of home. Travel Mode has automatically ended and normal rules are restored.")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(alertPendingIntent)

                    notificationManager.notify(5003, alertBuilder.build())
                    Log.d(TAG, "ScheduleReceiver: Reached home ($distanceMeters m <= 200m). Travel Mode ended automatically.")
                } else {
                    // Part 4: Still Away From Home!
                    val distKm = distanceMeters / 1000.0
                    val placeName = LocationVerificationHelper.getGeocodedAddressName(context, location.latitude, location.longitude)
                    val locDesc = "$placeName (${String.format(Locale.getDefault(), "%.1f", distKm)} km from home)"

                    prefs.lastTravelLocationName = locDesc
                    prefs.lastTravelConfirmationTimeMs = System.currentTimeMillis()
                    prefs.nextLocationCheckTimeMs = System.currentTimeMillis() + 2 * 60 * 60 * 1000L

                    ScheduleManager.scheduleTravelModeCheck(context, prefs.nextLocationCheckTimeMs)

                    val alertIntent = Intent(context, MainActivity::class.java)
                    val alertPendingIntent = PendingIntent.getActivity(
                        context, 5002, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val alertBuilder = NotificationCompat.Builder(context, "freres_break_reminder_v2")
                        .setSmallIcon(R.drawable.ic_dialog_alert)
                        .setContentTitle("Travel Location Verified 📍")
                        .setContentText("Verified ${String.format(Locale.getDefault(), "%.1f", distKm)} km from home. ${prefs.activeTravelMode} Mode remains active.")
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setAutoCancel(true)
                        .setContentIntent(alertPendingIntent)

                    notificationManager.notify(5003, alertBuilder.build())
                    Log.d(TAG, "ScheduleReceiver: Periodic location check verified $distKm km from home. Next check scheduled in 2 hours.")
                }
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

        val breakJustEnded = prefs.studyModeBreakEndTimeMs > 0L && now >= prefs.studyModeBreakEndTimeMs
        if (breakJustEnded) {
            Log.d(TAG, "ScheduleReceiver: Break timer expired! Clearing break state.")
            prefs.endStudyModeBreak()
        }

        if (scheduleInfo.isStudyActive) {
            prefs.isStudyModeEnabled = true
            FocusBlockerService.triggerImmediateCheck()

            val timeRemainingMs = scheduleInfo.currentStudyEndMs - now
            val userHasNotStartedBreakYet = prefs.studyModeBreakEndTimeMs < now
            val isAtBreakTransition = timeRemainingMs in -180_000L..120_000L

            val isStudyEndedState = isAtBreakTransition && userHasNotStartedBreakYet
            val isBreakEndedState = breakJustEnded

            if (isStudyEndedState || isBreakEndedState) {
                val screenType = if (isBreakEndedState) {
                    ReadOnlyLockScreenActivity.SCREEN_TYPE_BREAK_ENDED
                } else {
                    ReadOnlyLockScreenActivity.SCREEN_TYPE_STUDY_ENDED
                }

                Log.d(TAG, "ScheduleReceiver: Reminder transition entered! ScreenType: $screenType")
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
                    putExtra(ReadOnlyLockScreenActivity.EXTRA_SCREEN_TYPE, screenType)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                val lockScreenPendingIntent = PendingIntent.getActivity(
                    context, 2001, lockScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_dialog_alert)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

                if (isBreakEndedState) {
                    val startStudyingIntent = Intent(context, ScheduleReceiver::class.java).apply {
                        this.action = ACTION_START_STUDYING
                    }
                    val startStudyingPendingIntent = PendingIntent.getBroadcast(
                        context, 3003, startStudyingIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val endStudyIntent = Intent(context, ScheduleReceiver::class.java).apply {
                        this.action = ACTION_END_STUDY_MODE
                    }
                    val endStudyPendingIntent = PendingIntent.getBroadcast(
                        context, 3004, endStudyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    builder.setContentTitle("Break Time Ended • Ready to Focus?")
                        .setContentText("A short break keeps you focused for longer.")
                        .addAction(0, "Start Studying", startStudyingPendingIntent)
                        .addAction(0, "End Study Mode", endStudyPendingIntent)
                } else {
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

                    builder.setContentTitle("Study Interval Ended • Break Due")
                        .setContentText("A short break keeps you focused for longer.")
                        .addAction(0, "+5m Study Extension", extendPendingIntent)
                        .addAction(0, "Start My Break", breakPendingIntent)
                }

                val canUseFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    notificationManager.canUseFullScreenIntent()
                } else {
                    true
                }

                if (canUseFullScreen) {
                    prefs.lastFullScreenAttemptTimeMs = now
                    builder.setFullScreenIntent(lockScreenPendingIntent, true)
                    Log.d(TAG, "ScheduleReceiver: Attached fullScreenIntent targeting ReadOnlyLockScreenActivity ($screenType)")
                }

                if (Settings.canDrawOverlays(context)) {
                    try {
                        context.startActivity(lockScreenIntent)
                        Log.d(TAG, "ScheduleReceiver: Direct startActivity succeeded using SYSTEM_ALERT_WINDOW permission!")
                    } catch (e: Exception) {
                        Log.e(TAG, "ScheduleReceiver: Direct startActivity failed", e)
                    }
                }

                notificationManager.notify(2002, builder.build())
                Log.d(TAG, "ScheduleReceiver: Reminder notification ID 2002 posted successfully!")
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
