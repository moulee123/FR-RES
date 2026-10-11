package com.example.focuscontrol.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.focuscontrol.model.AppAllowanceRule
import com.example.focuscontrol.ui.components.AppCard
import com.example.focuscontrol.ui.components.LogoWordmark
import com.example.focuscontrol.ui.components.SectionEyebrow
import com.example.focuscontrol.ui.theme.BlueAccent
import com.example.focuscontrol.ui.theme.LavenderPrimary
import com.example.focuscontrol.ui.theme.NavyCardInner
import com.example.focuscontrol.ui.theme.NavyCardBorder
import com.example.focuscontrol.ui.theme.PinkAccent
import com.example.focuscontrol.ui.theme.PurpleAccent
import com.example.focuscontrol.ui.theme.TextMuted
import com.example.focuscontrol.ui.theme.TextWhite
import com.example.focuscontrol.ui.theme.WarningGold

@Composable
fun AppAllowanceEditorScreen(
    packageName: String,
    existingRule: AppAllowanceRule?,
    onSaveRule: (AppAllowanceRule) -> Unit,
    onDeleteRule: ((String) -> Unit)? = null,
    onCancel: () -> Unit
) {
    val currentTime = System.currentTimeMillis()
    val isRuleActive = existingRule?.isRuleActive(currentTime) == true

    // Default to existing values or sensible defaults
    val initialIntervalMins = existingRule?.intervalMinutes ?: 120
    val initialAllowedMins = existingRule?.allowedMinutes ?: 15

    // State for inputs
    var intervalValueText by remember { 
        mutableStateOf(if (initialIntervalMins % 60 == 0) (initialIntervalMins / 60).toString() else initialIntervalMins.toString())
    }
    var isIntervalHours by remember { mutableStateOf(initialIntervalMins % 60 == 0) }

    var durationValueText by remember { 
        mutableStateOf(if (initialAllowedMins % 60 == 0 && initialAllowedMins > 0) (initialAllowedMins / 60).toString() else initialAllowedMins.toString())
    }
    var isDurationHours by remember { mutableStateOf(initialAllowedMins % 60 == 0 && initialAllowedMins > 0) }

    var validationError by remember { mutableStateOf<String?>(null) }

    val currentActiveIntervalMinutes = existingRule?.intervalMinutes ?: initialIntervalMins
    val currentActiveAllowedMinutes = existingRule?.allowedMinutes ?: initialAllowedMins

    val appName = remember(packageName, existingRule) {
        existingRule?.appName ?: if (packageName.contains("instagram")) "Instagram"
        else if (packageName.contains("youtube")) "YouTube"
        else if (packageName.contains("musically") || packageName.contains("tiktok")) "TikTok"
        else "Selected App"
    }

    val gradientBrush = Brush.horizontalGradient(listOf(PurpleAccent, BlueAccent))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LogoWordmark(subtitle = "App Limits • Configure Rule")

            Text(
                text = "Cancel",
                style = MaterialTheme.typography.titleMedium,
                color = LavenderPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onCancel() }
            )
        }

        // Compact App Details Card
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PurpleAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = appName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }

        // Active Lock Strictness Status Card
        if (isRuleActive && existingRule != null) {
            AppCard(
                borderBrush = Brush.linearGradient(listOf(WarningGold, WarningGold))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Rule Is Locked",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarningGold.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("STRICT LOCK", style = MaterialTheme.typography.labelSmall, color = WarningGold, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "This rule has recorded usage in the active interval (${existingRule.usedForegroundTimeMs / 60000}m used). Options that weaken restrictions are locked until the next reset: ${existingRule.timeToResetText}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        // Configuration Options Card
        AppCard {
            SectionEyebrow("INTERVAL & DURATION CONFIGURATION")
            Spacer(Modifier.height(12.dp))

            // Interval Input
            Text("Recurring Reset Interval", style = MaterialTheme.typography.labelLarge, color = TextWhite)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = intervalValueText,
                    onValueChange = { intervalValueText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = NavyCardBorder,
                        focusedContainerColor = NavyCardInner,
                        unfocusedContainerColor = NavyCardInner
                    )
                )
                Spacer(Modifier.width(12.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavyCardInner)
                ) {
                    Box(
                        modifier = Modifier
                            .background(if (!isIntervalHours) PurpleAccent else Color.Transparent)
                            .clickable { isIntervalHours = false }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text("Min", color = if (!isIntervalHours) TextWhite else TextMuted, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .background(if (isIntervalHours) PurpleAccent else Color.Transparent)
                            .clickable { isIntervalHours = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text("Hrs", color = if (isIntervalHours) TextWhite else TextMuted, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Duration Input
            Text("Permitted Usage Duration Per Interval", style = MaterialTheme.typography.labelLarge, color = TextWhite)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = durationValueText,
                    onValueChange = { durationValueText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = NavyCardBorder,
                        focusedContainerColor = NavyCardInner,
                        unfocusedContainerColor = NavyCardInner
                    )
                )
                Spacer(Modifier.width(12.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavyCardInner)
                ) {
                    Box(
                        modifier = Modifier
                            .background(if (!isDurationHours) PurpleAccent else Color.Transparent)
                            .clickable { isDurationHours = false }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text("Min", color = if (!isDurationHours) TextWhite else TextMuted, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .background(if (isDurationHours) PurpleAccent else Color.Transparent)
                            .clickable { isDurationHours = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text("Hrs", color = if (isDurationHours) TextWhite else TextMuted, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            if (validationError != null) {
                Spacer(Modifier.height(8.dp))
                Text(validationError!!, color = WarningGold, style = MaterialTheme.typography.labelMedium)
            }
        }

        // Rules Policy Card
        AppCard {
            SectionEyebrow("ALLOWANCE BEHAVIOR")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "• Unused Allowance Expires\nAllowance expires at the end of each interval with no carryover.\n\n" +
                    "• Pauses During Study Sessions\nWhen Study Mode is ON, allowance timers pause and resume when Study Mode is OFF.\n\n" +
                    "• Lock Strictness\nRule settings can be made stricter but cannot be loosened once active in the current interval.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        // Save Button
        Button(
            onClick = {
                val intervalVal = intervalValueText.trim().toIntOrNull()
                val durationVal = durationValueText.trim().toIntOrNull()

                if (intervalVal == null || durationVal == null || intervalVal <= 0 || durationVal <= 0) {
                    validationError = "Please enter valid numbers greater than 0."
                    return@Button
                }

                val finalIntervalMins = if (isIntervalHours) intervalVal * 60 else intervalVal
                val finalDurationMins = if (isDurationHours) durationVal * 60 else durationVal

                if (finalDurationMins >= finalIntervalMins) {
                    validationError = "Permitted usage must be less than the reset interval."
                    return@Button
                }
                
                // Strictness check
                if (isRuleActive) {
                    if (finalDurationMins > currentActiveAllowedMinutes) {
                        validationError = "Cannot increase allowed time while the rule is locked/active."
                        return@Button
                    }
                    if (finalIntervalMins < currentActiveIntervalMinutes) {
                        validationError = "Cannot shorten the reset interval while the rule is locked/active."
                        return@Button
                    }
                }

                validationError = null

                val updated = AppAllowanceRule(
                    packageName = packageName,
                    appName = appName,
                    intervalMinutes = finalIntervalMins,
                    allowedMinutes = finalDurationMins,
                    remainingMinutes = finalDurationMins,
                    timeToResetText = if (finalIntervalMins % 60 == 0) "Resets in ${finalIntervalMins / 60}h" else "Resets in ${finalIntervalMins}m",
                    isEnabled = true,
                    intervalStartTimeMs = existingRule?.intervalStartTimeMs ?: currentTime,
                    usedForegroundTimeMs = existingRule?.usedForegroundTimeMs ?: 0L,
                    originalIntervalMinutes = if (isRuleActive) currentActiveIntervalMinutes else finalIntervalMins,
                    originalAllowedMinutes = if (isRuleActive) currentActiveAllowedMinutes else finalDurationMins,
                    studyModePausedTimeMs = existingRule?.studyModePausedTimeMs ?: 0L
                )
                onSaveRule(updated)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(gradientBrush, RoundedCornerShape(14.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = TextWhite
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Save Allowance Rule", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        // Delete Button (Disabled if rule is active)
        if (existingRule != null && onDeleteRule != null) {
            OutlinedButton(
                onClick = {
                    if (!isRuleActive) {
                        onDeleteRule(packageName)
                    }
                },
                enabled = !isRuleActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                border = BorderStroke(1.dp, if (!isRuleActive) PinkAccent else NavyCardInner),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (!isRuleActive) PinkAccent else TextMuted.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (!isRuleActive) "🗑 Delete Allowance Rule" else "🔒 Delete Locked Until Reset",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
