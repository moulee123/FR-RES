package com.example.focuscontrol.ui.screens

import com.example.focuscontrol.ui.theme.NavyCardInner
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.focuscontrol.ui.components.AppCard
import com.example.focuscontrol.ui.components.DigitalTimerDisplay
import com.example.focuscontrol.ui.components.LogoWordmark
import com.example.focuscontrol.ui.components.SectionEyebrow
import com.example.focuscontrol.ui.theme.LavenderGradientStart
import com.example.focuscontrol.ui.theme.LavenderPrimary
import com.example.focuscontrol.ui.theme.NavyBackground
import com.example.focuscontrol.ui.theme.NavyCardBorder
import com.example.focuscontrol.ui.theme.PinkAccent
import com.example.focuscontrol.ui.theme.PurpleAccent
import com.example.focuscontrol.ui.theme.TextMuted
import com.example.focuscontrol.ui.theme.TextWhite
import com.example.focuscontrol.FocusPreferences
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun ActiveSessionScreen(
    blockedCount: Int,
    onTake5MinExtension: () -> Unit,
    onStartBreak: () -> Unit,
    onEndSession: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { FocusPreferences(context) }
    
    var secondsRemaining by remember { mutableIntStateOf(2700) } // 45 minutes default
    var extensionsAdded by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1.seconds)
            secondsRemaining--
        }
    }

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeFormatted = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    val gradientBrush = Brush.horizontalGradient(listOf(LavenderGradientStart, PinkAccent))

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = NavyBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PurpleAccent.copy(alpha = 0.25f),
                            NavyBackground
                        ),
                        radius = 1200f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LogoWordmark(subtitle = "Active Focus Session")

                    Text(
                        text = "Close",
                        style = MaterialTheme.typography.titleMedium,
                        color = LavenderPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onClose() }
                    )
                }

                // Center Digital Timer Display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(LavenderPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⏳", style = MaterialTheme.typography.headlineLarge)
                    }

                    Spacer(Modifier.height(24.dp))

                    DigitalTimerDisplay(timeText = timeFormatted, label = "STUDY SESSION COUNTDOWN")

                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(LavenderPrimary.copy(alpha = 0.18f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$blockedCount Apps Restricted • Focus Locked",
                            style = MaterialTheme.typography.labelMedium,
                            color = LavenderPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (extensionsAdded > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "+${extensionsAdded * 5}m Study Extension Active (Clock Schedule Untouched)",
                            style = MaterialTheme.typography.labelSmall,
                            color = PinkAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bottom Action Controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppCard {
                        SectionEyebrow("STUDY MODE • 5-MINUTE EXTENSION")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Need 5 More Minutes?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Extending keeps apps blocked for 5 extra minutes without shifting future scheduled clock times.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                secondsRemaining += 300
                                extensionsAdded++
                                prefs.addStudyExtension(5)
                                onTake5MinExtension()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .background(gradientBrush, RoundedCornerShape(12.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = TextWhite
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+5 Extra Study Minutes", fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NavyCardInner)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Schedule Controlled • Session concludes automatically according to your saved schedule",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
