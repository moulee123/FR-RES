package com.example.focuscontrol.ui.screens

import android.content.Context
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.focuscontrol.FocusPreferences
import com.example.focuscontrol.model.AppAllowanceRule
import com.example.focuscontrol.model.AppInfo
import com.example.focuscontrol.ui.components.AppCard
import com.example.focuscontrol.ui.components.LogoWordmark
import com.example.focuscontrol.ui.components.SectionEyebrow
import com.example.focuscontrol.ui.theme.LavenderPrimary
import com.example.focuscontrol.ui.theme.NavyCardBorder
import com.example.focuscontrol.ui.theme.NavyCardInner
import com.example.focuscontrol.ui.theme.PurpleAccent
import com.example.focuscontrol.ui.theme.TextDimmed
import com.example.focuscontrol.ui.theme.TextMuted
import com.example.focuscontrol.ui.theme.TextWhite
import com.example.focuscontrol.ui.theme.WarningGold

@Composable
fun AppsScreen(
    blockedPackages: Set<String>,
    installedApps: List<AppInfo>,
    onToggleBlockedApp: (String, Boolean) -> Unit,
    onConfigureAllowanceRule: (String) -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { FocusPreferences(context) }
    
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(searchQuery, installedApps) {
        installedApps.filter { app ->
            searchQuery.isBlank() ||
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
        }
    }
    
    var configuredRules by remember { mutableStateOf(prefs.allowanceRules) }
    
    LaunchedEffect(Unit) {
        configuredRules = prefs.allowanceRules
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        LogoWordmark(subtitle = "App Allowance Rules")

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppCard {
                    SectionEyebrow("RECURRING ALLOWANCE")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Interval-Based App Usage Rules",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Grants a recurring usage allowance per interval. Unused allowance expires at the end of each interval without carryover. When Study Mode is ON, allowance timers pause and resume when Study Mode is OFF.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }

            item {
                SectionEyebrow("CONFIGURED APP RULES (${configuredRules.size})")
            }

            items(configuredRules, key = { "rule_${it.packageName}" }) { rule ->
                val appInfo = installedApps.find { it.packageName == rule.packageName }
                AppAllowanceCard(
                    rule = rule,
                    appInfo = appInfo
                ) { onConfigureAllowanceRule(rule.packageName) }
            }
            
            item {
                Spacer(Modifier.height(4.dp))
                SectionEyebrow("ALL APPS (TAP TO CONFIGURE)")
            }
            
            items(filteredApps, key = { "app_${it.packageName}" }) { app ->
                val hasRule = configuredRules.any { it.packageName == app.packageName }
                AppAllowanceListRow(
                    app = app,
                    hasRule = hasRule,
                    onClick = { onConfigureAllowanceRule(app.packageName) }
                )
            }
        }
    }
}

@Composable
private fun AppAllowanceCard(
    rule: AppAllowanceRule,
    appInfo: AppInfo?,
    onConfigure: () -> Unit
) {
    val currentTime = System.currentTimeMillis()
    val isRuleActive = rule.isRuleActive(currentTime)

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConfigure() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (appInfo?.icon != null) {
                        Image(
                            bitmap = appInfo.icon,
                            contentDescription = rule.appName,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PurpleAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = rule.appName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = PurpleAccent,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rule.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${rule.remainingMinutes}m remaining • ${rule.timeToResetText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(LavenderPrimary.copy(alpha = 0.15f))
                        .clickable { onConfigure() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.labelMedium,
                        color = LavenderPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PurpleAccent.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (rule.intervalMinutes % 60 == 0) "${rule.allowedMinutes}m per ${rule.intervalMinutes / 60}h Interval" else "${rule.allowedMinutes}m per ${rule.intervalMinutes}m Interval",
                        style = MaterialTheme.typography.labelSmall,
                        color = PurpleAccent,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isRuleActive) WarningGold.copy(alpha = 0.2f)
                            else LavenderPrimary.copy(alpha = 0.18f)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isRuleActive) "STRICT LOCK" else "EDITABLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isRuleActive) WarningGold else LavenderPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            val progress = if (rule.allowedMinutes > 0) rule.remainingMinutes.toFloat() / rule.allowedMinutes.toFloat() else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isRuleActive && rule.remainingMinutes <= 0) WarningGold else LavenderPrimary,
                trackColor = NavyCardInner
            )
        }
    }
}

@Composable
fun AppAllowanceListRow(
    app: AppInfo,
    hasRule: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NavyCardInner)
            .clickable(onClick = onClick)
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
                    .background(PurpleAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = PurpleAccent
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

        Text(
            text = if (hasRule) "Configured" else "Add Rule",
            style = MaterialTheme.typography.labelSmall,
            color = if (hasRule) TextMuted else PurpleAccent,
            fontWeight = FontWeight.Bold
        )
    }
}
