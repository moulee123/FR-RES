package com.example.focuscontrol

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.example.focuscontrol.model.getFixedIntervalStart

class FocusBlockerService : AccessibilityService() {

    private var lastBlockedPackage: String? = null
    private var lastBlockedTimeMs: Long = 0L
    
    private var currentForegroundPackage: String? = null
    private var currentPackageStartTime: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    
    private val checkUsageRunnable = object : Runnable {
        override fun run() {
            updateForegroundUsage()
            handler.postDelayed(this, 1000L) // Check every 1 second
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        handler.postDelayed(checkUsageRunnable, 1000L)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        handler.removeCallbacks(checkUsageRunnable)
    }

    private fun updateForegroundUsage() {
        val prefs = FocusPreferences(this)
        
        // 1. PROJECT 1 STUDY MODE PRIORITY
        // While Study Mode is active, Project 1 rules take priority.
        // Project 2 usage tracking & allowance evaluation MUST be suspended without losing data.
        if (prefs.isStudyModeEnabled) {
            val activePackage = currentForegroundPackage ?: getActiveForegroundPackage()
            if (activePackage != null &&
                activePackage != this.packageName &&
                activePackage != "com.example.focuscontrol" &&
                activePackage != "com.android.systemui" &&
                !activePackage.contains("launcher", ignoreCase = true)
            ) {
                if (prefs.isAppBlocked(activePackage)) {
                    val isOnBreak = prefs.isStudyModeOnBreak()
                    val isExtensionActive = prefs.isStudyExtensionActive()
                    if (!isOnBreak || isExtensionActive) {
                        blockApp(activePackage)
                    }
                }
            }
            // Reset package start time so time spent in Study Mode is NOT accumulated into Project 2 allowances
            currentPackageStartTime = System.currentTimeMillis()
            return
        }
        
        // 2. PROJECT 2 ALLOWANCE TRACKING (When Study Mode is inactive)
        val activePackage = currentForegroundPackage ?: getActiveForegroundPackage() ?: return
        
        // Never track or block FRÈRES itself, System UI, or Launchers
        if (activePackage == this.packageName ||
            activePackage == "com.example.focuscontrol" ||
            activePackage == "com.android.systemui" ||
            activePackage.contains("launcher", ignoreCase = true)
        ) {
            currentPackageStartTime = System.currentTimeMillis()
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentPackageStartTime == 0L) {
            currentPackageStartTime = currentTime
            return
        }
        
        val durationMs = currentTime - currentPackageStartTime
        currentPackageStartTime = currentTime // Reset tick anchor

        // Ignore unreasonable time jumps (e.g. device reboot or clock sync spikes)
        if (durationMs <= 0L || durationMs > 30_000L) {
            return
        }

        val rule = prefs.getAllowanceRule(activePackage)
        if (rule != null && rule.isEnabled) {
            val intervalDurationMs = rule.intervalMinutes * 60 * 1000L
            val trueFixedStart = getFixedIntervalStart(currentTime, rule.intervalMinutes)
            
            val effectiveStart = if (rule.intervalStartTimeMs == 0L) trueFixedStart else rule.intervalStartTimeMs
            val isExpired = (currentTime - effectiveStart) >= intervalDurationMs
            
            val updatedRule = if (isExpired) {
                // Fixed clock interval boundary reached: reset usage and update start anchor
                rule.copy(
                    intervalStartTimeMs = trueFixedStart,
                    usedForegroundTimeMs = durationMs,
                    remainingMinutes = rule.allowedMinutes,
                    timeToResetText = if (rule.intervalMinutes % 60 == 0) "Resets in ${rule.intervalMinutes / 60}h" else "Resets in ${rule.intervalMinutes}m",
                    studyModePausedTimeMs = 0L
                )
            } else {
                val newUsedTimeMs = rule.usedForegroundTimeMs + durationMs
                val allowedMs = rule.allowedMinutes * 60 * 1000L
                val remainingMs = maxOf(0L, allowedMs - newUsedTimeMs)
                val remainingMins = (remainingMs / (60 * 1000)).toInt()
                
                val timeToResetMs = maxOf(0L, intervalDurationMs - (currentTime - effectiveStart))
                val hoursToReset = timeToResetMs / (60 * 60 * 1000)
                val minsToReset = (timeToResetMs % (60 * 60 * 1000)) / (60 * 1000)
                
                val resetText = if (hoursToReset > 0) "Resets in ${hoursToReset}h ${minsToReset}m" else "Resets in ${minsToReset}m"
                
                rule.copy(
                    usedForegroundTimeMs = newUsedTimeMs,
                    remainingMinutes = remainingMins,
                    timeToResetText = resetText,
                )
            }
            
            prefs.saveAllowanceRule(updatedRule)
            
            // Trigger existing blocking mechanism if allowance is exhausted
            if (updatedRule.usedForegroundTimeMs >= updatedRule.allowedMinutes * 60 * 1000L) {
                blockApp(activePackage)
            }
        }
    }
    
    private fun blockApp(pkg: String) {
        // EXPLICIT SAFETY GUARD: NEVER BLOCK FRÈRES ITSELF, SYSTEM UI, OR LAUNCHER!
        if (pkg == this.packageName || pkg == "com.example.focuscontrol" || pkg == "com.android.systemui") {
            return
        }

        val currentTime = System.currentTimeMillis()
        if (pkg == lastBlockedPackage && (currentTime - lastBlockedTimeMs) < 500L) {
            return
        }
        
        lastBlockedPackage = pkg
        lastBlockedTimeMs = currentTime

        performGlobalAction(GLOBAL_ACTION_HOME)
        val intent = Intent(this, BlockedActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(BlockedActivity.EXTRA_BLOCKED_PACKAGE, pkg)
        }
        startActivity(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && type != AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            return
        }

        val eventPackage = event.packageName?.toString()
        if (eventPackage == this.packageName || eventPackage == "com.example.focuscontrol") {
            currentForegroundPackage = this.packageName
            currentPackageStartTime = System.currentTimeMillis()
            return
        }

        // 1. Never block if FocusControl itself is in the foreground
        // (Handled above by checking eventPackage)

        // 2. Identify the active foreground package
        val activePackage = getActiveForegroundPackage() ?: return
        
        if (activePackage != currentForegroundPackage) {
            updateForegroundUsage()
            currentForegroundPackage = activePackage
            currentPackageStartTime = System.currentTimeMillis()
        }

        // 3. Never block FocusControl, system UI, or launchers
        if (activePackage == this.packageName ||
            activePackage == "com.example.focuscontrol" ||
            activePackage == "com.android.systemui" ||
            activePackage.contains("launcher", ignoreCase = true)
        ) {
            return
        }

        // 4. Prevent duplicate rapid launches for the same package within 500ms
        val currentTime = System.currentTimeMillis()
        if (activePackage == lastBlockedPackage && (currentTime - lastBlockedTimeMs) < 500L) {
            return
        }

        val prefs = FocusPreferences(this)

        // 5. Check Study Mode rules
        if (prefs.isStudyModeEnabled && prefs.isAppBlocked(activePackage)) {
            val isOnBreak = prefs.isStudyModeOnBreak()
            val isExtensionActive = prefs.isStudyExtensionActive()
            
            // Block if we are NOT on a break, OR if we are on a break but an extension is active
            if (!isOnBreak || isExtensionActive) {
                blockApp(activePackage)
                return
            }
        }

        // Check Project 2 rules immediately on open
            val rule = prefs.getAllowanceRule(activePackage)
            if (rule != null && rule.isEnabled && !prefs.isStudyModeEnabled) {
                val intervalDurationMs = rule.intervalMinutes * 60 * 1000L
                val trueFixedStart = getFixedIntervalStart(currentTime, rule.intervalMinutes)
                val effectiveStart = if (rule.intervalStartTimeMs == 0L) trueFixedStart else rule.intervalStartTimeMs
                val isExpired = (currentTime - effectiveStart) >= intervalDurationMs
                
                if (!isExpired && rule.usedForegroundTimeMs >= rule.allowedMinutes * 60 * 1000L) {
                    blockApp(activePackage)
                }
            }
    }

    private fun getActiveForegroundPackage(): String? {
        // Primary check: rootInActiveWindow
        try {
            val root = rootInActiveWindow
            if (root != null) {
                val pkg = root.packageName?.toString()
                if (!pkg.isNullOrEmpty()) {
                    return pkg
                }
            }
        } catch (e: Exception) {
            // Ignore accessibility node errors
        }

        // Secondary check: interactive windows list
        try {
            val windowsList = windows
            for (window in windowsList) {
                if (window.isFocused || window.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                    val root = window.root
                    if (root != null) {
                        val pkg = root.packageName?.toString()
                        if (!pkg.isNullOrEmpty()) {
                            return pkg
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore window list access errors
        }

        return null
    }

    override fun onInterrupt() {}

    companion object {
        var instance: FocusBlockerService? = null

        fun triggerImmediateCheck() {
            instance?.updateForegroundUsage()
        }

        fun isServiceEnabled(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)

            val expectedComponent = ComponentName(context, FocusBlockerService::class.java)
            val expectedFlatName = expectedComponent.flattenToString()
            val expectedShortFlatName = expectedComponent.flattenToShortString()

            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedFlatName, ignoreCase = true) ||
                    componentName.equals(expectedShortFlatName, ignoreCase = true)
                ) {
                    return true
                }
            }
            return false
        }
    }
}
