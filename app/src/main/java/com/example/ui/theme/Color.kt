package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Indigo Primary (Light / Dark)
val Indigo600 = Color(0xFF4F46E5)
val Indigo700 = Color(0xFF4338CA)
val Indigo800 = Color(0xFF3730A3)
val Indigo500 = Color(0xFF6366F1)
val Indigo400 = Color(0xFF818CF8)
val Indigo300 = Color(0xFFA5B4FC)
val Indigo200 = Color(0xFFC7D2FE)
val Indigo100 = Color(0xFFE0E7FF)
val Indigo50 = Color(0xFFEEF2FF)

// Emerald Green (Success / Lending)
val Emerald600 = Color(0xFF059669)
val Emerald500 = Color(0xFF10B981)
val Emerald400 = Color(0xFF34D399)
val Emerald100 = Color(0xFFD1FAE5)
val Emerald50 = Color(0xFFECFDF5)

// Standard semantic colors
val FinanceGreen = Color(0xFF10B981)
val FinanceGreenDark = Color(0xFF059669)
val FinanceGreenLight = Color(0xFFECFDF5)
val FinanceGreenBorder = Color(0xFFA7F3D0)

val FinanceRed = Color(0xFFEF4444)
val FinanceRedDark = Color(0xFFDC2626)
val FinanceRedLight = Color(0xFFFEF2F2)
val FinanceRedBorder = Color(0xFFFECACA)

val FinanceAmber = Color(0xFFF59E0B)
val FinanceAmberDark = Color(0xFFD97706)
val FinanceAmberLight = Color(0xFFFFFBEB)
val FinanceAmberBorder = Color(0xFFFDE68A)

val FinanceBlue = Color(0xFF3B82F6)
val FinanceBlueDark = Color(0xFF2563EB)
val FinanceBlueLight = Color(0xFFEFF6FF)

val FinancePurple = Color(0xFF8B5CF6)
val FinancePurpleDark = Color(0xFF7C3AED)
val FinancePurpleLight = Color(0xFFF5F3FF)

// Slate Neutrals (Light & Dark)
val Slate950 = Color(0xFF020617)
val Slate900 = Color(0xFF0F172A)
val Slate850 = Color(0xFF131C2E)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)
val Slate50 = Color(0xFFF8FAFC)

// Obsidian Dark Specific Accents
val ObsidianDarkBg = Color(0xFF090D16)
val ObsidianDarkSurface = Color(0xFF131A29)
val ObsidianDarkSurfaceElevated = Color(0xFF1C263B)
val ObsidianDarkBorder = Color(0xFF2B3A52)

data class CustomThemeColors(
    val isDark: Boolean,
    val cardBorder: Color,
    val cardBackground: Color,
    val greenContainer: Color,
    val greenBorder: Color,
    val greenText: Color,
    val redContainer: Color,
    val redBorder: Color,
    val redText: Color,
    val amberContainer: Color,
    val amberBorder: Color,
    val amberText: Color,
    val purpleContainer: Color,
    val purpleBorder: Color,
    val purpleText: Color,
    val blueContainer: Color,
    val blueBorder: Color,
    val blueText: Color,
    val iconBoxBg: Color,
    val iconBoxTint: Color,
    val brandTint: Color
)

val LightCustomThemeColors = CustomThemeColors(
    isDark = false,
    cardBorder = Slate200,
    cardBackground = Color.White,
    greenContainer = Color(0xFFECFDF5),
    greenBorder = Color(0xFFA7F3D0),
    greenText = Color(0xFF059669),
    redContainer = Color(0xFFFEF2F2),
    redBorder = Color(0xFFFECACA),
    redText = Color(0xFFDC2626),
    amberContainer = Color(0xFFFFFBEB),
    amberBorder = Color(0xFFFDE68A),
    amberText = Color(0xFFD97706),
    purpleContainer = Color(0xFFF5F3FF),
    purpleBorder = Color(0xFFDDD6FE),
    purpleText = Color(0xFF7C3AED),
    blueContainer = Color(0xFFEFF6FF),
    blueBorder = Color(0xFFBFDBFE),
    blueText = Color(0xFF2563EB),
    iconBoxBg = Indigo50,
    iconBoxTint = Indigo600,
    brandTint = Indigo600
)

val DarkCustomThemeColors = CustomThemeColors(
    isDark = true,
    cardBorder = ObsidianDarkBorder,
    cardBackground = ObsidianDarkSurface,
    greenContainer = Color(0xFF063327),
    greenBorder = Color(0xFF059669),
    greenText = Color(0xFF34D399),
    redContainer = Color(0xFF3B1214),
    redBorder = Color(0xFFDC2626),
    redText = Color(0xFFF87171),
    amberContainer = Color(0xFF38230A),
    amberBorder = Color(0xFFD97706),
    amberText = Color(0xFFFBBF24),
    purpleContainer = Color(0xFF2A1B4E),
    purpleBorder = Color(0xFF7C3AED),
    purpleText = Color(0xFFA78BFA),
    blueContainer = Color(0xFF132A4A),
    blueBorder = Color(0xFF2563EB),
    blueText = Color(0xFF60A5FA),
    iconBoxBg = Color(0xFF1E283D),
    iconBoxTint = Color(0xFF818CF8),
    brandTint = Color(0xFF818CF8)
)

val LocalCustomThemeColors = staticCompositionLocalOf {
    LightCustomThemeColors
}

object AppTheme {
    val colors: CustomThemeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCustomThemeColors.current
}
