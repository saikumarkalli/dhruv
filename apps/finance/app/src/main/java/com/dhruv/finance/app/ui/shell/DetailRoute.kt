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

    data object Ask : DetailRoute

    data object Currency : DetailRoute

    data object UnitConverter : DetailRoute

    data object DateTool : DetailRoute

    data object TimeTool : DetailRoute

    data object Profile : DetailRoute

    data object Notifications : DetailRoute
}
