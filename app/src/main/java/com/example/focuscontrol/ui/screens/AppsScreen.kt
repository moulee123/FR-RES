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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focuscontrol.FocusPreferences
import com.example.focuscontrol.model.AppInfo
import com.example.focuscontrol.ui.components.AppCard
import com.example.focuscontrol.ui.components.LogoWordmark
import com.example.focuscontrol.ui.components.SectionEyebrow
import com.example.focuscontrol.ui.theme.BlueAccent
import com.example.focuscontrol.ui.theme.LavenderGradientStart
import com.example.focuscontrol.ui.theme.LavenderPrimary
import com.example.focuscontrol.ui.theme.LimeAccent
import com.example.focuscontrol.ui.theme.NavyCard
import com.example.focuscontrol.ui.theme.NavyCardBorder
import com.example.focuscontrol.ui.theme.NavyCardInner
import com.example.focuscontrol.ui.theme.PinkAccent
import com.example.focuscontrol.ui.theme.TextDimmed
import com.example.focuscontrol.ui.theme.TextMuted
import com.example.focuscontrol.ui.theme.TextWhite
import kotlinx.coroutines.launch

@Composable
fun AppsScreen(
    @Suppress("UNUSED_PARAMETER") blockedPackages: Set<String>,
    installedApps: List<AppInfo>,
    @Suppress("UNUSED_PARAMETER") onToggleBlockedApp: (String, Boolean) -> Unit,
    onConfigureAllowanceRule: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { FocusPreferences(context) }

    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("All") } // "All", "Limited", "Not Limited"
    var showOverflowMenu by remember { mutableStateOf(false) }

    var configuredRules by remember { mutableStateOf(prefs.allowanceRules) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        configuredRules = prefs.allowanceRules
    }

    val activeRulesCount = configuredRules.count { it.isEnabled }
    val totalAllowedMins = configuredRules.filter { it.isEnabled }.sumOf { it.allowedMinutes }

    val filteredApps = remember(searchQuery, filterMode, installedApps, configuredRules) {
        installedApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)

            val hasRule = configuredRules.any { it.packageName == app.packageName && it.isEnabled }
            val matchesFilter = when (filterMode) {
                "Limited" -> hasRule
                "Not Limited" -> !hasRule
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SECTION A: Header with FRÈRES Branding & Overflow Icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LogoWordmark(subtitle = "App Allowance Rules")

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Search Icon: Focuses search input and scrolls down
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NavyCardInner)
                        .clickable {
                            coroutineScope.launch {
                                scrollState.animateScrollTo(scrollState.maxValue)
                                searchFocusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔍", fontSize = 16.sp)
                }

                Spacer(Modifier.width(8.dp))

                // Three-dot Overflow Menu Box
                Box {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NavyCardInner)
                            .clickable { showOverflowMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⋮", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        modifier = Modifier.background(NavyCard)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Refresh Allowance Rules", color = TextWhite) },
                            onClick = {
                                showOverflowMenu = false
                                configuredRules = prefs.allowanceRules
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Accessibility Settings", color = TextWhite) },
                            onClick = {
                                showOverflowMenu = false
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Usage Access Settings", color = TextWhite) },
                            onClick = {
                                showOverflowMenu = false
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }

        // SECTION B: App Usage Overview Grid & Explanatory Card
        AppCard {
            SectionEyebrow("APP ALLOWANCE OVERVIEW")
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Apps with limits
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavyCardInner)
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Active Limits", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(Modifier.height(2.dp))
                        Text("$activeRulesCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LavenderPrimary)
                    }
                }

                // Metric 2: Total apps installed
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavyCardInner)
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Installed Apps", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(Modifier.height(2.dp))
                        Text("${installedApps.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                }

                // Metric 3: Total allowed today
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavyCardInner)
                        .padding(10.dp)
                ) {
                    Column {
                        val hours = totalAllowedMins / 60
                        val mins = totalAllowedMins % 60
                        val totalStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                        Text("Allowed Today", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(Modifier.height(2.dp))
                        Text(totalStr, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LimeAccent)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Info Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LavenderPrimary.copy(alpha = 0.12f))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ℹ️", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "App allowances enforce non-decreasing daily usage limits that operate independently from Study Mode.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        }

        // SECTION C: Add App Rule Prominent Button
        val gradientBrush = Brush.horizontalGradient(listOf(LavenderGradientStart, PinkAccent))
        Button(
            onClick = {
                val firstUnconfigured = installedApps.firstOrNull { app ->
                    configuredRules.none { it.packageName == app.packageName }
                }?.packageName ?: installedApps.firstOrNull()?.packageName ?: ""
                if (firstUnconfigured.isNotBlank()) {
                    onConfigureAllowanceRule(firstUnconfigured)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .background(gradientBrush, RoundedCornerShape(16.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = TextWhite
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("➕", fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Add App Rule", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Choose an app and set a daily time limit", style = MaterialTheme.typography.labelSmall, color = TextWhite.copy(alpha = 0.8f))
                }
            }
        }

        // SECTION D: Configured Apps Horizontal Row
        val activeRules = configuredRules.filter { it.isEnabled }
        if (activeRules.isNotEmpty()) {
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionEyebrow("CONFIGURED RULES (${activeRules.size})")
                    Text(
                        text = "Manage All",
                        style = MaterialTheme.typography.labelMedium,
                        color = LavenderPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { filterMode = "Limited" }
                    )
                }

                Spacer(Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(activeRules, key = { it.packageName }) { rule ->
                        val app = installedApps.find { it.packageName == rule.packageName }
                        val appName = app?.appName ?: rule.appName

                        Box(
                            modifier = Modifier
                                .width(170.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(NavyCardInner)
                                .clickable { onConfigureAllowanceRule(rule.packageName) }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (app?.icon != null) {
                                        Image(
                                            bitmap = app.icon,
                                            contentDescription = appName,
                                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(LavenderPrimary.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(appName.take(1), color = LavenderPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = appName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = "${rule.allowedMinutes}m limit • ${rule.remainingMinutes}m left",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LimeAccent
                                )

                                Spacer(Modifier.height(6.dp))

                                val progress = if (rule.allowedMinutes > 0) {
                                    (rule.remainingMinutes.toFloat() / rule.allowedMinutes.toFloat()).coerceIn(0f, 1f)
                                } else 0f

                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (progress < 0.2f) PinkAccent else LimeAccent,
                                    trackColor = NavyCardBorder
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION E: All Apps List & Filter Controls
        AppCard {
            SectionEyebrow("ALL INSTALLED APPS (${filteredApps.size})")

            Spacer(Modifier.height(10.dp))

            // Search text field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search installed apps...", color = TextDimmed) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocusRequester),
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = "✕",
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    searchQuery = ""
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                                .padding(8.dp)
                        )
                    }
                },
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

            Spacer(Modifier.height(10.dp))

            // Filter chips: All, Limited, Not Limited
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("All", "Limited", "Not Limited").forEach { mode ->
                    val isSel = filterMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) LavenderPrimary else NavyCardInner)
                            .clickable { filterMode = mode }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color(0xFF090C15) else TextMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (filteredApps.isEmpty()) {
                Text(
                    text = "No apps match the selected filter or search query.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredApps.forEach { app ->
                        val rule = configuredRules.find { it.packageName == app.packageName && it.isEnabled }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NavyCardInner)
                                .clickable { onConfigureAllowanceRule(app.packageName) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (app.icon != null) {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = app.appName,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
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
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (rule != null) {
                                        "${rule.allowedMinutes}m limit • ${rule.remainingMinutes}m left"
                                    } else {
                                        "No limit set"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (rule != null) LimeAccent else TextDimmed
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (rule != null) LavenderPrimary.copy(alpha = 0.2f) else BlueAccent.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (rule != null) "Edit Limit" else "Set Limit",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rule != null) LavenderPrimary else BlueAccent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
