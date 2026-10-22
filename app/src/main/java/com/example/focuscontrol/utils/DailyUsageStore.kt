package com.example.focuscontrol.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONObject

object DailyUsageStore {

    private const val PREFS_NAME = "freres_daily_usage_store"
    private const val MAX_SINGLE_APP_DAILY_MS = 24 * 60 * 60 * 1000L // 24 hours max per app per day

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun sanitizeUsageMap(map: Map<String, Long>): Map<String, Long> {
        val sanitized = mutableMapOf<String, Long>()
        for ((pkg, timeMs) in map) {
            val capped = minOf(maxOf(0L, timeMs), MAX_SINGLE_APP_DAILY_MS)
            if (capped > 0) {
                sanitized[pkg] = capped
            }
        }
        return sanitized
    }

    fun removeSummary(context: Context, dateStr: String) {
        getPrefs(context).edit {
            remove("summary_$dateStr")
        }
    }

    fun saveDailySummary(context: Context, dateStr: String, usageMap: Map<String, Long>) {
        val sanitized = sanitizeUsageMap(usageMap)
        if (sanitized.isEmpty()) return
        val json = JSONObject()
        for ((pkg, timeMs) in sanitized) {
            json.put(pkg, timeMs)
        }
        getPrefs(context).edit {
            putString("summary_$dateStr", json.toString())
        }
    }

    fun getDailySummary(context: Context, dateStr: String): Map<String, Long>? {
        val jsonStr = getPrefs(context).getString("summary_$dateStr", null) ?: return null
        return try {
            val json = JSONObject(jsonStr)
            val map = mutableMapOf<String, Long>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val valMs = json.getLong(key)
                val cappedMs = minOf(maxOf(0L, valMs), MAX_SINGLE_APP_DAILY_MS)
                if (cappedMs > 0) {
                    map[key] = cappedMs
                }
            }
            map
        } catch (_: Exception) {
            null
        }
    }

    fun reconcileAndSave(context: Context, dateStr: String, androidUsageMap: Map<String, Long>): Map<String, Long> {
        val existingLocal = getDailySummary(context, dateStr) ?: emptyMap()
        val combined = mutableMapOf<String, Long>()

        val allKeys = existingLocal.keys + androidUsageMap.keys
        for (pkg in allKeys) {
            val localTime = existingLocal[pkg] ?: 0L
            val androidTime = androidUsageMap[pkg] ?: 0L
            val chosen = minOf(maxOf(localTime, androidTime), MAX_SINGLE_APP_DAILY_MS)
            if (chosen > 0) {
                combined[pkg] = chosen
            }
        }

        saveDailySummary(context, dateStr, combined)
        return combined
    }

    fun getSummariesForDateRange(context: Context, startDateStr: String, endDateStr: String): Map<String, Map<String, Long>> {
        val prefs = getPrefs(context)
        val result = mutableMapOf<String, Map<String, Long>>()
        val allEntries = prefs.all

        for ((key, value) in allEntries) {
            if (key.startsWith("summary_") && value is String) {
                val dateStr = key.removePrefix("summary_")
                if (dateStr in startDateStr..endDateStr) {
                    try {
                        val json = JSONObject(value)
                        val map = mutableMapOf<String, Long>()
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val pkg = keys.next()
                            val valMs = json.getLong(pkg)
                            val cappedMs = minOf(maxOf(0L, valMs), MAX_SINGLE_APP_DAILY_MS)
                            if (cappedMs > 0) {
                                map[pkg] = cappedMs
                            }
                        }
                        result[dateStr] = map
                    } catch (_: Exception) {
                        // ignore corrupt records
                    }
                }
            }
        }
        return result
    }
}
