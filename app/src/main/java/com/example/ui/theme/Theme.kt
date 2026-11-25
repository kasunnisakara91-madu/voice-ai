package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AssistantDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianBlack,
    primaryContainer = DeepViolet,
    onPrimaryContainer = TextPrimaryDark,
    secondary = ElectricPurple,
    onSecondary = ObsidianBlack,
    secondaryContainer = ElevatedCard,
    onSecondaryContainer = NeonCyanSoft,
    tertiary = PlasmaPink,
    onTertiary = Color.White,
    background = ObsidianBlack,
    onBackground = TextPrimaryDark,
    surface = DeepVoidPurple,
    onSurface = TextPrimaryDark,
    surfaceVariant = MidnightCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = NeonStroke,
    error = CrimsonGlow,
    onError = Color.White
)

private val AssistantLightColorScheme = lightColorScheme(
    primary = RoyalPurple,
    onPrimary = Color.White,
    primaryContainer = CloudSurfaceVariant,
    onPrimaryContainer = TextPrimaryLight,
    secondary = DeepCyanLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = PlasmaPink,
    onTertiary = Color.White,
    background = CloudVioletBg,
    onBackground = TextPrimaryLight,
    surface = CloudSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = CloudSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFD4C9F5),
    error = CrimsonGlow,
    onError = Color.White
)

private val AssistantShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun AssistantVoiceTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AssistantDarkColorScheme else AssistantLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AssistantTypography,
        shapes = AssistantShapes,
        content = content
    )
}
