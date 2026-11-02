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

fun getThemePalette(paletteName: String): Pair<Color, Color> {
    return when (paletteName.uppercase()) {
        "EMERALD" -> Pair(Color(0xFF00C896), Color(0xFF004D38))
        "GOLD" -> Pair(Color(0xFFFFB703), Color(0xFF5C4000))
        "INDIGO" -> Pair(Color(0xFF6366F1), Color(0xFF2E308E))
        else -> Pair(CyanPrimary, CyanContainer)
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themePalette: String = "CYAN",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val (primaryColor, containerColor) = getThemePalette(themePalette)

    val darkScheme = darkColorScheme(
        primary = primaryColor,
        onPrimary = Color.Black,
        primaryContainer = containerColor,
        onPrimaryContainer = Color.White,
        secondary = TealSecondary,
        onSecondary = TealOnSecondary,
        secondaryContainer = TealContainer,
        onSecondaryContainer = TealOnContainer,
        tertiary = BlueTertiary,
        onTertiary = BlueOnTertiary,
        tertiaryContainer = BlueContainer,
        onTertiaryContainer = BlueOnContainer,
        background = DarkBackground,
        onBackground = TextPrimaryDark,
        surface = DarkSurface,
        onSurface = TextPrimaryDark,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = TextSecondaryDark,
        outline = DarkOutline,
        outlineVariant = DarkOutlineVariant
    )

    val lightScheme = lightColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = containerColor.copy(alpha = 0.2f),
        onPrimaryContainer = Color.Black,
        secondary = Color(0xFF008968),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFA6F5D5),
        onSecondaryContainer = Color(0xFF002117),
        tertiary = Color(0xFF006684),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFBFE9FF),
        onTertiaryContainer = Color(0xFF001F2A),
        background = LightBackground,
        onBackground = Color(0xFF141C24),
        surface = LightSurface,
        onSurface = Color(0xFF141C24),
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = Color(0xFF424F5C),
        outline = LightOutline,
        outlineVariant = LightOutlineVariant
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkScheme
        else -> lightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
