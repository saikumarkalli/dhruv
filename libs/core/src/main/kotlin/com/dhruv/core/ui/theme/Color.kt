package com.dhruv.core.ui.theme

import androidx.compose.ui.graphics.Color

// Dhruv Brand Colors (navy + silver, per brand kit)
val DhruvNavy         = Color(0xFF0D1B2A) // primary background / icon bg
val DhruvNavyElevated = Color(0xFF132B4D) // elevated navy surface
val DhruvBlue         = Color(0xFF1E3A6D) // mid navy / accents
val DhruvSilver       = Color(0xFFC0C6D1) // primary silver (refined from #C0C0C0)
val DhruvSilverLight  = Color(0xFFE6E9EF) // bright silver highlight
val DhruvSteel        = Color(0xFF8E97A6) // muted steel (orbital rings)
val DhruvAccent       = Color(0xFF3FA7FF) // accent blue
val DhruvLogoBg       = Color(0xFFF4F6FA) // light tint behind the full-color logo / app icon

// Legacy accent (a muted purple, historically mislabeled "gold"). Brand chrome now uses
// DhruvSilver/DhruvSilverLight; this is kept only for existing accent references (debug mocks etc.).
val DhruvGold   = Color(0xFF665080)

// Light Theme Palettes (with some elegant refinements)
val PrimaryLight = Color(0xFFF05A28) // MIUI Orange
val SecondaryLight = Color(0xFF455A64) // Slate Blue
val TertiaryLight = Color(0xFF00B0FF) // Vivid Cyan
val BackgroundLight = Color(0xFFF9F9F9) // Clean white/grey MIUI background
val SurfaceLight = Color(0xFFFFFFFF) // Clean white key surface

// Sophisticated Dark Theme Palettes
val PrimaryDark = Color(0xFFFF6D3B) // Bright MIUI Orange for Dark Theme
val SecondaryDark = Color(0xFFCFD8DC) // Muted Soft Gray
val TertiaryDark = Color(0xFF80D8FF) // Sky Blue
val BackgroundDark = Color(0xFF0A0A0A) // Pitch Black background matching MIUI
val SurfaceDark = Color(0xFF1E1E1E) // Slate neutral dark key/card surface
val SurfaceVariantDark = Color(0xFF2C2C2C) // Numeric key dark surface
val OnSurfaceDark = Color(0xFFF5F5F5) // Warm white text
val OnSurfaceVariantDark = Color(0xFF9E9E9E) // Subtitle text

// Key Special Colors for "Sophisticated Dark" & MIUI Spec
val SophModKeyBg = Color(0xFFEFEFEF) // Modifier bg light
val MiuiDarkModKeyBg = Color(0xFF2C2C2C) // Modifier bg dark
val SophModKeyText = Color(0xFF455A64)

val SophOpKeyBg = Color(0xFFF05A28) // MIUI Orange
val SophOpKeyText = Color(0xFFFFFFFF)

val SophOpKeyBgLight = Color(0xFFFFF1ED) // Extremely soft peach/orange
val SophOpKeyTextLight = Color(0xFFF05A28) // Orange text

val SophNumKeyBg = Color(0xFFFFFFFF) // White keys for Light
val MiuiDarkNumKeyBg = Color(0xFF1E1E1E) // Dark gray keys for dark theme
val SophNumKeyText = Color(0xFF212121)

val SophEqualsBg = Color(0xFFF05A28) // Orange equal button
val SophEqualsText = Color(0xFFFFFFFF)

val SophScienceKeyBg = Color(0xFFF5F5F5)
val SophScienceKeyText = Color(0xFF455A64)
val SophScienceKeyBorder = Color(0xFFE0E0E0)
val SophScienceKeyActiveBg = Color(0xFFCFD8DC)

