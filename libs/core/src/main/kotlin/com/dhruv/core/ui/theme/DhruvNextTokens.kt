package com.dhruv.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * DhruvNext design tokens — docs/superpowers/specs/2026-07-25-dhruvnext-ui-ux-design-reference.md
 * §4, landed per ADR-0024. Raw token data only; not yet wired into [DhruvTheme] or consumed by any
 * component (that lands with the shell rebuild and the `:libs:core` component library).
 */
data class DhruvNextColors(
    val bg: Color,
    val surf: Color,
    val surf2: Color,
    val line: Color,
    val line2: Color,
    val tx: Color,
    val tx2: Color,
    val tx3: Color,
    val acc: Color,
    val accSoft: Color,
    val accLine: Color,
    val onAcc: Color,
    val pos: Color,
    val neg: Color,
    val warn: Color,
    val chart1: Color,
    val chart2: Color,
    val chart3: Color,
    val chart4: Color,
    val chart5: Color,
    val chart6: Color,
)

// acc/onAcc reuse PrimaryLight/PrimaryDark (Color.kt) — DhruvNext's orange is numerically
// identical to the already-shipped default primary (ADR-0024).
val DhruvNextLightColors =
    DhruvNextColors(
        bg = Color(0xFFF7F7F5),
        surf = Color(0xFFFFFFFF),
        surf2 = Color(0xFFF1F1EE),
        line = Color(0xFFE5E4E0),
        line2 = Color(0xFFEFEEEB),
        tx = Color(0xFF14161A),
        tx2 = Color(0xFF6B6F76),
        tx3 = Color(0xFF9AA0A6),
        acc = PrimaryLight,
        accSoft = Color(0xFFFFF1ED),
        accLine = Color(0xFFF9CDBC),
        onAcc = Color(0xFFFFFFFF),
        pos = Color(0xFF00796B),
        neg = Color(0xFFB3261E),
        warn = Color(0xFFE65100),
        chart1 = Color(0xFFF05A28),
        chart2 = Color(0xFF00796B),
        chart3 = Color(0xFF0061A4),
        chart4 = Color(0xFF4A148C),
        chart5 = Color(0xFFE65100),
        chart6 = Color(0xFF455A64),
    )

val DhruvNextDarkColors =
    DhruvNextColors(
        bg = Color(0xFF0B0B0C),
        surf = Color(0xFF16171A),
        surf2 = Color(0xFF1E2024),
        line = Color(0xFF2A2C31),
        line2 = Color(0xFF222428),
        tx = Color(0xFFF2F3F5),
        tx2 = Color(0xFF9BA1A9),
        tx3 = Color(0xFF6E747C),
        acc = PrimaryDark,
        accSoft = Color(0x24FF6D3B), // 14%-alpha accent, per spec
        accLine = Color(0x52FF6D3B), // 32%-alpha accent, per spec
        onAcc = Color(0xFF231007),
        pos = Color(0xFF4CAF50),
        neg = Color(0xFFF2B8B5),
        warn = Color(0xFFFFB300),
        chart1 = Color(0xFFFF6D3B),
        chart2 = Color(0xFF4CAF50),
        chart3 = Color(0xFF80D8FF),
        chart4 = Color(0xFFB388FF),
        chart5 = Color(0xFFFFB300),
        chart6 = Color(0xFFCFD8DC),
    )

/** Corner radii (dp) — DhruvNext §4. */
object DhruvNextRadii {
    val card: Dp = 20.dp
    val listGroup: Dp = 18.dp
    val innerTile: Dp = 14.dp
    val pill: Dp = 26.dp
}

/** Spacing (dp) — DhruvNext §4: card padding, screen gutter, inter-card gap. */
object DhruvNextSpacing {
    val cardPadding: Dp = 18.dp
    val screenGutter: Dp = 16.dp
    val interCardGap: Dp = 12.dp
}

/**
 * Provides [DhruvNextColors] to the component library. Defaults to light — [DhruvTheme] provides
 * the resolved (dark-aware) value (via [resolveDhruvNextColors]) once composition reaches the
 * shell rebuild's `CompositionLocalProvider`; components must not read this before that boundary
 * is in place higher up the tree.
 */
val LocalDhruvNextColors = staticCompositionLocalOf { DhruvNextLightColors }

/**
 * Picks the dark/light base palette and, if the user overrode the global accent (D2/decision 2
 * — a 4-swatch picker in Settings, same [accentColorHex] field [DhruvTheme] already uses for
 * `MaterialTheme.colorScheme.primary]), swaps `acc`/`onAcc`/`accSoft`/`accLine` to match. A
 * malformed hex (should not happen from the picker, but callers pass user-settings data) falls
 * back to the base palette rather than crashing the whole theme.
 */
fun resolveDhruvNextColors(
    darkTheme: Boolean,
    accentColorHex: String?,
): DhruvNextColors {
    val base = if (darkTheme) DhruvNextDarkColors else DhruvNextLightColors
    val accent = accentColorHex?.let(::parseHexColor) ?: return base

    return base.copy(
        acc = accent,
        onAcc = if (accent.relativeLuminance() > ON_ACCENT_LUMINANCE_THRESHOLD) DARK_ON_ACCENT else Color.White,
        accSoft = accent.copy(alpha = ACCENT_SOFT_ALPHA),
        accLine = accent.copy(alpha = ACCENT_LINE_ALPHA),
    )
}

/**
 * "#RGB hex" → [Color], deliberately not using `android.graphics.Color.parseColor` — that's an
 * Android-framework stub under plain JVM unit tests (no Robolectric here) and would silently
 * return black instead of parsing. Pure-Kotlin `Color(Int)` needs no framework at all.
 */
private fun parseHexColor(hex: String): Color? {
    val cleaned = hex.removePrefix("#")
    val withAlpha =
        when (cleaned.length) {
            HEX_RGB_LENGTH -> "FF$cleaned"
            HEX_ARGB_LENGTH -> cleaned
            else -> return null
        }
    val argb = withAlpha.toLongOrNull(radix = 16) ?: return null
    return Color(argb.toInt())
}

/** Perceived (not CIE-precise) luminance — enough to pick readable on-accent text/icon color. */
private fun Color.relativeLuminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

private const val HEX_RGB_LENGTH = 6
private const val HEX_ARGB_LENGTH = 8
private const val ON_ACCENT_LUMINANCE_THRESHOLD = 0.55f
private const val ACCENT_SOFT_ALPHA = 0.14f
private const val ACCENT_LINE_ALPHA = 0.32f
private val DARK_ON_ACCENT = Color(0xFF14161A)
