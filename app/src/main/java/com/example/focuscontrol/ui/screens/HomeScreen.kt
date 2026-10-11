package com.example.focuscontrol.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.focuscontrol.FocusPreferences
import com.example.focuscontrol.model.AppInfo
import com.example.focuscontrol.model.StudySchedule
import com.example.focuscontrol.ui.components.AppCard
import java.util.Calendar
import com.example.focuscontrol.ui.components.LogoWordmark
import com.example.focuscontrol.ui.components.SectionEyebrow
import com.example.focuscontrol.ui.theme.BlueAccent
import com.example.focuscontrol.ui.theme.LavenderGradientStart
import com.example.focuscontrol.ui.theme.LavenderPrimary
import com.example.focuscontrol.ui.theme.LimeAccent
import com.example.focuscontrol.ui.theme.NavyCardBorder
import com.example.focuscontrol.ui.theme.NavyCardInner
import com.example.focuscontrol.ui.theme.PinkAccent
import com.example.focuscontrol.ui.theme.PurpleAccent
import com.example.focuscontrol.ui.theme.TextDimmed
import com.example.focuscontrol.ui.theme.TextMuted
import com.example.focuscontrol.ui.theme.TextWhite
import com.example.focuscontrol.ui.theme.WarningGold

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun HomeScreen(
    context: Context,
    isServiceEnabled: Boolean,
    studyModeEnabled: Boolean,
    isOnBreak: Boolean,
    blockedPackages: Set<String>,
    installedApps: List<AppInfo>,
    studySchedules: List<StudySchedule>,
    onEndBreak: () -> Unit,
    onToggleBlockedApp: (String, Boolean) -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    var showAppSelector by remember { mutableStateOf(false) }

    if (showAppSelector) {
        BlockedAppsSelector(
            blockedPackages = blockedPackages,
            installedApps = installedApps,
            onToggleBlockedApp = onToggleBlockedApp,
            onBack = { showAppSelector = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LogoWordmark()

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        when {
                            !isServiceEnabled -> WarningGold.copy(alpha = 0.2f)
                            isOnBreak -> LavenderPrimary.copy(alpha = 0.2f)
                            studyModeEnabled -> LimeAccent.copy(alpha = 0.2f)
                            else -> NavyCardInner
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    !isServiceEnabled -> WarningGold
                                    isOnBreak -> LavenderPrimary
                                    studyModeEnabled -> LimeAccent
                                    else -> TextDimmed
                                }
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            !isServiceEnabled -> "SETUP REQUIRED"
                            isOnBreak -> "ON BREAK"
                            studyModeEnabled -> "ACTIVE"
                            else -> "OFF"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            !isServiceEnabled -> WarningGold
                            isOnBreak -> LavenderPrimary
                            studyModeEnabled -> LimeAccent
                            else -> TextMuted
                        }
                    )
                }
            }
        }

        // Accessibility setup warning if disabled
        if (!isServiceEnabled) {
            AppCard(
                borderBrush = Brush.linearGradient(listOf(WarningGold, LavenderPrimary))
            ) {
                SectionEyebrow("ACCESSIBILITY SERVICE")
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Enable App Blocker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "FRÈRES needs Accessibility permission to detect when restricted apps are launched during focus sessions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            Brush.horizontalGradient(listOf(WarningGold, LavenderPrimary)),
                            RoundedCornerShape(14.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF090C15)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Open Settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // Travel Mode Confirmation Overdue Warning Card
        val prefs = remember { FocusPreferences(context) }
        val isTravelModeOverdue = remember(prefs.isTravelModeEnabled, prefs.lastTravelConfirmationTimeMs) {
            prefs.isTravelConfirmationOverdue()
        }

        if (isTravelModeOverdue) {
            AppCard(
                borderBrush = Brush.linearGradient(listOf(WarningGold, PinkAccent))
            ) {
                SectionEyebrow("TRAVEL MODE • OVERDUE CONFIRMATION")
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Location Verification Overdue",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(WarningGold.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("OVERDUE", style = MaterialTheme.typography.labelSmall, color = WarningGold, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Your 2-hour location confirmation for Travel Mode has expired. Verify your location to confirm travel outside ${prefs.homeLocationName} and maintain travel allowance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { onNavigateToSettings() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            Brush.horizontalGradient(listOf(WarningGold, LavenderPrimary)),
                            RoundedCornerShape(14.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF090C15)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("📍 Verify Location in Settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // Hero Study Mode Card
        val gradientBrush = Brush.horizontalGradient(listOf(LavenderGradientStart, PinkAccent))
        AppCard(
            borderBrush = Brush.linearGradient(
                colors = if (studyModeEnabled && isServiceEnabled && !isOnBreak) {
                    listOf(LavenderPrimary, PinkAccent)
                } else {
                    listOf(NavyCardBorder, NavyCardBorder)
                }
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LavenderPrimary.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "STUDY MODE • FOCUS SESSION",
                            style = MaterialTheme.typography.labelSmall,
                            color = LavenderPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Study Mode",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when {
                            !isServiceEnabled -> "Service setup required for blocking."
                            isOnBreak -> "On 5-minute study break. Blocking paused."
                            studyModeEnabled -> if (blockedPackages.isNotEmpty()) "Blocking active • ${blockedPackages.size} apps restricted." else "Session active. Tap below to restrict apps."
                            else -> "Ready when you need a quiet screen."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
                Spacer(Modifier.width(12.dp))
                
                // Read-only status badge replacing the manual toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            when {
                                !isServiceEnabled -> WarningGold.copy(alpha = 0.2f)
                                isOnBreak -> LavenderPrimary.copy(alpha = 0.2f)
                                studyModeEnabled -> LimeAccent.copy(alpha = 0.2f)
                                else -> NavyCardInner
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when {
                            !isServiceEnabled -> "SETUP REQUIRED"
                            isOnBreak -> "BREAK ACTIVE"
                            studyModeEnabled -> "STUDY ACTIVE"
                            else -> "SCHEDULED"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            !isServiceEnabled -> WarningGold
                            isOnBreak -> LavenderPrimary
                            studyModeEnabled -> LimeAccent
                            else -> TextMuted
                        }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            if (isOnBreak) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(LavenderPrimary.copy(alpha = 0.12f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "5-min Study Break Active",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LavenderPrimary
                    )
                    Button(
                        onClick = onEndBreak,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LavenderPrimary,
                            contentColor = TextWhite
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("End Break", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Responsive layout: stack buttons vertically so the label never clips
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showAppSelector = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            if (blockedPackages.isNotEmpty()) gradientBrush else Brush.linearGradient(listOf(NavyCardInner, NavyCardInner)),
                            RoundedCornerShape(14.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = TextWhite
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (blockedPackages.isEmpty()) "+ Select Apps" else "Manage Apps (${blockedPackages.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        // Upcoming Schedule Card
        AppCard {
            val nextScheduleInfo = remember(studySchedules) {
                val nowMs = System.currentTimeMillis()
                val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
                val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

                var nextSchedule: StudySchedule? = null
                var nextStartMs = Long.MAX_VALUE
                var nextEndMs = 0L

                for (schedule in studySchedules.filter { it.isEnabled }) {
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

                        val c = Calendar.getInstance()
                        c.set(Calendar.HOUR_OF_DAY, startHour)
                        c.set(Calendar.MINUTE, startMinute)
                        c.set(Calendar.SECOND, 0)
                        c.set(Calendar.MILLISECOND, 0)

                        var diff = targetDayOfWeek - currentDayOfWeek
                        if (diff < 0) diff += 7
                        c.add(Calendar.DAY_OF_YEAR, diff)
                        
                        val offsetMins = schedule.cycles.sumOf { it.studyDurationMinutes + it.breakDurationMinutes }
                        var baseStartMs = c.timeInMillis
                        
                        if (diff == 0 && (baseStartMs + offsetMins * 60 * 1000L) <= nowMs) {
                            c.add(Calendar.DAY_OF_YEAR, 7)
                            baseStartMs = c.timeInMillis
                        }

                        if (baseStartMs < nextStartMs) {
                            nextStartMs = baseStartMs
                            nextSchedule = schedule
                            nextEndMs = baseStartMs + offsetMins * 60 * 1000L
                        }
                    }
                }

                if (nextSchedule != null) {
                    val startCal = Calendar.getInstance().apply { timeInMillis = nextStartMs }
                    val startH = if (startCal.get(Calendar.HOUR) == 0) 12 else startCal.get(Calendar.HOUR)
                    val startM = startCal.get(Calendar.MINUTE).toString().padStart(2, '0')
                    val startAmPm = if (startCal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
                    
                    val endCal = Calendar.getInstance().apply { timeInMillis = nextEndMs }
                    val endH = if (endCal.get(Calendar.HOUR) == 0) 12 else endCal.get(Calendar.HOUR)
                    val endM = endCal.get(Calendar.MINUTE).toString().padStart(2, '0')
                    val endAmPm = if (endCal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
                    
                    val daysStr = nextSchedule.weekdays.joinToString(", ")
                    val formattedTimeRange = "$startH:$startM $startAmPm – $endH:$endM $endAmPm • $daysStr"

                    val timeToMs = nextStartMs - nowMs
                    val hours = timeToMs / (1000 * 60 * 60)
                    val mins = (timeToMs / (1000 * 60)) % 60
                    val timeStr = if (timeToMs < 0) "Active" else if (hours > 0) "In ${hours}h ${mins}m" else "In ${mins}m"

                    Triple(nextSchedule.name, formattedTimeRange, timeStr)
                } else {
                    null
                }
            }

            if (nextScheduleInfo != null) {
                val (schedName, timeRangeText, countdownText) = nextScheduleInfo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BlueAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⏰", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SectionEyebrow("UPCOMING SCHEDULE")
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = schedName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = timeRangeText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                    
                    Spacer(Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BlueAccent.copy(alpha = 0.18f))
                            .clickable { onNavigateToSchedule() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = countdownText,
                            style = MaterialTheme.typography.labelMedium,
                            color = BlueAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Empty state
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NavyCardInner),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📅", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SectionEyebrow("UPCOMING SCHEDULE")
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "No upcoming schedules",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "Set up your focus routines.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                    
                    Spacer(Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(LavenderPrimary.copy(alpha = 0.18f))
                            .clickable { onNavigateToSchedule() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+ Create",
                            style = MaterialTheme.typography.labelMedium,
                            color = LavenderPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Restricted Apps Quick Preview
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionEyebrow("RESTRICTED APPS PREVIEW")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${blockedPackages.size} Apps Blocked in Study Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelMedium,
                    color = LavenderPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showAppSelector = true }
                )
            }

            Spacer(Modifier.height(14.dp))

            if (blockedPackages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NavyCardInner)
                        .clickable { showAppSelector = true }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Tap here to configure restricted apps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                val appList = remember(blockedPackages, installedApps) {
                    blockedPackages.asSequence().take(6).mapNotNull { pkg ->
                        installedApps.find { it.packageName == pkg }
                    }.toList()
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(appList) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(60.dp)
                        ) {
                            if (app.icon != null) {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = app.appName,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(LavenderPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = app.appName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = LavenderPrimary
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Today's Focus Statistics Summary
        AppCard(
            modifier = Modifier.clickable { onNavigateToStats() }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionEyebrow("TODAY'S SUMMARY")
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "View Analytics",
                    style = MaterialTheme.typography.labelMedium,
                    color = LavenderPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "2h 18m",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "Total Focus Screen Time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(LimeAccent.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "↓ 38m saved today",
                        style = MaterialTheme.typography.labelMedium,
                        color = LimeAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BlockedAppsSelector(
    blockedPackages: Set<String>,
    installedApps: List<AppInfo>,
    onToggleBlockedApp: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(searchQuery, installedApps) {
        installedApps.filter { app ->
            searchQuery.isBlank() ||
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LogoWordmark(subtitle = "Study Mode • Blocked Apps")
            Text(
                text = "Done",
                style = MaterialTheme.typography.titleMedium,
                color = LavenderPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onBack() }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(NavyCardInner)
                .padding(12.dp)
        ) {
            Text(
                text = "Apps selected below are strictly blocked when Study Mode is ON. Unselected apps remain accessible during study sessions.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed apps...", color = TextDimmed) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = LavenderPrimary,
                unfocusedBorderColor = NavyCardBorder,
                focusedContainerColor = NavyCardInner,
                unfocusedContainerColor = NavyCardInner
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                val isBlocked = blockedPackages.contains(app.packageName)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NavyCardInner)
                        .clickable { onToggleBlockedApp(app.packageName, !isBlocked) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (app.icon != null) {
                        Image(
                            bitmap = app.icon,
                            contentDescription = app.appName,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(LavenderPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = app.appName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = LavenderPrimary
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDimmed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Checkbox(
                        checked = isBlocked,
                        onCheckedChange = { onToggleBlockedApp(app.packageName, it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = LavenderPrimary,
                            checkmarkColor = Color(0xFF090C15),
                            uncheckedColor = TextDimmed
                        )
                    )
                }
            }
        }
    }
}
