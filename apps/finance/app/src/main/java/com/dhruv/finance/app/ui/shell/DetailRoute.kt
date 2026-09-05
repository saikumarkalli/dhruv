package com.dhruv.finance.app.ui.shell

/**
 * Shell-level "detail/utility" destinations (DhruvNext §5's OWNER=null routes,
 * `platform/DESIGN-SYSTEM.md`): reached from chrome
 * affordances (top-bar icon, floating Ask pill), rendered full-screen with a back top bar and no
 * tab bar. Unlike Plan's loan/invest/tax/everyday (which stay inside Plan's own nested
 * `NavController` and keep the tab bar visible), these are never deep-linked from outside the app
 * today, so a full second-level `NavController` would be pure ceremony — same reasoning NAV6
 * already applied to onboarding/lock. `MainActivity` owns the single `detailRoute` state that
 * swaps one of these in over the tabs scaffold; `shell` (the app-switcher) is a bottom sheet, not
 * a route here, and is tracked as separate boolean visibility state instead.
 */
sealed interface DetailRoute {
    data object Settings : DetailRoute

    /** Settings sub-routes (004-settings T012) — one back step to [Settings], one more to the tab. */
    data object SettingsAccount : DetailRoute

    data object SettingsApp : DetailRoute

    data class SettingsModule(
        val moduleKey: String,
    ) : DetailRoute

    data object Ask : DetailRoute

    /** Home's "View details" entry into C1-C7 (`:apps:finance:feature:networth`'s
     * `NetWorthFeatureRoot`, added 001-net-worth-tracker Phase 8) — that module owns a real nested
     * `NavHostController` of its own (assets/holding-detail/add-holding/add-valuation/liabilities),
     * unlike every other route in this file, so `MainActivity` hoists and hardware-back-integrates
     * it the same way it already does for [com.dhruv.finance.app.ui.plan.PlanLauncher]'s
     * `planNavController` — see the `netWorthNavController` handling in `MainActivity.kt`. */
    data object NetWorth : DetailRoute

    data object Currency : DetailRoute

    data object UnitConverter : DetailRoute

    data object DateTool : DetailRoute

    data object TimeTool : DetailRoute

    data object Profile : DetailRoute

    data object Notifications : DetailRoute
}
