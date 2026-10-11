package com.example.focuscontrol

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.focuscontrol.ui.theme.DotMatrixFontFamily
import com.example.focuscontrol.ui.theme.FocusControlTheme
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale

class ReadOnlyLockScreenActivity : ComponentActivity() {

    companion object {
        const val TAG = "FRERES_BREAK_DEBUG"
        const val EXTRA_STUDY_END_MS = "extra_study_end_ms"
        const val EXTRA_BLOCKED_COUNT = "extra_blocked_count"
        const val EXTRA_BREAK_MINS = "extra_break_mins"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "BreakActivity onCreate() - Attempting full screen lockscreen presentation")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        // Hide System Status Bar completely for Immersive Full Screen Presentation
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.statusBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val prefs = FocusPreferences(this)
        prefs.lastLockScreenActivityLaunchTimeMs = System.currentTimeMillis()

        enableEdgeToEdge()

        val blockedCount = intent.getIntExtra(EXTRA_BLOCKED_COUNT, 0)
        val breakMins = intent.getIntExtra(EXTRA_BREAK_MINS, 5)

        setContent {
            FocusControlTheme {
                ReadOnlyLockScreenContent(
                    blockedCount = blockedCount,
                    breakMins = breakMins,
                    onContinueStudying = {
                        Log.d(TAG, "BreakActivity: Button 1 Selected (+5m Study Extension)")
                        prefs.addStudyExtension(5)
                        ScheduleManager.rescheduleAll(this, prefs.studySchedules)
                        finish()
                    },
                    onStartBreak = {
                        Log.d(TAG, "BreakActivity: Button 2 Selected (Start My Break - $breakMins mins)")
                        prefs.startStudyModeBreak(breakMins)
                        ScheduleManager.rescheduleAll(this, prefs.studySchedules)
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "BreakActivity onResume() - Activity is visible in foreground")
    }
}

@Composable
fun ReadOnlyLockScreenContent(
    blockedCount: Int,
    breakMins: Int,
    onContinueStudying: () -> Unit,
    onStartBreak: () -> Unit
) {
    // Dynamic 24-hour Local Time (HH:mm) for Desk Clock
    var currentTimeStr by remember {
        val cal = Calendar.getInstance()
        mutableStateOf(String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)))
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10000L)
            val cal = Calendar.getInstance()
            currentTimeStr = String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        }
    }

    // Sunset Study Room Atmosphere Palette matching reference screenshot
    val darkRoomBg = Color(0xFF140D0B)
    val amberSunsetGlow = Color(0xFFD9621E)
    val warmAmberGold = Color(0xFFFFB020)
    val creamGlowStart = Color(0xFFFFF6DF)
    val creamGlowEnd = Color(0xFFFFD59E)
    val darkGlassBg = Color(0x661E1410)
    val cardBorderGold = Color(0xFF8C532B)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = darkRoomBg
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF23140F),
                            Color(0xFF180D0A),
                            darkRoomBg
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Background Radial Sunset Light Effect
            Box(
                modifier = Modifier
                    .size(340.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                amberSunsetGlow.copy(alpha = 0.28f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP SECTION: Official Logo & Brand Headings
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Circular Glowing Badge Logo
                    Box(
                        modifier = Modifier
                            .size(105.dp)
                            .clip(CircleShape)
                            .border(BorderStroke(2.dp, warmAmberGold.copy(alpha = 0.8f)), CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(amberSunsetGlow.copy(alpha = 0.6f), Color(0xFF1F110B))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.freres_logo),
                            contentDescription = "FRÈRES Logo",
                            modifier = Modifier.size(90.dp)
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "F R È R E S",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 4.sp
                    )

                    Spacer(Modifier.height(4.dp))

                    // Heading: Study Interval Ended ≡
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Study Interval\nEnded",
                            fontFamily = DotMatrixFontFamily,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 36.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "≡",
                            fontSize = 24.sp,
                            color = warmAmberGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Break Due!",
                        fontFamily = DotMatrixFontFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = warmAmberGold
                    )

                    Text(
                        text = "Tap an action below or unlock to choose.",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // MIDDLE SECTION: Cozy Study Room Illustration Card with Desk Clock & Books
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    amberSunsetGlow.copy(alpha = 0.15f),
                                    Color(0xFF26150F)
                                )
                            )
                        )
                        .border(1.dp, cardBorderGold.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🛋️", fontSize = 28.sp)
                            // Digital Desk Clock with Device's Actual Time (HH:mm)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF120A07))
                                    .border(1.dp, warmAmberGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = currentTimeStr,
                                    fontFamily = DotMatrixFontFamily,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "SMALL STEPS\nBIG CHANGES",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = warmAmberGold,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 11.sp
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF382017))
                                    .padding(horizontal = 14.dp, vertical = 2.dp)
                            ) {
                                Text("GOOD HABITS", fontSize = 10.sp, color = warmAmberGold, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF4A261B))
                                    .padding(horizontal = 14.dp, vertical = 2.dp)
                            ) {
                                Text("BETTER FOCUS", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF26150F))
                                    .padding(horizontal = 14.dp, vertical = 2.dp)
                            ) {
                                Text("BRIGHTER YOU", fontSize = 10.sp, color = warmAmberGold, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Good Habits Brighter You • $blockedCount Apps Restricted",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // BOTTOM SECTION: Exactly Two Large Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Button 1: +5m Study Extension (Cream/Amber Glowing Card)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(95.dp)
                            .shadow(8.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(creamGlowStart, creamGlowEnd)
                                )
                            )
                            .clickable { onContinueStudying() }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "⏱️+", fontSize = 22.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "+5m",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2A1505)
                            )
                            Text(
                                text = "Study Extension",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4A280F)
                            )
                        }
                    }

                    // Button 2: Start My Break (Dark Glass Card)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(95.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(darkGlassBg)
                            .border(1.5.dp, cardBorderGold.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                            .clickable { onStartBreak() }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "☕️", fontSize = 24.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Start My Break",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
