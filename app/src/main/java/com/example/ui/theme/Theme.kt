package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF818CF8), // Luminous modern Indigo
    onPrimary = Color(0xFF090D16),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF34D399), // Emerald
    onSecondary = Color(0xFF064E3B),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFFFBBF24), // Amber
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = ObsidianDarkBg,
    surface = ObsidianDarkSurface,
    onBackground = Slate50,
    onSurface = Slate50,
    surfaceVariant = ObsidianDarkSurfaceElevated,
    onSurfaceVariant = Slate400,
    outline = ObsidianDarkBorder,
    outlineVariant = Slate700
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF4F46E5), // Indigo 600
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = Color(0xFF059669), // Emerald 600
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECFDF5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFFD97706), // Amber 600
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFFBEB),
    onTertiaryContainer = Color(0xFF78350F),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Slate200,
    outlineVariant = Slate300
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val customColors = if (darkTheme) DarkCustomThemeColors else LightCustomThemeColors

  CompositionLocalProvider(LocalCustomThemeColors provides customColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
  }
}
