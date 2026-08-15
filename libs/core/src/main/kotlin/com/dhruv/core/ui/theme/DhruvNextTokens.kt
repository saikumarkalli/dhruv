package com.dhruv.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * DhruvNext design tokens — platform/DESIGN-SYSTEM.md
 * §4, landed per ADR-0024. Raw token data only; not yet wired into [DhruvTheme] or consumed by any
 * component (that lands with the shell rebuild and the `:libs:core` component library).
 */
data class DhruvNextColors(
    val bg: Color,
    val surf: Color,
    val surf2: Color,
    val line: Color,
    val line2: Color,
    val lineStrong: Color,
    val tx: Color,
    val tx2: Color,
    val tx3: Color,
    val acc: Color,
    val accBright: Color,
    val accSoft: Color,
    val accLine: Color,
    val onAcc: Color,
    val pos: Color,
    val posBright: Color,
    val posSoft: Color,
    val neg: Color,
    val negSoft: Color,
    val warn: Color,
    val warnSoft: Color,
    val chart1: Color,
    val chart2: Color,
    val chart3: Color,
    val chart4: Color,
    val chart5: Color,
    val chart6: Color,
)

// Colors aligned with Claude Design project v1.0 FINAL (Dhruv Brand & Theme / Dhruv Web App).
// acc/onAcc reuse PrimaryLight/PrimaryDark (Color.kt) — numerically identical (ADR-0024).
val DhruvNextLightColors =
    DhruvNextColors(
        bg = Color(0xFFF9F9F9),
        surf = Color(0xFFFFFFFF),
        surf2 = Color(0xFFF3F4F6),
        line = Color(0xFFE5E7EB),
        line2 = Color(0xFFEFEEEB),
        lineStrong = Color(0xFFD1D5DB),
        tx = Color(0xFF111827),
        tx2 = Color(0xFF374151),
        tx3 = Color(0xFF6B7280),
        acc = PrimaryLight,
        accBright = PrimaryDark,
        accSoft = Color(0xFFFFF1ED),
        accLine = Color(0xFFF9CDBC),
        onAcc = Color(0xFFFFFFFF),
        pos = Color(0xFF00796B),
        posBright = Color(0xFF4DB6AC),
        posSoft = Color(0xFFE0F2F0),
        neg = Color(0xFFB3261E),
        negSoft = Color(0xFFFCECEB),
        warn = Color(0xFFB45309),
        warnSoft = Color(0xFFFEF3E2),
        chart1 = Color(0xFFF05A28),
        chart2 = Color(0xFF00796B),
        chart3 = Color(0xFF00B0FF),
        chart4 = Color(0xFF4A148C),
        chart5 = Color(0xFFB45309),
        chart6 = Color(0xFF455A64),
    )

val DhruvNextDarkColors =
    DhruvNextColors(
        bg = Color(0xFF0A0A0A),
        surf = Color(0xFF1E1E1E),
        surf2 = Color(0xFF2C2C2C),
        line = Color(0xFF3A3A3A),
        line2 = Color(0xFF2C2C2C),
        lineStrong = Color(0xFF4A4A4A),
        tx = Color(0xFFF5F5F5),
        tx2 = Color(0xFFCFD8DC),
        tx3 = Color(0xFF9E9E9E),
        acc = PrimaryDark,
        accBright = Color(0xFFFF8A5C),
        accSoft = Color(0x24FF6D3B), // 14%-alpha accent
        accLine = Color(0x52FF6D3B), // 32%-alpha accent
        onAcc = Color(0xFF003258),
        pos = Color(0xFF4DB6AC),
        posBright = Color(0xFF80CBC4),
        posSoft = Color(0x294DB6AC), // 16%-alpha
        neg = Color(0xFFF2B8B5),
        negSoft = Color(0x24F2B8B5), // 14%-alpha
        warn = Color(0xFFFFB300),
        warnSoft = Color(0x26FFB300), // 15%-alpha
        chart1 = Color(0xFFFF6D3B),
        chart2 = Color(0xFF4DB6AC),
        chart3 = Color(0xFF80D8FF),
        chart4 = Color(0xFFB388FF),
        chart5 = Color(0xFFFFB300),
        chart6 = Color(0xFFCFD8DC),
    )

/** One breakpoint's worth of spacing (dp) — DhruvNext §4: card padding, screen gutter, inter-card gap, section gap, input group gap. */
data class DhruvNextSpacingValues(
    val cardPadding: Dp,
    val screenGutter: Dp,
    val interCardGap: Dp,
    val sectionGap: Dp,
    val inputGroupGap: Dp,
)

/** One breakpoint's worth of corner radii (dp) — DhruvNext §4. */
data class DhruvNextRadiiValues(
    val card: Dp,
    val listGroup: Dp,
    val innerTile: Dp,
    val pill: Dp,
)

/**
 * One breakpoint's worth of the named type scale — DhruvNext §4's roles (title 17/700, card
 * 15/700, body 13.5, meta 11–12, section-label 10/700 uppercase, hero 30–46/700). [hero] is the
 * screen-size-responsive tier only; content-length-adaptive sizing (e.g. the calculator result's
 * shrink-to-fit) is a separate, complementary mechanism layered on top by the caller, not this.
 */
data class DhruvNextTypeScaleValues(
    val hero: TextUnit,
    val title: TextUnit,
    val cardTitle: TextUnit,
    val body: TextUnit,
    val meta: TextUnit,
    val sectionLabel: TextUnit,
)

/**
 * One breakpoint's worth of calculator-keypad glyph sizes. Deliberately separate from
 * [DhruvNextTypeScaleValues] — keypad digits/operators are interactive-button glyphs sized to fit
 * a fixed-weight grid of keys, not content text; scaling them onto the content scale's `hero` tier
 * (30–46sp) would look disproportionate against the keypad's own visual hierarchy. This tier still
 * scales — modestly — so the keypad isn't the one part of the screen frozen at phone-portrait size.
 */
data class DhruvNextKeypadScaleValues(
    val digit: TextUnit,
    val operator: TextUnit,
    val function: TextUnit,
    val caption: TextUnit,
)

/** Bundles one breakpoint's worth of every DhruvNext responsive token together. */
data class DhruvNextResponsiveTokens(
    val spacing: DhruvNextSpacingValues,
    val radii: DhruvNextRadiiValues,
    val type: DhruvNextTypeScaleValues,
    val keypad: DhruvNextKeypadScaleValues,
)

/**
 * Resolves DhruvNext's spacing/radii/type-scale/keypad-scale tokens for a screen size. Three
 * tiers: small (<360dp width or <600dp height — this also catches a phone rotated to landscape,
 * since its portrait width becomes a <600dp landscape height), tablet (>=600dp width), and the
 * phone-portrait default. Uses the same width/height thresholds as [calculateResponsiveMetrics] so
 * both responsive systems in this app agree on what counts as "tablet"/"small". A fourth "wide but
 * short" tier (`width > height` while `height >= 600`) was considered and dropped: given these
 * thresholds it is mathematically unreachable (that combination always implies `width >= 600`,
 * which the tablet check already claims first) — shipping an unreachable branch just to look
 * symmetric isn't worth the dead code. Corner radii are deliberately constant across every tier —
 * DhruvNext §4 gives fixed values with no size-tier variants, and radius scaling isn't a standard
 * responsive pattern the way spacing/type/keypad are.
 */
@Suppress("LongMethod")
fun calculateDhruvNextResponsiveTokens(
    widthDp: Int,
    heightDp: Int,
): DhruvNextResponsiveTokens {
    val isTablet = widthDp >= 600
    val isExtremelySmall = widthDp < 360 || heightDp < 600

    val spacing =
        when {
            isExtremelySmall ->
                DhruvNextSpacingValues(
                    cardPadding = 14.dp,
                    screenGutter = 12.dp,
                    interCardGap = 10.dp,
                    sectionGap = 20.dp,
                    inputGroupGap = 10.dp,
                )
            isTablet ->
                DhruvNextSpacingValues(
                    cardPadding = 24.dp,
                    screenGutter = 20.dp,
                    interCardGap = 16.dp,
                    sectionGap = 28.dp,
                    inputGroupGap = 16.dp,
                )
            else ->
                DhruvNextSpacingValues(
                    cardPadding = 22.dp,
                    screenGutter = 16.dp,
                    interCardGap = 12.dp,
                    sectionGap = 24.dp,
                    inputGroupGap = 12.dp,
                )
        }

    val radii = DhruvNextRadiiValues(card = 16.dp, listGroup = 18.dp, innerTile = 14.dp, pill = 26.dp)

    val type =
        when {
            isExtremelySmall ->
                DhruvNextTypeScaleValues(
                    hero = 32.sp,
                    title = 15.sp,
                    cardTitle = 13.5.sp,
                    body = 12.sp,
                    meta = 10.sp,
                    sectionLabel = 9.sp,
                )
            isTablet ->
                DhruvNextTypeScaleValues(
                    hero = 46.sp,
                    title = 20.sp,
                    cardTitle = 17.sp,
                    body = 15.sp,
                    meta = 13.sp,
                    sectionLabel = 11.sp,
                )
            else ->
                DhruvNextTypeScaleValues(
                    hero = 38.sp,
                    title = 17.sp,
                    cardTitle = 15.sp,
                    body = 13.5.sp,
                    meta = 11.5.sp,
                    sectionLabel = 10.sp,
                )
        }

    val keypad =
        when {
            isExtremelySmall -> DhruvNextKeypadScaleValues(digit = 18.sp, operator = 22.sp, function = 11.sp, caption = 9.sp)
            isTablet -> DhruvNextKeypadScaleValues(digit = 26.sp, operator = 30.sp, function = 15.sp, caption = 13.sp)
            else -> DhruvNextKeypadScaleValues(digit = 22.sp, operator = 26.sp, function = 13.sp, caption = 11.sp)
        }

    return DhruvNextResponsiveTokens(spacing, radii, type, keypad)
}

/** Defaults match the phone-portrait tier so a component previewed with no [DhruvTheme] ancestor
 * (should not happen in production — see [LocalDhruvNextColors]) still renders sane sizes. */
val LocalDhruvNextSpacingValues =
    staticCompositionLocalOf {
        DhruvNextSpacingValues(cardPadding = 22.dp, screenGutter = 16.dp, interCardGap = 12.dp, sectionGap = 24.dp, inputGroupGap = 12.dp)
    }
val LocalDhruvNextRadiiValues =
    staticCompositionLocalOf { DhruvNextRadiiValues(card = 16.dp, listGroup = 18.dp, innerTile = 14.dp, pill = 26.dp) }
val LocalDhruvNextTypeScale =
    staticCompositionLocalOf {
        DhruvNextTypeScaleValues(hero = 38.sp, title = 17.sp, cardTitle = 15.sp, body = 13.5.sp, meta = 11.5.sp, sectionLabel = 10.sp)
    }
val LocalDhruvNextKeypadScale =
    staticCompositionLocalOf { DhruvNextKeypadScaleValues(digit = 22.sp, operator = 26.sp, function = 13.sp, caption = 11.sp) }

/**
 * Corner radii (dp) — DhruvNext §4. Same call-site shape as before (`DhruvNextRadii.card`); now
 * backed by [LocalDhruvNextRadiiValues] instead of a plain constant, so no call site needed to
 * change for this to become theme-driven. Values are constant across breakpoints (see
 * [calculateDhruvNextResponsiveTokens]) but still routed through the composition local for
 * consistency and in case that changes later.
 */
object DhruvNextRadii {
    val card: Dp
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextRadiiValues.current.card
    val listGroup: Dp
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextRadiiValues.current.listGroup
    val innerTile: Dp
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextRadiiValues.current.innerTile
    val pill: Dp
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextRadiiValues.current.pill
}

/**
 * Spacing (dp) — DhruvNext §4: card padding, screen gutter, inter-card gap. Same call-site shape
 * as before (`DhruvNextSpacing.screenGutter`); now screen-size responsive via
 * [LocalDhruvNextSpacingValues] — every existing call site becomes responsive automatically.
 */
object DhruvNextSpacing {
    val cardPadding: Dp
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextSpacingValues.current.cardPadding
    val screenGutter: Dp
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextSpacingValues.current.screenGutter
    val interCardGap: Dp
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextSpacingValues.current.interCardGap
    val sectionGap: Dp
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextSpacingValues.current.sectionGap
    val inputGroupGap: Dp
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextSpacingValues.current.inputGroupGap
}

/**
 * Named type scale — DhruvNext §4's roles, screen-size responsive. Use `DhruvNextType.title`/
 * `.cardTitle`/`.body`/`.meta`/`.sectionLabel`/`.hero` instead of hardcoding a `.sp` literal in
 * component or screen code, the same way colors go through [LocalDhruvNextColors] instead of a
 * raw hex.
 */
object DhruvNextType {
    val hero: TextUnit
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextTypeScale.current.hero
    val title: TextUnit
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextTypeScale.current.title
    val cardTitle: TextUnit
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextTypeScale.current.cardTitle
    val body: TextUnit
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextTypeScale.current.body
    val meta: TextUnit
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextTypeScale.current.meta
    val sectionLabel: TextUnit
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextTypeScale.current.sectionLabel
}

/**
 * Calculator-keypad glyph sizes — screen-size responsive, but on its own modest scale separate
 * from [DhruvNextType] (see [DhruvNextKeypadScaleValues] for why). `digit` = numeral/`=` glyphs,
 * `operator` = the larger `%÷×−+` symbols, `function` = scientific-row labels (sin/cos/log/√/…),
 * `caption` = small toggle/badge text (DEG/RAD, key badges).
 */
object DhruvNextKeypad {
    val digit: TextUnit
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextKeypadScale.current.digit
    val operator: TextUnit
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextKeypadScale.current.operator
    val function: TextUnit
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextKeypadScale.current.function
    val caption: TextUnit
        @Composable @ReadOnlyComposable
        get() = LocalDhruvNextKeypadScale.current.caption
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
        accBright = accent,
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
            else -> null
        }
    return withAlpha?.toLongOrNull(radix = 16)?.let { Color(it.toInt()) }
}

/** Perceived (not CIE-precise) luminance — enough to pick readable on-accent text/icon color. */
private fun Color.relativeLuminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

private const val HEX_RGB_LENGTH = 6
private const val HEX_ARGB_LENGTH = 8
private const val ON_ACCENT_LUMINANCE_THRESHOLD = 0.55f
private const val ACCENT_SOFT_ALPHA = 0.14f
private const val ACCENT_LINE_ALPHA = 0.32f
private val DARK_ON_ACCENT = Color(0xFF14161A)
