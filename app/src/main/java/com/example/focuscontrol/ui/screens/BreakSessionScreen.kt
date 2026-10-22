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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.example.focuscontrol.ui.components.DigitalTimerDisplay
import com.example.focuscontrol.ui.components.LogoWordmark
import com.example.focuscontrol.ui.theme.BlueAccent
import com.example.focuscontrol.ui.theme.LavenderGradientStart
import com.example.focuscontrol.ui.theme.LavenderPrimary
import com.example.focuscontrol.ui.theme.NavyBackground
import com.example.focuscontrol.ui.theme.NavyCardBorder
import com.example.focuscontrol.ui.theme.PinkAccent
import com.example.focuscontrol.ui.theme.TextWhite
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun BreakSessionScreen(
    onResumeStudying: () -> Unit,
    onClose: () -> Unit,
) {
    var secondsRemaining by remember { mutableIntStateOf(300) } // 5 minutes default

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
                            BlueAccent.copy(alpha = 0.2f),
                            NavyBackground
                        ),
                        radius = 1200f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LogoWordmark(subtitle = "Study Break Interval")

                    Text(
                        text = "Close",
                        style = MaterialTheme.typography.titleMedium,
                        color = LavenderPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onClose() }
                    )
                }

                // Center Timer Display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(BlueAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("☕️", style = MaterialTheme.typography.headlineLarge)
                    }

                    Spacer(Modifier.height(24.dp))

                    DigitalTimerDisplay(timeText = timeFormatted, label = "STUDY BREAK COUNTDOWN")

                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BlueAccent.copy(alpha = 0.18f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Rest Period Active • App Blocking Paused",
                            style = MaterialTheme.typography.labelMedium,
                            color = BlueAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onResumeStudying,
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
                        Text("Resume Study Session Now", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        border = BorderStroke(1.dp, NavyCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Return to Dashboard")
                    }
                }
            }
        }
    }
}
