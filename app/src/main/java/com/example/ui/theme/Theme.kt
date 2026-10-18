package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PakhiPrimaryDark,
    onPrimary = Color(0xFF003548),
    primaryContainer = Color(0xFF004D67),
    onPrimaryContainer = Color(0xFFC2E8FF),
    secondary = PakhiPrimaryAccentDark,
    onSecondary = Color(0xFF1D2878),
    secondaryContainer = Color(0xFF354090),
    onSecondaryContainer = Color(0xFFE0E0FF),
    background = PakhiBackgroundDark,
    onBackground = PakhiTextDark,
    surface = PakhiSurfaceDark,
    onSurface = PakhiTextDark,
    surfaceVariant = PakhiSurfaceVariantDark,
    onSurfaceVariant = PakhiTextMutedDark,
    outline = Color(0xFF334155)
)

private val LightColorScheme = lightColorScheme(
    primary = PakhiPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC7E7FF),
    onPrimaryContainer = Color(0xFF001E2E),
    secondary = PakhiPrimaryAccentLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0FF),
    onSecondaryContainer = Color(0xFF121A64),
    background = PakhiBackgroundLight,
    onBackground = PakhiTextLight,
    surface = PakhiSurfaceLight,
    onSurface = PakhiTextLight,
    surfaceVariant = PakhiSurfaceVariantLight,
    onSurfaceVariant = PakhiTextMutedLight,
    outline = Color(0xFFCBD5E1)
)

@Composable
fun PakhiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve brand palette by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PakhiTypography,
        content = content
    )
}
