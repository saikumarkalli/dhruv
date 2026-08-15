// Named for the back-navigation contract it implements (BackAction + resolveBackAction together),
// not just the first declaration — same convention as NavBars.kt/ConsentGateScaffold.kt.
@file:Suppress("MatchingDeclarationName", "ktlint:standard:filename")

package com.dhruv.core.navigation

/**
 * What a system back-press should do, resolved from shell state alone — pure decision logic, no
 * Activity/Compose dependency, so the back contract (N1/N2,
 * `docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md` §4) is unit-testable
 * without Robolectric. `MainActivity`'s `OnBackPressedCallback` calls [resolveBackAction] then
 * performs the matching side effect — the decision and the effect are deliberately split so the
 * decision can be proven correct in isolation.
 */
enum class BackAction {
    /** A shell-level detail route (Settings/Ask/Currency/…) is showing — close it first. */
    CLOSE_DETAIL,

    /** The active tab has its own nested back stack (only Plan today) — pop one level of it. */
    POP_NESTED,

    /** Not on the first tab and nothing else to unwind — return to tab 0. */
    RETURN_TO_FIRST_TAB,

    /** Already at tab 0 with nothing else on any stack — let the system exit the app. */
    EXIT_APP,
}

/**
 * Resolves one back-press against the shell's current state, in the fixed precedence order the
 * functional spec's nav contract requires: a shown detail route pops first, then the active tab's
 * own nested back stack, then the pager returns to the first tab, then the app exits. This is the
 * single place that order is encoded — `MainActivity` must not re-derive it inline.
 */
fun resolveBackAction(
    hasDetailRoute: Boolean,
    activeTabHasNestedBackStack: Boolean,
    currentTabIndex: Int,
): BackAction =
    when {
        hasDetailRoute -> BackAction.CLOSE_DETAIL
        activeTabHasNestedBackStack -> BackAction.POP_NESTED
        currentTabIndex != 0 -> BackAction.RETURN_TO_FIRST_TAB
        else -> BackAction.EXIT_APP
    }
