package com.example.focuscontrol.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focuscontrol.model.AppDetailAnalytics
import com.example.focuscontrol.model.ReportingPeriod
import com.example.focuscontrol.ui.components.AppCard
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
import com.example.focuscontrol.utils.UsageStatsHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppAnalyticsDetailScreen(
    packageName: String,
    initialPeriod: ReportingPeriod = ReportingPeriod.TODAY,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var period by remember { mutableStateOf(initialPeriod) }
    var analyticsData by remember { mutableStateOf<AppDetailAnalytics?>(null) }
    var selectedHourIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(packageName, period) {
        analyticsData = UsageStatsHelper.getAppDetailAnalytics(context, packageName, period)
    }

    val data = analyticsData

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "← Back",
                style = MaterialTheme.typography.titleMedium,
                color = LavenderPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onBack() }
            )

            Text(
                text = "App Analytics",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.width(48.dp))
        }

        if (data != null) {
            // App Identity & Category Header Card
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (data.icon != null) {
                        Image(
                            bitmap = data.icon,
                            contentDescription = data.appName,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(LavenderPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = data.appName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = LavenderPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = data.appName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = data.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDimmed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(LavenderPrimary.copy(alpha = 0.18f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = data.categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = LavenderPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Reporting Period Selector Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ReportingPeriod.entries) { p ->
                    val isSelected = p == period
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) LavenderPrimary else NavyCardInner)
                            .clickable {
                                period = p
                                selectedHourIndex = -1
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

            // Usage Overview Stats Grid
            val hours = data.totalTimeMs / (1000 * 60 * 60)
            val mins = (data.totalTimeMs / (1000 * 60)) % 60
            val totalFormatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

            val diffMs = data.totalTimeMs - data.prevPeriodTotalTimeMs
            val diffHours = Math.abs(diffMs) / (1000 * 60 * 60)
            val diffMins = (Math.abs(diffMs) / (1000 * 60)) % 60
            val diffFormatted = if (diffHours > 0) "${diffHours}h ${diffMins}m" else "${diffMins}m"

            val pctChange = if (data.prevPeriodTotalTimeMs > 0) {
                ((diffMs.toFloat() / data.prevPeriodTotalTimeMs.toFloat()) * 100).toInt()
            } else 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppCard(modifier = Modifier.weight(1f)) {
                    SectionEyebrow("USAGE FOR ${period.label.uppercase()}")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = totalFormatted,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when {
                            data.prevPeriodTotalTimeMs == 0L -> "No prior history"
                            diffMs > 0 -> "+$pctChange% (+$diffFormatted) vs prev"
                            diffMs < 0 -> "$pctChange% (-$diffFormatted) vs prev"
                            else -> "Unchanged vs prev period"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            diffMs > 0 -> PinkAccent
                            diffMs < 0 -> LimeAccent
                            else -> TextMuted
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                AppCard(modifier = Modifier.weight(1f)) {
                    SectionEyebrow("LAUNCH SESSIONS")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (data.estimatedSessions > 0) "${data.estimatedSessions}" else "N/A",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = LimeAccent
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (data.estimatedSessions > 0) "Recorded launches" else "Event data limited",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            // Interactive Usage Chart for App
            AppCard {
                SectionEyebrow("${period.label.uppercase()} APP USAGE TREND")
                Spacer(Modifier.height(12.dp))

                if (data.chartBars.isEmpty() || data.totalTimeMs == 0L) {
                    Text(
                        text = "No usage recorded for ${data.appName} during ${period.label.lowercase()}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                } else {
                    val maxVal = maxOf(1L, data.chartBars.maxOf { it.valueMs })

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        data.chartBars.forEach { bar ->
                            val heightFraction = (bar.valueMs.toFloat() / maxVal.toFloat()).coerceIn(0.04f, 1f)
                            val bHours = bar.valueMs / (1000 * 60 * 60)
                            val bMins = (bar.valueMs / (1000 * 60)) % 60
                            val valStr = if (bHours > 0) "${bHours}h ${bMins}m" else "${bMins}m"

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = if (bar.valueMs > 0) valStr else "",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted),
                                    maxLines = 1,
                                    overflow = TextOverflow.Visible
                                )

                                Spacer(Modifier.weight(1f))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .fillMaxHeight(heightFraction)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (bar.valueMs > 0) Brush.verticalGradient(listOf(LavenderGradientStart, BlueAccent))
                                            else Brush.verticalGradient(listOf(NavyCardInner, NavyCardInner))
                                        )
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = bar.label,
                                    style = TextStyle(fontSize = 9.sp, color = TextWhite, fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 24-Hour Hourly Timeline Card
            AppCard {
                SectionEyebrow("24-HOUR HOURLY ACTIVITY TIMELINE")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tap an hour block to inspect recorded activity during that time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Spacer(Modifier.height(14.dp))

                val maxHourly = maxOf(1L, data.hourlyDistribution.maxOrNull() ?: 1L)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Hourly Blocks Grid (2 rows of 12)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (h in 0..11) {
                            val hMs = data.hourlyDistribution.getOrElse(h) { 0L }
                            val isSel = selectedHourIndex == h
                            val intensity = if (maxHourly > 0) (hMs.toFloat() / maxHourly.toFloat()).coerceIn(0f, 1f) else 0f

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            isSel -> LavenderPrimary
                                            hMs > 0 -> BlueAccent.copy(alpha = 0.3f + intensity * 0.7f)
                                            else -> NavyCardInner
                                        }
                                    )
                                    .clickable { selectedHourIndex = if (isSel) -1 else h },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format(Locale.ROOT, "%02d", h),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSel) Color(0xFF090C15) else TextWhite
                                    )
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (h in 12..23) {
                            val hMs = data.hourlyDistribution.getOrElse(h) { 0L }
                            val isSel = selectedHourIndex == h
                            val intensity = if (maxHourly > 0) (hMs.toFloat() / maxHourly.toFloat()).coerceIn(0f, 1f) else 0f

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            isSel -> LavenderPrimary
                                            hMs > 0 -> BlueAccent.copy(alpha = 0.3f + intensity * 0.7f)
                                            else -> NavyCardInner
                                        }
                                    )
                                    .clickable { selectedHourIndex = if (isSel) -1 else h },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format(Locale.ROOT, "%02d", h),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = if (isSel) Color(0xFF090C15) else TextWhite
                                    )
                                )
                            }
                        }
                    }

                    // Hourly Detail Display
                    if (selectedHourIndex in 0..23) {
                        val selectedMs = data.hourlyDistribution.getOrElse(selectedHourIndex) { 0L }
                        val sMins = (selectedMs / (1000 * 60)) % 60
                        val sSecs = (selectedMs / 1000) % 60
                        val hourLabel = String.format(Locale.ROOT, "%02d:00 - %02d:00", selectedHourIndex, (selectedHourIndex + 1) % 24)

                        Spacer(Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(LavenderPrimary.copy(alpha = 0.15f))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Hour $hourLabel",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = if (selectedMs > 0) "${sMins}m ${sSecs}s active" else "No usage recorded",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selectedMs > 0) LimeAccent else TextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Recorded Timestamps Info Card
            val timeFmt = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }

            AppCard {
                SectionEyebrow("RECORDED USAGE TIMESTAMPS")
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("First Activity Recorded", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (data.firstRecordedTimeMs > 0L) timeFmt.format(Date(data.firstRecordedTimeMs)) else "N/A",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Last Activity Recorded", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (data.lastRecordedTimeMs > 0L) timeFmt.format(Date(data.lastRecordedTimeMs)) else "N/A",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Note: Timestamps reflect events logged by Android's UsageStatsManager service.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDimmed
                )
            }
        }
    }
}
