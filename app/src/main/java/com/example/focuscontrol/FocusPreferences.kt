package com.example.focuscontrol

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.focuscontrol.model.AppAllowanceRule
import com.example.focuscontrol.model.StudyCycle
import com.example.focuscontrol.model.StudySchedule
import org.json.JSONArray
import org.json.JSONObject

class FocusPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_control_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_STUDY_MODE_ENABLED = "study_mode_enabled"
        private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
        private const val KEY_STUDY_BREAK_END_TIME = "study_break_end_time"
        private const val KEY_STUDY_EXTENSION_END_TIME = "study_extension_end_time"

        private const val KEY_DAILY_LIMITS_ENABLED = "daily_limits_enabled"
    }

    var isStudyModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_STUDY_MODE_ENABLED, false)
        set(value) {
            prefs.edit { putBoolean(KEY_STUDY_MODE_ENABLED, value) }
            updateProject2PauseState(value)
        }

    var blockedPackageNames: Set<String>
        get() = prefs.getStringSet(KEY_BLOCKED_PACKAGES, emptySet())?.toSet() ?: emptySet()
        set(value) = prefs.edit { putStringSet(KEY_BLOCKED_PACKAGES, HashSet(value)) }

    fun isAppBlocked(packageName: String): Boolean {
        if (packageName == "com.example.focuscontrol") return false
        return blockedPackageNames.contains(packageName)
    }

    fun setAppBlocked(packageName: String, isBlocked: Boolean) {
        val current = blockedPackageNames.toMutableSet()
        if (isBlocked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        blockedPackageNames = current
    }

    // Study Mode Breaks (Project 1)
    var studyModeBreakEndTimeMs: Long
        get() = prefs.getLong(KEY_STUDY_BREAK_END_TIME, 0L)
        set(value) = prefs.edit { putLong(KEY_STUDY_BREAK_END_TIME, value) }

    fun isStudyModeOnBreak(): Boolean {
        val endTime = studyModeBreakEndTimeMs
        return endTime > System.currentTimeMillis()
    }

    fun startStudyModeBreak(durationMinutes: Int = 5) {
        studyModeBreakEndTimeMs = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
    }

    fun endStudyModeBreak() {
        studyModeBreakEndTimeMs = 0L
    }

    var studyExtensionEndTimeMs: Long
        get() = prefs.getLong(KEY_STUDY_EXTENSION_END_TIME, 0L)
        set(value) = prefs.edit { putLong(KEY_STUDY_EXTENSION_END_TIME, value) }

    fun isStudyExtensionActive(): Boolean {
        return studyExtensionEndTimeMs > System.currentTimeMillis()
    }

    fun addStudyExtension(durationMinutes: Int = 5) {
        val now = System.currentTimeMillis()
        val currentEnd = if (studyExtensionEndTimeMs > now) studyExtensionEndTimeMs else now
        studyExtensionEndTimeMs = currentEnd + (durationMinutes * 60 * 1000L)
    }

    // Diagnostics Telemetry Tracking
    var lastBreakNotificationTimeMs: Long
        get() = prefs.getLong("last_break_notif_time", 0L)
        set(value) = prefs.edit { putLong("last_break_notif_time", value) }

    var lastFullScreenAttemptTimeMs: Long
        get() = prefs.getLong("last_fullscreen_attempt_time", 0L)
        set(value) = prefs.edit { putLong("last_fullscreen_attempt_time", value) }

    var lastLockScreenActivityLaunchTimeMs: Long
        get() = prefs.getLong("last_lockscreen_activity_launch_time", 0L)
        set(value) = prefs.edit { putLong("last_lockscreen_activity_launch_time", value) }

    // Home Location Preferences
    var isHomeVerified: Boolean
        get() = isHomeLocationSet || prefs.getBoolean("home_location_verified", false)
        set(value) = prefs.edit { putBoolean("home_location_verified", value) }

    // Permanent Home Location Preferences (Part 1)
    val isHomeLocationSet: Boolean
        get() = prefs.getBoolean("is_home_location_set", false) || prefs.getBoolean("home_location_verified", false)

    var homeLatitude: Double
        get() = Double.fromBits(prefs.getLong("home_latitude_bits", 0L))
        set(value) = prefs.edit { putLong("home_latitude_bits", value.toBits()) }

    var homeLongitude: Double
        get() = Double.fromBits(prefs.getLong("home_longitude_bits", 0L))
        set(value) = prefs.edit { putLong("home_longitude_bits", value.toBits()) }

    var homeLocationName: String
        get() = prefs.getString("home_location_name", "Not Set") ?: "Not Set"
        set(value) = prefs.edit { putString("home_location_name", value) }

    var homeVerificationTimestamp: Long
        get() = prefs.getLong("home_verify_timestamp", 0L)
        set(value) = prefs.edit { putLong("home_verify_timestamp", value) }

    fun setHomeLocation(lat: Double, lng: Double, name: String) {
        if (isHomeLocationSet) return // Rule 4: Once saved, home location must not be editable or replaced
        prefs.edit {
            putBoolean("is_home_location_set", true)
            putBoolean("home_location_verified", true)
            putLong("home_latitude_bits", lat.toBits())
            putLong("home_longitude_bits", lng.toBits())
            putString("home_location_name", name)
            putLong("home_verify_timestamp", System.currentTimeMillis())
        }
    }

    // Travel Mode Preferences (Parts 2-6)
    // activeTravelMode values: "NONE", "LOCAL", "REGULAR"
    var activeTravelMode: String
        get() = prefs.getString("active_travel_mode", "NONE") ?: "NONE"
        set(value) {
            prefs.edit {
                putString("active_travel_mode", value)
                putBoolean("travel_mode_enabled", value != "NONE")
            }
        }

    var isTravelModeEnabled: Boolean
        get() = activeTravelMode != "NONE" || prefs.getBoolean("travel_mode_enabled", false)
        set(value) {
            if (!value) {
                activeTravelMode = "NONE"
            }
            prefs.edit { putBoolean("travel_mode_enabled", value) }
        }

    var travelModeStartTimeMs: Long
        get() = prefs.getLong("travel_mode_start_time_ms", 0L)
        set(value) = prefs.edit { putLong("travel_mode_start_time_ms", value) }

    var nextLocationCheckTimeMs: Long
        get() = prefs.getLong("next_location_check_time_ms", 0L)
        set(value) = prefs.edit { putLong("next_location_check_time_ms", value) }

    var lastTravelConfirmationTimeMs: Long
        get() = prefs.getLong("last_travel_confirm_time", 0L)
        set(value) = prefs.edit { putLong("last_travel_confirm_time", value) }

    var lastTravelLocationName: String
        get() = prefs.getString("last_travel_location_name", "Not Verified") ?: "Not Verified"
        set(value) = prefs.edit { putString("last_travel_location_name", value) }

    fun isTravelModeActive(): Boolean = activeTravelMode == "LOCAL" || activeTravelMode == "REGULAR" || isTravelModeEnabled
    fun isLocalTravelModeActive(): Boolean = activeTravelMode == "LOCAL"
    fun isRegularTravelModeActive(): Boolean = activeTravelMode == "REGULAR"

    fun stopTravelMode() {
        activeTravelMode = "NONE"
        isTravelModeEnabled = false
        travelModeStartTimeMs = 0L
        nextLocationCheckTimeMs = 0L
        lastTravelConfirmationTimeMs = 0L
        lastTravelLocationName = "Not Verified"
    }

    fun isTravelModeConfirmed(): Boolean {
        if (!isTravelModeActive() || !isHomeVerified) return false
        val confirmTime = lastTravelConfirmationTimeMs
        if (confirmTime == 0L) return false
        val timeSinceConfirm = System.currentTimeMillis() - confirmTime
        return timeSinceConfirm < 2 * 60 * 60 * 1000L // 2-hour validity window
    }

    fun isTravelConfirmationOverdue(): Boolean {
        if (!isTravelModeActive() || !isHomeVerified) return false
        val confirmTime = lastTravelConfirmationTimeMs
        if (confirmTime == 0L) return true
        val timeSinceConfirm = System.currentTimeMillis() - confirmTime
        return timeSinceConfirm >= 2 * 60 * 60 * 1000L
    }

    // Project 2 Daily Limits settings (Separated from Study Mode)
    var isDailyLimitsEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_LIMITS_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_DAILY_LIMITS_ENABLED, value) }

    private val KEY_ALLOWANCE_RULES = "allowance_rules"

    var allowanceRules: List<AppAllowanceRule>
        get() {
            val jsonString = prefs.getString(KEY_ALLOWANCE_RULES, "[]") ?: "[]"
            return try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<AppAllowanceRule>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val legacyIntervalHours = obj.optInt("intervalHours", -1)
                    val finalIntervalMinutes = if (obj.has("intervalMinutes")) obj.getInt("intervalMinutes") else (legacyIntervalHours * 60).coerceAtLeast(60)
                    
                    list.add(
                        AppAllowanceRule(
                            packageName = obj.getString("packageName"),
                            appName = obj.getString("appName"),
                            intervalMinutes = finalIntervalMinutes,
                            allowedMinutes = obj.getInt("allowedMinutes"),
                            remainingMinutes = obj.optInt("remainingMinutes", obj.getInt("allowedMinutes")),
                            timeToResetText = obj.optString("timeToResetText", ""),
                            isEnabled = obj.optBoolean("isEnabled", true),
                            intervalStartTimeMs = obj.optLong("intervalStartTimeMs", 0L),
                            usedForegroundTimeMs = obj.optLong("usedForegroundTimeMs", 0L),
                            originalIntervalMinutes = obj.optInt("originalIntervalMinutes", finalIntervalMinutes),
                            originalAllowedMinutes = obj.optInt("originalAllowedMinutes", obj.getInt("allowedMinutes")),
                            studyModePausedTimeMs = obj.optLong("studyModePausedTimeMs", 0L)
                        )
                    )
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
        set(value) {
            val array = JSONArray()
            for (rule in value) {
                val obj = JSONObject()
                obj.put("packageName", rule.packageName)
                obj.put("appName", rule.appName)
                obj.put("intervalMinutes", rule.intervalMinutes)
                obj.put("intervalHours", rule.intervalMinutes / 60) // Legacy support
                obj.put("allowedMinutes", rule.allowedMinutes)
                obj.put("remainingMinutes", rule.remainingMinutes)
                obj.put("timeToResetText", rule.timeToResetText)
                obj.put("isEnabled", rule.isEnabled)
                obj.put("intervalStartTimeMs", rule.intervalStartTimeMs)
                obj.put("usedForegroundTimeMs", rule.usedForegroundTimeMs)
                obj.put("originalIntervalMinutes", rule.originalIntervalMinutes)
                obj.put("originalAllowedMinutes", rule.originalAllowedMinutes)
                obj.put("studyModePausedTimeMs", rule.studyModePausedTimeMs)
                array.put(obj)
            }
            prefs.edit { putString(KEY_ALLOWANCE_RULES, array.toString()) }
        }
        
    fun getAllowanceRule(packageName: String): AppAllowanceRule? {
        return allowanceRules.find { it.packageName == packageName }
    }

    fun saveAllowanceRule(rule: AppAllowanceRule): Boolean {
        val rules = allowanceRules.toMutableList()
        val index = rules.indexOfFirst { it.packageName == rule.packageName }
        val currentTime = System.currentTimeMillis()
        
        if (index != -1) {
            val existing = rules[index]
            if (!existing.canBeWeakenedBy(rule.allowedMinutes, rule.intervalMinutes, rule.isEnabled, currentTime)) {
                return false // Reject weakening active rule in backend persistence layer
            }
            rules[index] = rule
        } else {
            rules.add(rule)
        }
        allowanceRules = rules
        return true
    }

    fun deleteAllowanceRule(packageName: String): Boolean {
        val existing = getAllowanceRule(packageName) ?: return true
        val currentTime = System.currentTimeMillis()
        if (existing.isRuleActive(currentTime)) {
            return false // Reject deleting active rule in backend persistence layer
        }
        val rules = allowanceRules.filter { it.packageName != packageName }
        allowanceRules = rules
        return true
    }
        
    private val KEY_STUDY_SCHEDULES = "study_schedules"

    var studySchedules: List<StudySchedule>
        get() {
            val jsonString = prefs.getString(KEY_STUDY_SCHEDULES, "[]") ?: "[]"
            return try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<StudySchedule>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    
                    val cyclesJson = obj.optJSONArray("cycles")
                    val cycles = mutableListOf<StudyCycle>()
                    if (cyclesJson != null) {
                        for (j in 0 until cyclesJson.length()) {
                            val cycleObj = cyclesJson.getJSONObject(j)
                            cycles.add(
                                StudyCycle(
                                    studyDurationMinutes = cycleObj.getInt("studyDurationMinutes"),
                                    breakDurationMinutes = cycleObj.getInt("breakDurationMinutes")
                                )
                            )
                        }
                    }
                    
                    val weekdaysJson = obj.optJSONArray("weekdays")
                    val weekdays = mutableListOf<String>()
                    if (weekdaysJson != null) {
                        for (j in 0 until weekdaysJson.length()) {
                            weekdays.add(weekdaysJson.getString(j))
                        }
                    }

                    list.add(
                        StudySchedule(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            startTime = obj.getString("startTime"),
                            studyDurationMinutes = obj.getInt("studyDurationMinutes"),
                            breakDurationMinutes = obj.getInt("breakDurationMinutes"),
                            weekdays = weekdays,
                            isEnabled = obj.optBoolean("isEnabled", true),
                            blockedAppCount = obj.optInt("blockedAppCount", 4),
                            cycles = cycles
                        )
                    )
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
        set(value) {
            val array = JSONArray()
            for (schedule in value) {
                val obj = JSONObject()
                obj.put("id", schedule.id)
                obj.put("name", schedule.name)
                obj.put("startTime", schedule.startTime)
                obj.put("studyDurationMinutes", schedule.studyDurationMinutes)
                obj.put("breakDurationMinutes", schedule.breakDurationMinutes)
                obj.put("isEnabled", schedule.isEnabled)
                obj.put("blockedAppCount", schedule.blockedAppCount)
                
                val weekdaysArray = JSONArray()
                for (day in schedule.weekdays) {
                    weekdaysArray.put(day)
                }
                obj.put("weekdays", weekdaysArray)
                
                val cyclesArray = JSONArray()
                for (cycle in schedule.cycles) {
                    val cycleObj = JSONObject()
                    cycleObj.put("studyDurationMinutes", cycle.studyDurationMinutes)
                    cycleObj.put("breakDurationMinutes", cycle.breakDurationMinutes)
                    cyclesArray.put(cycleObj)
                }
                obj.put("cycles", cyclesArray)
                
                array.put(obj)
            }
            prefs.edit { putString(KEY_STUDY_SCHEDULES, array.toString()) }
        }

    fun saveStudySchedule(schedule: StudySchedule) {
        val schedules = studySchedules.toMutableList()
        val index = schedules.indexOfFirst { it.id == schedule.id }
        if (index != -1) {
            schedules[index] = schedule
        } else {
            schedules.add(schedule)
        }
        studySchedules = schedules
    }

    fun deleteStudySchedule(scheduleId: String) {
        val schedules = studySchedules.filter { it.id != scheduleId }
        studySchedules = schedules
    }

    fun updateProject2PauseState(isPaused: Boolean) {
        val rules = allowanceRules.toMutableList()
        val currentTime = System.currentTimeMillis()
        var updated = false
        
        for (i in rules.indices) {
            val rule = rules[i]
            if (isPaused) {
                // Starting study mode (pausing Project 2)
                if (rule.studyModePausedTimeMs == 0L) {
                    rules[i] = rule.copy(studyModePausedTimeMs = currentTime)
                    updated = true
                }
            } else {
                // Ending study mode (resuming Project 2)
                if (rule.studyModePausedTimeMs > 0L) {
                    val pausedDuration = currentTime - rule.studyModePausedTimeMs
                    rules[i] = rule.copy(
                        intervalStartTimeMs = rule.intervalStartTimeMs + pausedDuration,
                        studyModePausedTimeMs = 0L
                    )
                    updated = true
                }
            }
        }
        if (updated) {
            allowanceRules = rules
        }
    }
}
