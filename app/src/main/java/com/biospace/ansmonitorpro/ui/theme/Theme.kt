package com.biospace.ansmonitorpro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Exact BioSpace Monitor palette ───────────────────────────────────────────
object AppColors {
    val Background     = Color(0xFF000000)
    val CardBg         = Color(0xFF0A0E1A)
    val CardBg2        = Color(0xFF0D1221)
    val Cyan           = Color(0xFF00E5FF)
    val CyanDim        = Color(0xFF00B8CC)
    val Gold           = Color(0xFFFFA500)
    val GoldDim        = Color(0xFFCC8400)
    val Green          = Color(0xFF00FF88)
    val GreenDim       = Color(0xFF00CC6A)
    val Magenta        = Color(0xFFFF00FF)
    val MagentaDim     = Color(0xFFCC00CC)
    val Red            = Color(0xFFFF3333)
    val Orange         = Color(0xFFFF6600)   // ← moved here; was duplicate extension in 3 files
    val White          = Color(0xFFFFFFFF)
    val TextPrimary    = Color(0xFFE0E8F0)
    val TextSecondary  = Color(0xFF8B9DB5)
    val TextDim        = Color(0xFF4A5568)
    val Divider        = Color(0xFF1A2030)
    val TabBorder      = Color(0xFF1E2A3A)
    val ActiveTab      = Color(0xFF00E5FF)
}

@Composable
fun ANSMonitorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = AppColors.Background,
            surface    = AppColors.CardBg,
            primary    = AppColors.Cyan,
            onSurface  = AppColors.TextPrimary,
        ),
        content = content
    )
}
