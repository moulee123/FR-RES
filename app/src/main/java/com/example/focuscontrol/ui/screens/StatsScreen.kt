package com.example.focuscontrol.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
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

data class AppUsageStat(
    val packageName: String,
    val appName: String,
    val timeForegroundMs: Long,
    val icon: ImageBitmap?
)

@Composable
fun StatsScreen() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(UsageStatsHelper.hasUsageStatsPermission(context)) }
    var todayUsage by remember { mutableStateOf<List<AppUsageStat>>(emptyList()) }
    var totalTimeTodayMs by remember { mutableStateOf(0L) }

    OnLifecycleEvent { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            hasPermission = UsageStatsHelper.hasUsageStatsPermission(context)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val usageMap = UsageStatsHelper.getTodayUsageStats(context)
            val pm = context.packageManager
            val statsList = mutableListOf<AppUsageStat>()
            var total = 0L

            for ((pkg, timeMs) in usageMap) {
                // Filter out some system noise trivially, only taking apps with meaningful time (> 1 min)
                if (timeMs > 60_000L) {
                    try {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        val name = pm.getApplicationLabel(appInfo).toString()
                        val iconDrawable = pm.getApplicationIcon(appInfo)
                        statsList.add(
                            AppUsageStat(
                                packageName = pkg,
                                appName = name,
                                timeForegroundMs = timeMs,
                                icon = iconDrawable.toBitmap().asImageBitmap()
                            )
                        )
                        total += timeMs
                    } catch (e: PackageManager.NameNotFoundException) {
                        // ignore unresolvable packages
                    }
                }
            }

            todayUsage = statsList.sortedByDescending { it.timeForegroundMs }
            totalTimeTodayMs = total
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        LogoWordmark(subtitle = "Usage Statistics")

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
                    text = "FRÈRES requires 'Usage Access' permission to display your daily screen time and focus analytics.",
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
                        // Note: The user will need to return to the app and the LaunchedEffect re-checks permission via lifecycle. 
                        // To force re-check, we could use a LifecycleObserver, but for simplicity we rely on standard recomposition or manual toggle.
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
            // Stats UI
            val totalHours = totalTimeTodayMs / (1000 * 60 * 60)
            val totalMins = (totalTimeTodayMs / (1000 * 60)) % 60

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppCard(modifier = Modifier.weight(1f)) {
                    SectionEyebrow("TOTAL SCREEN TIME")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${totalHours}h ${totalMins}m",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("Today", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }

                AppCard(modifier = Modifier.weight(1f)) {
                    SectionEyebrow("APPS TRACKED")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${todayUsage.size}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = LimeAccent
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("Active Today", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }

            AppCard {
                SectionEyebrow("TODAY'S USAGE BREAKDOWN")
                Spacer(Modifier.height(10.dp))
                
                if (todayUsage.isEmpty()) {
                    Text("No usage data available yet for today.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                } else {
                    val maxTime = todayUsage.firstOrNull()?.timeForegroundMs?.toFloat() ?: 1f
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        todayUsage.take(15).forEach { stat ->
                            val hours = stat.timeForegroundMs / (1000 * 60 * 60)
                            val mins = (stat.timeForegroundMs / (1000 * 60)) % 60
                            val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                            val fraction = (stat.timeForegroundMs.toFloat() / maxTime).coerceIn(0.05f, 1f)
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (stat.icon != null) {
                                    Image(
                                        bitmap = stat.icon,
                                        contentDescription = stat.appName,
                                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(LavenderPrimary.copy(alpha=0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(stat.appName.take(1), color = LavenderPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Spacer(Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(stat.appName, color = TextWhite, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Text(timeStr, color = TextMuted, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction)
                                            .height(6.dp)
                                            .clip(CircleShape)
                                            .background(Brush.horizontalGradient(listOf(LavenderGradientStart, BlueAccent)))
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
