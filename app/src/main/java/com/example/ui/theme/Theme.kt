package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = GoldSun,
    onPrimary = SkyfallMidnight,
    primaryContainer = GoldAmber,
    onPrimaryContainer = SkyfallMidnight,
    secondary = CyanIce,
    onSecondary = SkyfallMidnight,
    secondaryContainer = SkyfallSlateCard,
    onSecondaryContainer = CyanIce,
    tertiary = NeonViolet,
    onTertiary = Color.White,
    background = SkyfallMidnight,
    onBackground = TextPrimaryDark,
    surface = SkyfallDeepIndigo,
    onSurface = TextPrimaryDark,
    surfaceVariant = SkyfallSlateCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = SkyfallGlassBorder,
    error = CrimsonDanger,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = GoldAmber,
    onPrimary = Color.White,
    primaryContainer = GoldSun,
    onPrimaryContainer = SkyfallInkDark,
    secondary = CyanIce,
    onSecondary = SkyfallInkDark,
    secondaryContainer = SkyfallSoftSurface,
    onSecondaryContainer = SkyfallInkDark,
    tertiary = NeonViolet,
    onTertiary = Color.White,
    background = SkyfallCloudWhite,
    onBackground = TextPrimaryLight,
    surface = Color.White,
    onSurface = TextPrimaryLight,
    surfaceVariant = SkyfallSoftSurface,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1),
    error = CrimsonDanger,
    onError = Color.White
)

val SkyfallShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun TumbleTowerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = SkyfallShapes,
        content = content
    )
}
