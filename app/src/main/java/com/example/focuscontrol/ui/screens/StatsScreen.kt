package com.example.focuscontrol.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import com.example.focuscontrol.model.ChartBarData
import com.example.focuscontrol.model.PeriodUsageSummary
import com.example.focuscontrol.model.ReportingPeriod
import com.example.focuscontrol.ui.components.AppCard
import com.example.focuscontrol.ui.components.LogoWordmark
import com.example.focuscontrol.ui.components.SectionEyebrow
import com.example.focuscontrol.ui.theme.BlueAccent
import com.example.focuscontrol.ui.theme.LavenderGradientStart
import com.example.focuscontrol.ui.theme.LavenderPrimary
import com.example.focuscontrol.ui.theme.LimeAccent
import com.example.focuscontrol.ui.theme.NavyCardInner
import com.example.focuscontrol.ui.theme.PinkAccent
import com.example.focuscontrol.ui.theme.TextDimmed
import com.example.focuscontrol.ui.theme.TextMuted
import com.example.focuscontrol.ui.theme.TextWhite
import com.example.focuscontrol.ui.theme.WarningGold
import com.example.focuscontrol.utils.OnLifecycleEvent
import com.example.focuscontrol.utils.UsageStatsHelper
import java.util.Locale

@Composable
fun StatsScreen() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(UsageStatsHelper.hasUsageStatsPermission(context)) }
    var selectedPeriod by remember { mutableStateOf(ReportingPeriod.TODAY) }
    var selectedBarData by remember { mutableStateOf<ChartBarData?>(null) }
    var summaryData by remember { mutableStateOf<PeriodUsageSummary?>(null) }
    var selectedAppPackage by remember { mutableStateOf<String?>(null) }
    var showAllApps by remember { mutableStateOf(false) }

    val chartScrollState = rememberScrollState()

    OnLifecycleEvent { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            hasPermission = UsageStatsHelper.hasUsageStatsPermission(context)
        }
    }

    BackHandler(enabled = selectedAppPackage != null) {
        selectedAppPackage = null
    }

    if (selectedAppPackage != null) {
        AppAnalyticsDetailScreen(
            packageName = selectedAppPackage!!,
            initialPeriod = selectedPeriod,
            onBack = { selectedAppPackage = null }
        )
        return
    }

    LaunchedEffect(hasPermission, selectedPeriod, selectedBarData) {
        if (hasPermission) {
            val barStart = selectedBarData?.startTimeMs ?: 0L
            val barEnd = selectedBarData?.endTimeMs ?: 0L
            summaryData = UsageStatsHelper.getPeriodUsageSummary(
                context = context,
                period = selectedPeriod,
                selectedBarStartMs = barStart,
                selectedBarEndMs = barEnd
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        LogoWordmark(subtitle = "Usage Statistics & Analytics")

        if (!hasPermission) {
            AppCard(
                borderBrush = Brush.linearGradient(listOf(WarningGold, LavenderPrimary))
            ) {
                SectionEyebrow("USAGE ACCESS REQUIRED")
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Permission Needed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "FRÈRES requires 'Usage Access' permission to display your daily screen time, trend charts, and focus analytics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
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
                    Text("Grant Permission", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            // Priority 3: Reporting Period Selector Row with trailing contentPadding
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ReportingPeriod.entries) { p ->
                    val isSelected = p == selectedPeriod
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) LavenderPrimary else NavyCardInner)
                            .clickable {
                                selectedPeriod = p
                                selectedBarData = null
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = p.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF090C15) else TextMuted
                        )
                    }
                }
            }

            val summary = summaryData
            if (summary != null) {
                // Priority 2: Overview Screen Time Stats Card with weight(1f) and text constraints
                val hours = summary.totalTimeMs / (1000 * 60 * 60)
                val mins = (summary.totalTimeMs / (1000 * 60)) % 60
                val totalFormatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

                val diffMs = summary.totalTimeMs - summary.prevPeriodTotalTimeMs
                val diffHours = Math.abs(diffMs) / (1000 * 60 * 60)
                val diffMins = (Math.abs(diffMs) / (1000 * 60)) % 60
                val diffFormatted = if (diffHours > 0) "${diffHours}h ${diffMins}m" else "${diffMins}m"

                val pctChange = if (summary.prevPeriodTotalTimeMs > 0) {
                    ((diffMs.toFloat() / summary.prevPeriodTotalTimeMs.toFloat()) * 100).toInt()
                } else 0

                val avgHours = summary.dailyAverageMs / (1000 * 60 * 60)
                val avgMins = (summary.dailyAverageMs / (1000 * 60)) % 60
                val avgFormatted = if (avgHours > 0) "${avgHours}h ${avgMins}m/day" else "${avgMins}m/day"

                AppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionEyebrow(
                                text = "${selectedPeriod.label.uppercase()} SCREEN TIME (${summary.startDateStr})",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = totalFormatted,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LavenderPrimary.copy(alpha = 0.18f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${summary.appCount} Apps Used",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LavenderPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Avg: $avgFormatted",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Text(
                                text = String.format(Locale.ROOT, "Peak: %02d:00-%02d:00", summary.peakHour, (summary.peakHour + 1) % 24),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Highlight Card for #1 Most Used App
                if (summary.mostUsedApp != null) {
                    val topApp = summary.mostUsedApp
                    val topHours = topApp.timeForegroundMs / (1000 * 60 * 60)
                    val topMins = (topApp.timeForegroundMs / (1000 * 60)) % 60
                    val topTimeStr = if (topHours > 0) "${topHours}h ${topMins}m" else "${topMins}m"

                    AppCard(
                        modifier = Modifier.clickable { selectedAppPackage = topApp.packageName }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                if (topApp.icon != null) {
                                    Image(
                                        bitmap = topApp.icon,
                                        contentDescription = topApp.appName,
                                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(LavenderPrimary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(topApp.appName.take(1), color = LavenderPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Column {
                                    SectionEyebrow("🏆 MOST USED APP")
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = topApp.appName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = topApp.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Text(
                                text = topTimeStr,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = LimeAccent
                            )
                        }
                    }
                }

                // Priority 4: Interactive Usage Chart with 3-hour ticks, auto-scroll, and clean future hour indicators
                AppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionEyebrow("${selectedPeriod.label.uppercase()} USAGE CHART")

                        if (selectedBarData != null) {
                            Text(
                                text = "Clear Filter ✕",
                                style = MaterialTheme.typography.labelSmall,
                                color = LavenderPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { selectedBarData = null }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (summary.chartBars.isEmpty() || summary.totalTimeMs == 0L) {
                        Text(
                            text = "No screen time recorded for ${selectedPeriod.label.lowercase()}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    } else {
                        val maxVal = maxOf(1L, summary.chartBars.maxOf { it.valueMs })
                        val isManyBars = summary.chartBars.size > 7

                        // Auto scroll to peak hour / active bar for 24-hour chart
                        LaunchedEffect(selectedPeriod, summary.chartBars) {
                            if (isManyBars && summary.peakHour in 0..23) {
                                val targetScroll = maxOf(0, (summary.peakHour - 2) * 28)
                                chartScrollState.animateScrollTo(targetScroll)
                            }
                        }

                        val chartRowModifier = if (isManyBars) {
                            Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .horizontalScroll(chartScrollState)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        }

                        Row(
                            modifier = chartRowModifier,
                            horizontalArrangement = if (isManyBars) Arrangement.spacedBy(4.dp) else Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            summary.chartBars.forEachIndexed { index, bar ->
                                val isSelected = selectedBarData?.startTimeMs == bar.startTimeMs
                                val heightFraction = (bar.valueMs.toFloat() / maxVal.toFloat()).coerceIn(0.04f, 1f)
                                val bHours = bar.valueMs / (1000 * 60 * 60)
                                val bMins = (bar.valueMs / (1000 * 60)) % 60
                                val valStr = if (bHours > 0) "${bHours}h ${bMins}m" else "${bMins}m"

                                val barBrush = when {
                                    bar.isFuture -> Brush.verticalGradient(listOf(NavyCardInner, NavyCardInner))
                                    isSelected -> Brush.linearGradient(listOf(LavenderPrimary, LavenderPrimary))
                                    bar.valueMs > 0 -> Brush.verticalGradient(listOf(LavenderGradientStart, BlueAccent))
                                    else -> Brush.verticalGradient(listOf(NavyCardInner, NavyCardInner))
                                }

                                // Show tick label every 3 hours (00, 03, 06, 09, 12, 15, 18, 21) or if selected
                                val showLabel = !isManyBars || (index % 3 == 0) || isSelected

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = if (isManyBars) {
                                        Modifier
                                            .width(28.dp)
                                            .fillMaxHeight()
                                            .clickable(enabled = !bar.isFuture) { selectedBarData = if (isSelected) null else bar }
                                    } else {
                                        Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable(enabled = !bar.isFuture) { selectedBarData = if (isSelected) null else bar }
                                            .padding(horizontal = 2.dp)
                                    }
                                ) {
                                    Text(
                                        text = if (bar.valueMs > 0 && !bar.isFuture) valStr else "",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            color = if (isSelected) LimeAccent else TextMuted
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Visible,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(Modifier.weight(1f))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(if (isManyBars) 0.85f else 0.9f)
                                            .fillMaxHeight(if (bar.isFuture) 0.02f else heightFraction)
                                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                            .background(barBrush)
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = if (showLabel) bar.label.take(2) else "",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            color = if (isSelected) LavenderPrimary else if (bar.isFuture) TextDimmed else TextWhite,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Visible,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        if (selectedBarData != null) {
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LavenderPrimary.copy(alpha = 0.15f))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Filtered by ${selectedBarData?.sublabel ?: selectedBarData?.label} (${selectedBarData?.valueMs?.let { ms -> val h = ms / 3600000; val m = (ms / 60000) % 60; if (h > 0) "${h}h ${m}m" else "${m}m" }}). Tap bar again to clear filter.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LavenderPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // App Categories Breakdown Card
                if (summary.categoryStats.isNotEmpty()) {
                    AppCard {
                        SectionEyebrow("USAGE BY CATEGORY")
                        Spacer(Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            summary.categoryStats.forEach { cat ->
                                val catHours = cat.timeMs / (1000 * 60 * 60)
                                val catMins = (cat.timeMs / (1000 * 60)) % 60
                                val catFormatted = if (catHours > 0) "${catHours}h ${catMins}m" else "${catMins}m"
                                val pctText = "${(cat.percentage * 100).toInt()}%"

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(cat.categoryName, style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                                        Text("$catFormatted ($pctText)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(cat.percentage.coerceIn(0.04f, 1f))
                                            .height(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (cat.categoryName) {
                                                    "Social Media" -> PinkAccent
                                                    "Games" -> WarningGold
                                                    "Productivity" -> LimeAccent
                                                    "Entertainment & Media" -> BlueAccent
                                                    else -> LavenderPrimary
                                                }
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                // App Usage Breakdown List
                AppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionEyebrow("APP USAGE BREAKDOWN (${summary.appStats.size})")
                        Text(
                            text = "Tap app for detail →",
                            style = MaterialTheme.typography.labelSmall,
                            color = LavenderPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    if (summary.appStats.isEmpty()) {
                        Text(
                            text = "No app usage data available for this selection.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        val maxTime = summary.appStats.firstOrNull()?.timeForegroundMs?.toFloat() ?: 1f
                        val displayedApps = if (showAllApps) summary.appStats else summary.appStats.take(6)

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            displayedApps.forEach { stat ->
                                val hours = stat.timeForegroundMs / (1000 * 60 * 60)
                                val mins = (stat.timeForegroundMs / (1000 * 60)) % 60
                                val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                                val fraction = (stat.timeForegroundMs.toFloat() / maxTime).coerceIn(0.05f, 1f)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NavyCardInner)
                                        .clickable { selectedAppPackage = stat.packageName }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (stat.icon != null) {
                                        Image(
                                            bitmap = stat.icon,
                                            contentDescription = stat.appName,
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
                                                text = stat.appName.take(1).uppercase(),
                                                color = LavenderPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = stat.appName,
                                                color = TextWhite,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = timeStr,
                                                color = LavenderPrimary,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(Modifier.height(4.dp))

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction)
                                                .height(5.dp)
                                                .clip(CircleShape)
                                                .background(Brush.horizontalGradient(listOf(LavenderGradientStart, BlueAccent)))
                                        )
                                    }
                                }
                            }

                            if (summary.appStats.size > 6) {
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(LavenderPrimary.copy(alpha = 0.15f))
                                        .clickable { showAllApps = !showAllApps }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (showAllApps) "Show Top 6 Only ↑" else "View All ${summary.appStats.size} Apps ↓",
                                        color = LavenderPrimary,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
