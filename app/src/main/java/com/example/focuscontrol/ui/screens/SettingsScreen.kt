package com.example.focuscontrol.ui.screens

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.location.Location
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import java.util.Locale
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.example.focuscontrol.FocusBlockerService
import com.example.focuscontrol.FocusPreferences
import com.example.focuscontrol.ScheduleManager
import com.example.focuscontrol.ScheduleReceiver
import com.example.focuscontrol.admin.DeviceOwnerHelper
import com.example.focuscontrol.admin.DeviceOwnerStatus
import com.example.focuscontrol.ui.components.AppCard
import com.example.focuscontrol.ui.components.LogoWordmark
import com.example.focuscontrol.ui.components.SectionEyebrow
import com.example.focuscontrol.ui.theme.LavenderGradientStart
import com.example.focuscontrol.ui.theme.LavenderPrimary
import com.example.focuscontrol.ui.theme.LimeAccent
import com.example.focuscontrol.ui.theme.NavyCardInner
import com.example.focuscontrol.ui.theme.PinkAccent
import com.example.focuscontrol.ui.theme.PurpleAccent
import com.example.focuscontrol.ui.theme.TextDimmed
import com.example.focuscontrol.ui.theme.TextMuted
import com.example.focuscontrol.ui.theme.TextWhite
import com.example.focuscontrol.ui.theme.WarningGold
import com.example.focuscontrol.utils.LocationVerificationHelper

@Composable
fun SettingsScreen(
    context: Context,
    isServiceEnabled: Boolean
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var breakRemindersEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        LogoWordmark(subtitle = "App Settings & Governance")

        // Accessibility Service Card
        AppCard(
            borderBrush = Brush.linearGradient(
                if (isServiceEnabled) listOf(LimeAccent, LavenderPrimary) else listOf(WarningGold, LavenderPrimary)
            )
        ) {
            SectionEyebrow("PERMISSIONS & ACCESSIBILITY")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Accessibility Service",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isServiceEnabled) "Active • FRÈRES can detect restricted apps." else "Action Required • Tap to enable in Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isServiceEnabled) LimeAccent.copy(alpha = 0.2f) else WarningGold.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isServiceEnabled) "ACTIVE" else "SETUP",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isServiceEnabled) LimeAccent else WarningGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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
                    .height(46.dp)
                    .background(
                        Brush.horizontalGradient(listOf(LavenderGradientStart, PinkAccent)),
                        RoundedCornerShape(12.dp)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextWhite
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Open Accessibility Settings", fontWeight = FontWeight.Bold)
            }
        }

        // Full Screen Intent Permission Card
        AppCard {
            SectionEyebrow("FULL-SCREEN BREAK REMINDER PERMISSION")
            Spacer(Modifier.height(8.dp))
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val isFullScreenAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                notificationManager.canUseFullScreenIntent()
            } else true

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Full Screen Intent Access",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isFullScreenAllowed) "Allowed • Break screen can show on lockscreen." else "Disallowed • Tap below to grant permission.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isFullScreenAllowed) LimeAccent.copy(alpha = 0.2f) else WarningGold.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isFullScreenAllowed) "ALLOWED" else "DENIED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFullScreenAllowed) LimeAccent else WarningGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isFullScreenAllowed) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                Uri.parse("package:${context.packageName}")
                            ).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .background(
                            Brush.horizontalGradient(listOf(LavenderGradientStart, PinkAccent)),
                            RoundedCornerShape(12.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = TextWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Full Screen Permission", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Appear on Top Permission Card (for Over-App Popups when unlocked)
        AppCard {
            SectionEyebrow("APPEAR ON TOP (FULL-SCREEN OVER OTHER APPS)")
            Spacer(Modifier.height(8.dp))
            val isOverlayAllowed = Settings.canDrawOverlays(context)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Appear on Top Permission",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isOverlayAllowed) "Allowed • Break screen can pop up over active apps." else "Optional • Grant to allow full-screen popup while using other apps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isOverlayAllowed) LimeAccent.copy(alpha = 0.2f) else WarningGold.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isOverlayAllowed) "ALLOWED" else "OPTIONAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOverlayAllowed) LimeAccent else WarningGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isOverlayAllowed) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .background(
                            Brush.horizontalGradient(listOf(LavenderGradientStart, PinkAccent)),
                            RoundedCornerShape(12.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = TextWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Appear on Top Permission", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Device Owner & Uninstall Protection Card
        AppCard {
            SectionEyebrow("DEVICE OWNER & UNINSTALL PROTECTION")
            Spacer(Modifier.height(8.dp))
            val doStatus = DeviceOwnerHelper.getDeviceOwnerStatus(context)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device Owner Protection",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when (doStatus) {
                            DeviceOwnerStatus.ENFORCED ->
                                "Active • Hardware-level uninstall protection is enforced by Device Owner policy."
                            DeviceOwnerStatus.PROVISIONED_POLICY_PENDING ->
                                "Pending • App is Device Owner, but uninstall protection policy is not yet confirmed active."
                            DeviceOwnerStatus.NOT_PROVISIONED ->
                                "Inactive • App running in normal installation mode. Uninstall protection requires Device Owner provisioning."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (doStatus) {
                                DeviceOwnerStatus.ENFORCED -> LimeAccent.copy(alpha = 0.2f)
                                else -> WarningGold.copy(alpha = 0.2f)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when (doStatus) {
                            DeviceOwnerStatus.ENFORCED -> "ENFORCED"
                            DeviceOwnerStatus.PROVISIONED_POLICY_PENDING -> "PENDING"
                            DeviceOwnerStatus.NOT_PROVISIONED -> "INACTIVE"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (doStatus) {
                            DeviceOwnerStatus.ENFORCED -> LimeAccent
                            else -> WarningGold
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "• Normal Installation Mode: Standard permissions (Accessibility & Exact Alarms) manage focus sessions and app allowances, but CANNOT prevent app uninstallation.\n\n" +
                        "• Device Owner Mode: Grants hardware-level anti-uninstall protection via setUninstallBlocked(). Activation requires provisioning a newly reset device via ADB (`adb shell dpm set-device-owner ...`) or QR code.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        // Section 11: SYSTEM DIAGNOSTICS CARD
        AppCard {
            SectionEyebrow("SYSTEM DIAGNOSTICS & RUNTIME STATUS")
            Spacer(Modifier.height(8.dp))
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val prefs = remember { FocusPreferences(context) }

            val canExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true
            val notifEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            val breakChannel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) notificationManager.getNotificationChannel("freres_break_reminder_v2") else null
            val breakChannelImportance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                breakChannel?.importance?.toString() ?: "Created on next break"
            } else "N/A"
            val canFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) notificationManager.canUseFullScreenIntent() else true
            val canDrawOverlays = Settings.canDrawOverlays(context)
            val isDeviceOwner = DeviceOwnerHelper.isDeviceOwner(context)
            val isInteractive = powerManager.isInteractive
            val isKeyguardLocked = keyguardManager.isKeyguardLocked
            val nowMs = System.currentTimeMillis()

            val lastNotif = if (prefs.lastBreakNotificationTimeMs > 0) "${(nowMs - prefs.lastBreakNotificationTimeMs) / 1000}s ago" else "None"
            val lastAttempt = if (prefs.lastFullScreenAttemptTimeMs > 0) "${(nowMs - prefs.lastFullScreenAttemptTimeMs) / 1000}s ago" else "None"
            val lastLaunch = if (prefs.lastLockScreenActivityLaunchTimeMs > 0) "${(nowMs - prefs.lastLockScreenActivityLaunchTimeMs) / 1000}s ago" else "None"

            Text(
                text = "• Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                        "• Compile / Target SDK: 37 / 37\n" +
                        "• Exact Alarm Allowed: $canExactAlarm\n" +
                        "• Notifications Enabled: $notifEnabled\n" +
                        "• Break Channel Importance: $breakChannelImportance\n" +
                        "• canUseFullScreenIntent(): $canFullScreen\n" +
                        "• canDrawOverlays (Appear on Top): $canDrawOverlays\n" +
                        "• Device Owner Status: $isDeviceOwner\n" +
                        "• Accessibility Service Active: $isServiceEnabled\n" +
                        "• Screen Currently Interactive: $isInteractive\n" +
                        "• Device Keyguard Locked: $isKeyguardLocked\n" +
                        "• Study Mode Active: ${prefs.isStudyModeEnabled}\n" +
                        "• Study On Break: ${prefs.isStudyModeOnBreak()}\n" +
                        "• Last Break Notification: $lastNotif\n" +
                        "• Last FullScreen Intent Attempt: $lastAttempt\n" +
                        "• Last Activity Launch: $lastLaunch\n" +
                        "• Screen-Off Lockscreen Launch: Full-Screen Break Activity\n" +
                        "• Screen-On Active-App Launch: " + (if (canDrawOverlays) "Full-Screen Activity (Appear on Top)" else "Heads-Up Notification Banner"),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        // Samsung Battery Guidance Card
        AppCard {
            SectionEyebrow("SAMSUNG & BATTERY GUIDANCE")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Background Optimization",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "To ensure Samsung One UI does not kill the Accessibility Service in the background, set FRÈRES to 'Unrestricted' in App Info > Battery.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        // Notification Preferences Card
        AppCard {
            SectionEyebrow("NOTIFICATIONS & REMINDERS")
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Session Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextWhite)
                    Text("Alerts when a study session starts or ends", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = TextWhite, checkedTrackColor = LavenderPrimary)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Break Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextWhite)
                    Text("Notify when 5-min study breaks conclude", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
                Switch(
                    checked = breakRemindersEnabled,
                    onCheckedChange = { breakRemindersEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = TextWhite, checkedTrackColor = LavenderPrimary)
                )
            }
        }

        val prefs = remember { FocusPreferences(context) }
        var isHomeVerified by remember { mutableStateOf(prefs.isHomeVerified) }
        var isFetchingHomeLoc by remember { mutableStateOf(false) }
        var homeLocError by remember { mutableStateOf<String?>(null) }

        // Home Location Confirmation Dialog state
        var showHomeConfirmDialog by remember { mutableStateOf(false) }
        var pendingHomeLoc by remember { mutableStateOf<Location?>(null) }
        var pendingHomeName by remember { mutableStateOf("") }
        var pendingRadiusKm by remember { mutableStateOf(prefs.homeRadiusKm) }

        var travelModeEnabled by remember { mutableStateOf(prefs.isTravelModeEnabled) }
        val isConfirmed = remember(prefs.lastTravelConfirmationTimeMs, prefs.isHomeVerified) { prefs.isTravelModeConfirmed() }
        val isOverdue = remember(prefs.lastTravelConfirmationTimeMs, prefs.isHomeVerified) { prefs.isTravelConfirmationOverdue() }
        var isVerifyingTravel by remember { mutableStateOf(false) }
        var verificationResult by remember { mutableStateOf<String?>(null) }

        // 1. Home Location Setup Card
        AppCard {
            SectionEyebrow("HOME LOCATION SETUP")
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Home Region", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextWhite)
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isHomeVerified) LimeAccent.copy(alpha = 0.2f)
                                    else WarningGold.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isHomeVerified) "VERIFIED" else "SETUP REQUIRED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isHomeVerified) LimeAccent else WarningGold
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (isHomeVerified) {
                        Text(
                            text = "${prefs.homeLocationName} (${prefs.homeRadiusKm.toInt()} km radius)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                        Text(
                            text = "Coords: ${String.format(Locale.getDefault(), "%.4f, %.4f", prefs.homeLatitude, prefs.homeLongitude)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    } else {
                        Text(
                            text = "Travel Mode requires a verified home location to determine when you are traveling. Acquire your current location to set your home region.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (isFetchingHomeLoc) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = LavenderPrimary, strokeWidth = 2.dp)
                    Text("Acquiring fresh location fix...", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
                
                LaunchedEffect(Unit) {
                    LocationVerificationHelper.getCurrentLocationFix(context) { location, statusMsg ->
                        isFetchingHomeLoc = false
                        if (location != null) {
                            val name = LocationVerificationHelper.getGeocodedAddressName(context, location.latitude, location.longitude)
                            pendingHomeLoc = location
                            pendingHomeName = name
                            pendingRadiusKm = prefs.homeRadiusKm
                            homeLocError = null
                            showHomeConfirmDialog = true
                        } else {
                            homeLocError = statusMsg
                        }
                    }
                }
            } else {
                Button(
                    onClick = { isFetchingHomeLoc = true; homeLocError = null },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHomeVerified) NavyCardInner else LavenderPrimary,
                        contentColor = if (isHomeVerified) LavenderPrimary else TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isHomeVerified) "📍 Change Home Location" else "📍 Use Current Location as Home",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (homeLocError != null) {
                Spacer(Modifier.height(8.dp))
                Text(homeLocError!!, color = PinkAccent, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Home Location Confirmation Dialog
        if (showHomeConfirmDialog && pendingHomeLoc != null) {
            AlertDialog(
                onDismissRequest = { showHomeConfirmDialog = false },
                title = {
                    Text("Confirm Home Location", fontWeight = FontWeight.Bold, color = TextWhite)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Detected Location:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            text = pendingHomeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Coordinates: ${String.format(Locale.getDefault(), "%.4f, %.4f", pendingHomeLoc!!.latitude, pendingHomeLoc!!.longitude)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDimmed
                        )
                        
                        Spacer(Modifier.height(6.dp))
                        Text("Select Home Radius:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15.0, 30.0, 50.0).forEach { radius ->
                                val isSelected = pendingRadiusKm == radius
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) LavenderPrimary else NavyCardInner)
                                        .clickable { pendingRadiusKm = radius }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${radius.toInt()} km",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) TextWhite else TextMuted,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Travel Mode will only operate when you are outside this ${pendingRadiusKm.toInt()} km radius from home.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val loc = pendingHomeLoc!!
                            prefs.homeLatitude = loc.latitude
                            prefs.homeLongitude = loc.longitude
                            prefs.homeRadiusKm = pendingRadiusKm
                            prefs.homeLocationName = pendingHomeName
                            prefs.isHomeVerified = true
                            prefs.homeVerificationTimestamp = System.currentTimeMillis()
                            
                            isHomeVerified = true
                            showHomeConfirmDialog = false
                            homeLocError = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary, contentColor = TextWhite),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Confirm as Home Location", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showHomeConfirmDialog = false },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }
                },
                containerColor = NavyCardInner,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 2. Travel Mode Card
        AppCard {
            SectionEyebrow("SPECIAL MODES")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Travel Mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextWhite)
                        Spacer(Modifier.width(8.dp))
                        if (travelModeEnabled) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            !isHomeVerified -> WarningGold.copy(alpha = 0.2f)
                                            isOverdue -> WarningGold.copy(alpha = 0.2f)
                                            isConfirmed -> LimeAccent.copy(alpha = 0.2f)
                                            else -> Color.Gray.copy(alpha = 0.2f)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when {
                                        !isHomeVerified -> "SETUP HOME FIRST"
                                        isOverdue -> "OVERDUE"
                                        isConfirmed -> "CONFIRMED"
                                        else -> "PENDING"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        !isHomeVerified || isOverdue -> WarningGold
                                        isConfirmed -> LimeAccent
                                        else -> TextMuted
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isHomeVerified) {
                            "Suspend Study Mode limits while traveling outside ${prefs.homeLocationName} (${prefs.homeRadiusKm.toInt()} km radius). Requires 2-hour location verification."
                        } else {
                            "Set and verify your home location above first before activating Travel Mode."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = travelModeEnabled,
                    enabled = isHomeVerified,
                    onCheckedChange = { isEnabled ->
                        if (!isHomeVerified) {
                            verificationResult = "Please set and verify your home location above first."
                            return@Switch
                        }
                        travelModeEnabled = isEnabled
                        prefs.isTravelModeEnabled = isEnabled
                        if (!isEnabled) {
                            prefs.lastTravelConfirmationTimeMs = 0L
                            prefs.lastTravelLocationName = "Not Verified"
                            verificationResult = null
                        } else {
                            isVerifyingTravel = true
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = TextWhite, checkedTrackColor = PurpleAccent)
                )
            }

            if (travelModeEnabled && isHomeVerified) {
                Spacer(Modifier.height(14.dp))
                val timeRemaining = if (isConfirmed) {
                    val remainingMs = (prefs.lastTravelConfirmationTimeMs + 2 * 60 * 60 * 1000L) - System.currentTimeMillis()
                    val hours = remainingMs / (60 * 60 * 1000L)
                    val mins = (remainingMs / (60 * 1000L)) % 60
                    if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                } else "0m"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavyCardInner)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Current Location: ${prefs.lastTravelLocationName}", color = TextWhite, style = MaterialTheme.typography.labelMedium)
                        if (isConfirmed) {
                            Text("Next check required in $timeRemaining", color = TextDimmed, style = MaterialTheme.typography.labelSmall)
                        } else if (isOverdue) {
                            Text("Verification expired. Limits restored.", color = WarningGold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    
                    if (isVerifyingTravel) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PurpleAccent,
                            strokeWidth = 2.dp
                        )
                        LaunchedEffect(Unit) {
                            LocationVerificationHelper.checkIsOutsideHome(
                                context = context,
                                homeLat = prefs.homeLatitude,
                                homeLng = prefs.homeLongitude,
                                homeRadiusKm = prefs.homeRadiusKm
                            ) { isOutside, locDesc, _ ->
                                isVerifyingTravel = false
                                if (isOutside) {
                                    prefs.lastTravelLocationName = locDesc
                                    prefs.lastTravelConfirmationTimeMs = System.currentTimeMillis()
                                    ScheduleManager.scheduleTravelModeCheck(context, System.currentTimeMillis() + 2 * 60 * 60 * 1000L)
                                    verificationResult = "Verified Outside Home: $locDesc"
                                } else {
                                    prefs.lastTravelLocationName = locDesc
                                    prefs.lastTravelConfirmationTimeMs = 0L // Revoke confirmation
                                    verificationResult = if (locDesc.contains("Permission") || locDesc.contains("Disabled") || locDesc.contains("Timeout") || locDesc.contains("Error")) {
                                        "Error: $locDesc"
                                    } else {
                                        "Travel Mode denied: You are inside your home radius ($locDesc)."
                                    }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { isVerifyingTravel = true; verificationResult = null },
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent.copy(alpha = 0.2f), contentColor = PurpleAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isConfirmed) "Refresh" else "Verify Location", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (verificationResult != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = verificationResult!!,
                    color = if (verificationResult!!.startsWith("Verified")) LimeAccent else PinkAccent,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // About Card
        AppCard {
            SectionEyebrow("ABOUT")
            Spacer(Modifier.height(8.dp))
            Text("FRÈRES", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(Modifier.height(2.dp))
            Text("Version 1.0.0 • Production Build", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
    }
}
