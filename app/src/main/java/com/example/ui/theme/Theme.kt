package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = SagePale,
    onPrimaryContainer = ForestGreen,
    secondary = Terracotta,
    onSecondary = Color.White,
    secondaryContainer = WarmGold.copy(alpha = 0.28f),
    onSecondaryContainer = RoastedBrown,
    tertiary = EmeraldAccent,
    onTertiary = Color.White,
    tertiaryContainer = NoticeSand,
    onTertiaryContainer = NoticeText,
    background = WarmCream,
    onBackground = DeepInk,
    surface = SoftCreamSurface,
    onSurface = DeepInk,
    surfaceVariant = SurfaceTinted,
    onSurfaceVariant = MutedOlive,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = SoftCreamSurface,
    surfaceContainer = SurfaceTinted,
    surfaceContainerHigh = SagePale,
    outline = SandBorder,
    outlineVariant = SandBorder.copy(alpha = 0.7f),
    error = DangerClay,
    onError = Color.White,
    errorContainer = DangerSurface,
    onErrorContainer = DangerClay,
)

private val DarkColorScheme = darkColorScheme(
    primary = MintLight,
    onPrimary = ForestGreen,
    primaryContainer = DarkForestContainer,
    onPrimaryContainer = SagePale,
    secondary = GoldLight,
    onSecondary = RoastedBrown,
    secondaryContainer = Terracotta.copy(alpha = 0.25f),
    onSecondaryContainer = GoldLight,
    tertiary = EmeraldAccent,
    onTertiary = Color.White,
    background = DarkForestBackground,
    onBackground = Color(0xFFEAF1E9),
    surface = DarkForestSurface,
    onSurface = Color(0xFFEAF1E9),
    surfaceVariant = DarkForestContainer,
    onSurfaceVariant = Color(0xFFA5BCB0),
    surfaceContainerLowest = Color(0xFF0A110E),
    surfaceContainerLow = DarkForestSurface,
    surfaceContainer = DarkForestContainer,
    surfaceContainerHigh = Color(0xFF284035),
    outline = Color(0xFF2E473B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

val AlbanCafeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun AlbanCafeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AlbanCafeShapes,
        content = content,
    )
}
