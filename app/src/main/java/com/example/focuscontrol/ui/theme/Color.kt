package com.example.focuscontrol.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val card: Color,
    val cardInner: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val textDimmed: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val warningGold: Color
)

val LightAppColors = AppColors(
    isDark = false,
    background = Color(0xFFF7F6F2),
    card = Color(0xFFFFFFFF),
    cardInner = Color(0xFFF0EFEA),
    cardBorder = Color(0xFFE2E0D8),
    textPrimary = Color(0xFF151515),
    textMuted = Color(0xFF666666),
    textDimmed = Color(0xFF888888),
    primaryAccent = Color(0xFF3563F5),
    secondaryAccent = Color(0xFFE4E5FF),
    warningGold = Color(0xFFD97706)
)

val DarkAppColors = AppColors(
    isDark = true,
    background = Color(0xFF121318),
    card = Color(0xFF1C1D24),
    cardInner = Color(0xFF252732),
    cardBorder = Color(0xFF303342),
    textPrimary = Color(0xFFF0F2F8),
    textMuted = Color(0xFFA0A5B5),
    textDimmed = Color(0xFF6E7485),
    primaryAccent = Color(0xFF5B7FFF),
    secondaryAccent = Color(0xFF212B4C),
    warningGold = Color(0xFFFBBF24)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

// Dynamic Composable color getters
val NavyBackground: Color
    @Composable
    get() = LocalAppColors.current.background

val NavyCard: Color
    @Composable
    get() = LocalAppColors.current.card

val NavyCardInner: Color
    @Composable
    get() = LocalAppColors.current.cardInner

val NavyCardBorder: Color
    @Composable
    get() = LocalAppColors.current.cardBorder

val LavenderPrimary: Color
    @Composable
    get() = LocalAppColors.current.primaryAccent

val LavenderGradientStart: Color
    @Composable
    get() = LocalAppColors.current.primaryAccent

val PurpleAccent: Color
    @Composable
    get() = LocalAppColors.current.primaryAccent

val PinkAccent: Color
    @Composable
    get() = LocalAppColors.current.primaryAccent

val BlueAccent: Color
    @Composable
    get() = LocalAppColors.current.primaryAccent

val LimeAccent: Color
    @Composable
    get() = LocalAppColors.current.primaryAccent

val WarningGold: Color
    @Composable
    get() = LocalAppColors.current.warningGold

val LavenderSecondary: Color
    @Composable
    get() = LocalAppColors.current.secondaryAccent

val TextWhite: Color
    @Composable
    get() = LocalAppColors.current.textPrimary

val TextMuted: Color
    @Composable
    get() = LocalAppColors.current.textMuted

val TextDimmed: Color
    @Composable
    get() = LocalAppColors.current.textDimmed

// Reusable Brush Gradients
val LavenderPinkGradient: Brush
    @Composable
    get() = Brush.horizontalGradient(listOf(LavenderPrimary, LavenderPrimary))

val PurpleBlueGradient: Brush
    @Composable
    get() = Brush.horizontalGradient(listOf(LavenderPrimary, LavenderPrimary))

val LavenderBlueGradient: Brush
    @Composable
    get() = Brush.horizontalGradient(listOf(LavenderPrimary, LavenderPrimary))

val NavyCanvasGradient: Brush
    @Composable
    get() = Brush.verticalGradient(listOf(NavyBackground, NavyBackground))

// Legacy color aliases
val FocusBackground: Color
    @Composable
    get() = NavyBackground

val FocusSurface: Color
    @Composable
    get() = NavyCard

val FocusSurfaceVariant: Color
    @Composable
    get() = NavyCardInner

val FocusOnSurface: Color
    @Composable
    get() = TextWhite

val FocusLime: Color
    @Composable
    get() = LavenderPrimary
