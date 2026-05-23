package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class SectionColorTheme(
    val id: String,
    val name: String,
    val lightPrimary: Color,
    val darkPrimary: Color
)

val ColorOptions = listOf(
    SectionColorTheme("cyan", "Neon Cyan", Color(0xFF00838F), Color(0xFF00E5FF)),
    SectionColorTheme("blue", "Polar Blue", Color(0xFF0061A4), Color(0xFFD3E3FD)),
    SectionColorTheme("purple", "Nebula Purple", Color(0xFF4A148C), Color(0xFFB388FF)),
    SectionColorTheme("green", "Forest Green", Color(0xFF00796B), Color(0xFF4CAF50)),
    SectionColorTheme("coral", "Aurora Coral", Color(0xFFD32F2F), Color(0xFFFF8A80)),
    SectionColorTheme("amber", "Cosmic Amber", Color(0xFFE65100), Color(0xFFFFB300))
)

fun getAccentColor(themeId: String, isDark: Boolean): Color {
    val theme = ColorOptions.firstOrNull { it.id == themeId } ?: ColorOptions[0]
    return if (isDark) theme.darkPrimary else theme.lightPrimary
}
