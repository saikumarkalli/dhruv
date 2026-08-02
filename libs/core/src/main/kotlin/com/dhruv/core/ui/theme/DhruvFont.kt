package com.dhruv.core.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.dhruv.core.R

val SpaceGroteskFamily = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

/** Global font-family preference stored in DataStore key "font_family". */
enum class DhruvFont {
    /** Default: Space Grotesk display + Inter body. */
    DEFAULT,

    /** All text in JetBrains Mono. */
    MONO,

    /** Rounded placeholder (maps to SansSerif until a bundled rounded typeface ships). */
    ROUNDED,

    /** Brand serif (Space Grotesk used as brand display). */
    BRAND_SERIF,
}
