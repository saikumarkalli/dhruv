package com.dhruv.finance.app.navigation

import com.dhruv.core.navigation.NavTarget
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * The only cross-feature navigation mechanism (NAV1/ADR-0024,
 * `docs/superpowers/specs/2026-07-12-security-navigation-technical-review.md`). Producers call
 * [navigate] with a [NavTarget] (route id + validated args, never a screen class reference);
 * `MainActivity` collects [targets] and drives the pager + the target tab's `NavController`.
 *
 * No replay: a late collector (e.g. right after a config change) must not replay a stale
 * navigation — screen state is restored via Compose's own SavedState mechanism, not by re-firing
 * old targets. [extraBufferCapacity] of 1 only protects a `navigate` call made a moment before
 * `MainActivity`'s collector starts (app cold start), not a general history buffer.
 */
class NavigationDispatcher {
    private val _targets = MutableSharedFlow<NavTarget>(replay = 0, extraBufferCapacity = 1)
    val targets: SharedFlow<NavTarget> = _targets

    fun navigate(target: NavTarget) {
        _targets.tryEmit(target)
    }
}
