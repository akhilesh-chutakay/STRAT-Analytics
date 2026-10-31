package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val SophisticatedDarkColorScheme = darkColorScheme(
    primary = SophisticatedPrimary,
    onPrimary = SophisticatedOnPrimary,
    primaryContainer = SophisticatedPrimaryContainer,
    onPrimaryContainer = SophisticatedOnPrimaryContainer,
    secondary = SophisticatedSecondary,
    onSecondary = SophisticatedOnSecondary,
    secondaryContainer = SophisticatedSecondaryContainer,
    onSecondaryContainer = SophisticatedOnSecondaryContainer,
    tertiary = SophisticatedTertiary,
    onTertiary = SophisticatedOnTertiary,
    tertiaryContainer = SophisticatedTertiaryContainer,
    onTertiaryContainer = SophisticatedOnTertiaryContainer,
    background = SophisticatedBackground,
    onBackground = SophisticatedOnSurface,
    surface = SophisticatedSurface,
    onSurface = SophisticatedOnSurface,
    surfaceVariant = SophisticatedSurfaceVariant,
    onSurfaceVariant = SophisticatedOnSurfaceVariant,
    outline = SophisticatedOutline,
    outlineVariant = SophisticatedBorderSubtle,
    surfaceTint = SophisticatedPrimary
)

val SophisticatedLightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SophisticatedSecondary,
    onSecondary = SophisticatedOnSecondary,
    secondaryContainer = SophisticatedSecondaryContainer,
    onSecondaryContainer = SophisticatedOnSecondaryContainer,
    tertiary = SophisticatedTertiary,
    onTertiary = SophisticatedOnTertiary,
    tertiaryContainer = SophisticatedTertiaryContainer,
    onTertiaryContainer = SophisticatedOnTertiaryContainer,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = SophisticatedBorderSubtle,
    surfaceTint = PrimaryLight
)

val SophisticatedShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Sophisticated Dark is the primary signature theme
    dynamicColor: Boolean = false, // Use our intentional curated Sophisticated Dark design palette
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) SophisticatedDarkColorScheme else SophisticatedLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SophisticatedShapes,
        content = content
    )
}

