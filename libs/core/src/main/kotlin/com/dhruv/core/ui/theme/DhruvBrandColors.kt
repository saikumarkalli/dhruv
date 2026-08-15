// File named for what it documents (brand-chrome colors), not the `DhruvBrand` object name —
// mirrors DhruvNextTokens.kt's naming, which also doesn't match its top-level declarations.
@file:Suppress("MatchingDeclarationName")

package com.dhruv.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand chrome — theme-invariant (ADR-0028). Unlike [DhruvNextColors] this does NOT flip between
 * light and dark: it carries brand identity on the splash screen, hero gradient cards (net worth,
 * a liability's amortisation hero, goal progress), the Settings identity card, and the dark-hero
 * screens the design draws regardless of the user's system theme (holding detail, quick-add,
 * account detail, goal detail, retirement, P&L, the AA-consent modal). Everywhere else reads
 * [LocalDhruvNextColors] instead — brand chrome is not a general-purpose surface palette.
 *
 * Values are the same constants [DhruvBrand.kt][com.dhruv.core.ui.components] already draws the
 * logo/wordmark with ([DhruvNavy] etc. in `Color.kt`) — bundled here as the themed group so new
 * hero/glass surfaces consume one object instead of importing raw top-level colors directly
 * (the "no hardcoding" rule: values flow through a named token, not a scattered import).
 */
object DhruvBrand {
    val navy: Color = DhruvNavy
    val navyElevated: Color = DhruvNavyElevated
    val blueMid: Color = DhruvBlue
    val accentBlue: Color = DhruvAccent
    val silver: Color = DhruvSilver
    val silverLight: Color = DhruvSilverLight
    val steel: Color = DhruvSteel
    val logoBg: Color = DhruvLogoBg
}
