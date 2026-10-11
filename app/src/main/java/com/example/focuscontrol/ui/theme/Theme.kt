package com.example.focuscontrol.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun FocusControlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = appColors.primaryAccent,
            onPrimary = appColors.background,
            secondary = appColors.primaryAccent,
            onSecondary = appColors.textPrimary,
            tertiary = appColors.primaryAccent,
            onTertiary = appColors.textPrimary,
            background = appColors.background,
            onBackground = appColors.textPrimary,
            surface = appColors.card,
            onSurface = appColors.textPrimary,
            surfaceVariant = appColors.cardInner,
            onSurfaceVariant = appColors.textMuted,
        )
    } else {
        lightColorScheme(
            primary = appColors.primaryAccent,
            onPrimary = Color.White,
            secondary = appColors.primaryAccent,
            onSecondary = appColors.textPrimary,
            tertiary = appColors.primaryAccent,
            onTertiary = appColors.textPrimary,
            background = appColors.background,
            onBackground = appColors.textPrimary,
            surface = appColors.card,
            onSurface = appColors.textPrimary,
            surfaceVariant = appColors.cardInner,
            onSurfaceVariant = appColors.textMuted,
        )
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
