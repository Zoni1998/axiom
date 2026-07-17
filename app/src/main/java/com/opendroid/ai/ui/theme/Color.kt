package com.opendroid.ai.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * ZonIA premium color palette — glassmorphism-ready with AMOLED dark mode.
 */
data class ZoniaColors(
    val background: Color,
    val surface: Color,
    val glassSurface: Color,
    val glassBorder: Color,
    val cardBackground: Color,
    val borderColor: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentPrimary: Color,    // Premium purple
    val accentSecondary: Color,  // Neon green
    val accentCyan: Color,
    val accentRed: Color,
    val glassGradientStart: Color,
    val glassGradientEnd: Color,
    val isDark: Boolean
)

// ═══════════════ DARK — AMOLED Black ═══════════════
val DarkPalette = ZoniaColors(
    background = Color(0xFF000000),          // Pure AMOLED black
    surface = Color(0xFF0A0A0A),            // Slightly elevated
    glassSurface = Color(0x14FFFFFF),       // Ultra-subtle glass (8% white)
    glassBorder = Color(0x1AFFFFFF),        // Glass border (10% white)
    cardBackground = Color(0x0DFFFFFF),     // Card (5% white)
    borderColor = Color(0x1AFFFFFF),        // Subtle borders
    textPrimary = Color(0xFFF5F5F7),        // Soft white (Apple-style)
    textSecondary = Color(0xFF98989D),      // Muted gray
    accentPrimary = Color(0xFFA78BFA),      // Premium violet
    accentSecondary = Color(0xFF00FF88),    // Neon green (kept)
    accentCyan = Color(0xFF5EEAD4),         // Soft teal
    accentRed = Color(0xFFFF453A),          // iOS red
    glassGradientStart = Color(0x1AA78BFA), // Purple glass tint
    glassGradientEnd = Color(0x1A6366F1),   // Blue glass tint
    isDark = true
)

// ═══════════════ LIGHT — iOS-style Soft ═══════════════
val LightPalette = ZoniaColors(
    background = Color(0xFFF2F2F7),         // iOS light gray
    surface = Color(0xFFFFFFFF),            // Pure white
    glassSurface = Color(0xCCFFFFFF),       // Frosted glass (80% white)
    glassBorder = Color(0x1A000000),        // Subtle dark border
    cardBackground = Color(0xFFFFFFFF),     // White cards
    borderColor = Color(0xFFE5E5EA),        // iOS separator
    textPrimary = Color(0xFF1C1C1E),        // Near black
    textSecondary = Color(0xFF8E8E93),      // iOS gray
    accentPrimary = Color(0xFF7C3AED),      // Deep violet
    accentSecondary = Color(0xFF059669),    // Emerald green
    accentCyan = Color(0xFF0D9488),         // Darker teal
    accentRed = Color(0xFFDC2626),          // Red
    glassGradientStart = Color(0x1A7C3AED), // Purple tint
    glassGradientEnd = Color(0x1A6366F1),   // Blue tint
    isDark = false
)

val LocalZoniaColors = compositionLocalOf { DarkPalette }

object AppTheme {
    val colors: ZoniaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalZoniaColors.current
}

// ── Legacy aliases for backward compat ──
val DarkBackground = DarkPalette.background
val DarkSurface = DarkPalette.surface
val CardBackground = DarkPalette.cardBackground
val BorderColor = DarkPalette.borderColor
val TextPrimary = DarkPalette.textPrimary
val TextSecondary = DarkPalette.textSecondary
val AccentSecondary = DarkPalette.accentSecondary
val AccentPrimary = DarkPalette.accentPrimary
val AccentCyan = DarkPalette.accentCyan
val AccentRed = DarkPalette.accentRed
