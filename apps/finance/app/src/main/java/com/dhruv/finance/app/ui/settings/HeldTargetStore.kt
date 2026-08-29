package com.dhruv.finance.app.ui.settings

import com.dhruv.core.navigation.NavTarget

/**
 * The pending [NavTarget] held while the app is LOCKED (`contracts/app-lock-gate.md` §3) — a deep
 * link, notification tap or launcher shortcut arriving while locked is held here rather than
 * delivered or dropped. Deliberately in-memory only: never serialized, never surviving process
 * death (data-model.md §5) — a persisted held target would let a link the user never authenticated
 * for wait across a cold start, which contradicts gate §1 rule 1 (cold start always locks).
 *
 * Plain Kotlin, no Compose/Android dependency, so the hold/replace/dispatch-once state machine is
 * unit-testable without Robolectric.
 */
class HeldTargetStore {
    private var held: NavTarget? = null

    /** Rule 16: only one target is held — a new arrival replaces whatever was already held. */
    fun hold(target: NavTarget) {
        held = target
    }

    /**
     * Rule 14/15: returns the held target and clears it — dispatched exactly once, never twice.
     * Process death clearing it (data-model.md §5) needs no code of its own: this store is a plain
     * in-memory field, so it ceases to exist along with the rest of the process — there is nothing
     * to persist and therefore nothing to explicitly clear on that path.
     */
    fun takeAndClear(): NavTarget? {
        val target = held
        held = null
        return target
    }
}
