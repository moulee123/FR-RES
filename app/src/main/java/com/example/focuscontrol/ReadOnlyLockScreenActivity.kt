package com.example.focuscontrol

import android.app.NotificationManager
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.focuscontrol.ui.theme.RobotoFontFamily
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
        const val EXTRA_SCREEN_TYPE = "extra_screen_type"

        const val SCREEN_TYPE_STUDY_ENDED = "study_ended"
        const val SCREEN_TYPE_BREAK_ENDED = "break_ended"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "ReadOnlyLockScreenActivity onCreate() - Attempting full screen lockscreen presentation")

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

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.statusBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val prefs = FocusPreferences(this)
        prefs.lastLockScreenActivityLaunchTimeMs = System.currentTimeMillis()

        enableEdgeToEdge()

        val breakMins = intent.getIntExtra(EXTRA_BREAK_MINS, 5)

        val explicitScreenType = intent.getStringExtra(EXTRA_SCREEN_TYPE)
        val screenType = explicitScreenType ?: if (prefs.isStudyModeOnBreak()) {
            SCREEN_TYPE_BREAK_ENDED
        } else {
            SCREEN_TYPE_STUDY_ENDED
        }

        setContent {
            FocusControlTheme {
                ReadOnlyLockScreenContent(
                    screenType = screenType,
                    breakMins = breakMins,
                    onContinueStudying = {
                        Log.d(TAG, "ReadOnlyLockScreenActivity: Screen A Button 1 Selected (+5m Study Extension)")
                        prefs.addStudyExtension(5)
                        ScheduleManager.rescheduleAll(this, prefs.studySchedules)
                        finish()
                    },
                    onStartBreak = {
                        Log.d(TAG, "ReadOnlyLockScreenActivity: Screen A Button 2 Selected (Start My Break - $breakMins mins)")
                        prefs.startStudyModeBreak(breakMins)
                        ScheduleManager.rescheduleAll(this, prefs.studySchedules)
                        finish()
                    },
                    onResumeStudying = {
                        Log.d(TAG, "ReadOnlyLockScreenActivity: Screen B Button 1 Selected (Start Studying)")
                        prefs.endStudyModeBreak()
                        prefs.isStudyModeEnabled = true
                        ScheduleManager.rescheduleAll(this, prefs.studySchedules)
                        finish()
                    },
                    onEndStudyMode = {
                        Log.d(TAG, "ReadOnlyLockScreenActivity: Screen B Button 2 Selected (End Study Mode)")
                        prefs.endStudyModeBreak()
                        prefs.isStudyModeEnabled = false
                        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(2002)
                        ScheduleManager.rescheduleAll(this, prefs.studySchedules)
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "ReadOnlyLockScreenActivity onResume() - Activity is visible in foreground")
    }
}

@Composable
fun ReadOnlyLockScreenContent(
    screenType: String,
    breakMins: Int,
    onContinueStudying: () -> Unit,
    onStartBreak: () -> Unit,
    onResumeStudying: () -> Unit,
    onEndStudyMode: () -> Unit
) {
    // Dynamic 24-hour Local Time (HH:mm) for Compact Desk Clock Card
    var currentTimeStr by remember {
        val cal = Calendar.getInstance()
        mutableStateOf(String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)))
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000L)
            val cal = Calendar.getInstance()
            currentTimeStr = String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        }
    }

    // Single-click debouncing state to prevent duplicate taps
    var isActionProcessing by remember { mutableStateOf(false) }

    val handleAction = { action: () -> Unit ->
        if (!isActionProcessing) {
            isActionProcessing = true
            action()
        }
    }

    // Modern Premium Black & Dark Charcoal Palette with Restrained Blue Accents
    val darkBg = Color(0xFF0A0A0C)
    val charcoalSurface = Color(0xFF16161A)
    val charcoalCardBorder = Color(0xFF2C2C34)
    val blueAccent = Color(0xFF0A84FF)
    val blueGlowStart = Color(0xFF0A84FF)
    val textWhite = Color(0xFFFFFFFF)
    val textMuted = Color(0xFF9E9EA8)
    val secondaryButtonBg = Color(0xFF1C1C22)
    val secondaryButtonBorder = Color(0xFF34343E)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = darkBg
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF121218),
                            darkBg,
                            darkBg
                        )
                    )
                )
        ) {
            // Subtle Radial Blue Accent Lighting Effect
            Box(
                modifier = Modifier
                    .size(360.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                blueGlowStart.copy(alpha = 0.16f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP SECTION: Official Circular Logo & Brand Headings
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(Modifier.height(12.dp))

                    // Circular FRÈRES Logo Artwork
                    Image(
                        painter = painterResource(R.drawable.freres_logo),
                        contentDescription = "FRÈRES Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "F R È R E S",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textWhite.copy(alpha = 0.7f),
                        letterSpacing = 4.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    // Screen-Specific Headings & Status
                    val headingText = if (screenType == ReadOnlyLockScreenActivity.SCREEN_TYPE_BREAK_ENDED) {
                        "Break Time Ended"
                    } else {
                        "Study Interval Ended"
                    }

                    val statusText = if (screenType == ReadOnlyLockScreenActivity.SCREEN_TYPE_BREAK_ENDED) {
                        "Ready to Focus?"
                    } else {
                        "Break Due!"
                    }

                    Text(
                        text = headingText,
                        fontFamily = RobotoFontFamily,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = textWhite,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp
                    )

                    Text(
                        text = statusText,
                        fontFamily = RobotoFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = blueAccent
                    )
                }

                Spacer(Modifier.height(20.dp))

                // MIDDLE SECTION: Compact Desk Clock Card & Short Motivational Line
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Compact Live 24-Hour Desk Clock Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(charcoalSurface)
                            .border(1.dp, charcoalCardBorder, RoundedCornerShape(18.dp))
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "⏰", fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = currentTimeStr,
                                fontFamily = RobotoFontFamily,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = textWhite,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    // Short Subtle Motivational Line
                    val motivationalLine = if (screenType == ReadOnlyLockScreenActivity.SCREEN_TYPE_BREAK_ENDED) {
                        "Ready to dive back into deep focus?"
                    } else {
                        "A short break keeps you focused for longer."
                    }

                    Text(
                        text = motivationalLine,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = textMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(Modifier.height(28.dp))

                // BOTTOM SECTION: Responsive Action Buttons (Exactly Two Actions)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (screenType == ReadOnlyLockScreenActivity.SCREEN_TYPE_BREAK_ENDED) {
                        // SCREEN B ACTIONS
                        // Action 1: Start Studying (Primary Blue Solid Button)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 68.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(blueAccent)
                                .clickable { handleAction(onResumeStudying) }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "⚡", fontSize = 20.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Start Studying",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textWhite,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Action 2: End Study Mode (Dark Charcoal Outlined Button)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 68.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(secondaryButtonBg)
                                .border(1.2.dp, secondaryButtonBorder, RoundedCornerShape(18.dp))
                                .clickable { handleAction(onEndStudyMode) }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "⏹️", fontSize = 20.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "End Study Mode",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textWhite,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // SCREEN A ACTIONS
                        // Action 1: +5m Study Extension (Secondary Card Button with Blue Border)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 68.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(secondaryButtonBg)
                                .border(1.2.dp, blueAccent.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                                .clickable { handleAction(onContinueStudying) }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "⏱️+", fontSize = 20.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "+5m Study Extension",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textWhite,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Action 2: Start My Break (Primary Blue Solid Button)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 68.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(blueAccent)
                                .clickable { handleAction(onStartBreak) }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "☕", fontSize = 20.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Start My Break",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textWhite,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
