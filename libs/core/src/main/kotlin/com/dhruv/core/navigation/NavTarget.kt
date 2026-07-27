package com.dhruv.core.navigation

/**
 * Stable identity for a top-level DhruvNext tab (ADR-0024). Page index is always resolved by
 * looking this key up in the currently-visible tab list — never by raw pager position — so a
 * flag flip that changes the tab count mid-session can't point [SelectTab] at the wrong tab
 * (NAV1/NAV4, `docs/superpowers/specs/2026-07-12-security-navigation-technical-review.md`).
 */
enum class TabKey { HOME, CALC, PLAN, INSIGHTS }

/** A drill-in destination inside the Plan tab's nested NavHost — Plan stays the highlighted tab. */
enum class PlanTool { LOAN, INVEST, TAX, EVERYDAY }

/**
 * The only cross-feature navigation vocabulary (NAV1/ADR-0024): a target names WHERE to go by id,
 * never by screen class reference, so features stay decoupled from each other's Gradle modules.
 * `:apps:finance:app`'s `NavigationDispatcher` maps a target to (tab, nested route) and drives the
 * pager + that tab's `NavController`. Adding a route is a two-step: add the case here, add the
 * matching row in the route registry (`2026-07-12-app-design-standard.md` §3.3).
 *
 * Currency/Unit/Date/Time/Settings/Ask are deliberately NOT here: per the DhruvNext nav model
 * (`2026-07-25-dhruvnext-ui-ux-design-reference.md` §5's OWNER table) they belong to no tab —
 * they're shell-level detail routes with their own back-top-bar, not part of any tab's nested
 * back stack, so they don't need cross-tab dispatch.
 */
sealed interface NavTarget {
    data class SelectTab(
        val tab: TabKey,
    ) : NavTarget

    data class OpenPlanTool(
        val tool: PlanTool,
    ) : NavTarget
}

/** Which tab a [NavTarget] belongs to — every case names exactly one. */
val NavTarget.tab: TabKey
    get() =
        when (this) {
            is NavTarget.SelectTab -> tab
            is NavTarget.OpenPlanTool -> TabKey.PLAN
        }

/**
 * Resolves a target's tab against the tabs actually visible right now, by key rather than
 * position (NAV4). An unknown or currently-hidden tab (e.g. a flag flipped it off mid-session)
 * falls back to the first visible tab instead of an out-of-bounds index.
 */
fun List<TabKey>.pageIndexFor(target: NavTarget): Int {
    val index = indexOf(target.tab)
    return if (index >= 0) index else 0
}
