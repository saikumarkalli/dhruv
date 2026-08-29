package com.dhruv.core.security

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Whether the app is locked right now (`contracts/app-lock-gate.md` §1). Pure and total: no clock
 * access, no credential-availability check inside it — every input is passed in, which is what
 * makes this a JVM unit test instead of a manual device check (gate rule 6). Mirrors
 * `navigation/BackContract.kt`'s decision/effect split; the effect lives in
 * `apps/finance/app/ui/settings/AppLockGate.kt`.
 */
enum class LockState { LOCKED, UNLOCKED }

/** Auto-lock timeout options (data-model.md §3) — append-only ids, never rename a shipped one. */
enum class LockTimeout(
    val id: String,
    val duration: Duration?,
) {
    IMMEDIATE("immediate", null),
    AFTER_1_MIN("after_1_min", 1.minutes),
    AFTER_5_MIN("after_5_min", 5.minutes),
    AFTER_15_MIN("after_15_min", 15.minutes),
    ;

    companion object {
        fun fromId(id: String): LockTimeout = entries.firstOrNull { it.id == id } ?: AFTER_1_MIN
    }
}

/**
 * @param enabled the user's stored `biometric_enabled` preference.
 * @param timeout the user's stored auto-lock timeout.
 * @param elapsedSinceBackground time since the app last left the foreground (`ProcessLifecycleOwner`
 * `ON_STOP`); `null` means this is a cold start — no prior foreground session exists at all. This
 * function is only ever evaluated at those two points (cold start, or `ON_START` following an
 * `ON_STOP`), never continuously while already in the foreground, so `null` unambiguously means
 * cold start — there is no third "mid-session, never backgrounded" call site.
 * @param alreadyAuthenticatedThisForeground whether the foreground session that just ended (or is
 * continuing, if [elapsedSinceBackground] is null and the process never died) had already been
 * unlocked. The caller resets this to `false` whenever a lock actually engages.
 * @param hasEnrolledCredential whether the device has a usable biometric or device credential right
 * now, checked fresh at every resolve (gate §1 rule 5) — not cached from when the switch was toggled.
 */
fun appLockState(
    enabled: Boolean,
    timeout: LockTimeout,
    elapsedSinceBackground: Duration?,
    alreadyAuthenticatedThisForeground: Boolean,
    hasEnrolledCredential: Boolean,
): LockState =
    when {
        // Rule 5: no credential enrolled -> never lock, regardless of the stored preference. This
        // is what prevents a removed-credential from permanently excluding the user from Settings.
        !hasEnrolledCredential -> LockState.UNLOCKED
        // Rule 4: lock off -> always unlocked, no stale state.
        !enabled -> LockState.UNLOCKED
        // Rule 1: cold start (no prior foreground session in this process) -> always locked.
        elapsedSinceBackground == null -> LockState.LOCKED
        // Rule 3: the foreground session that just ended was never authenticated -> stays locked.
        !alreadyAuthenticatedThisForeground -> LockState.LOCKED
        // Rule 2: Immediate locks on any backgrounding, however brief — elapsedSinceBackground
        // being present at all means an ON_STOP happened.
        timeout == LockTimeout.IMMEDIATE -> LockState.LOCKED
        // Rule 2: every other timeout locks when elapsed >= timeout (inclusive boundary).
        elapsedSinceBackground >= requireNotNull(timeout.duration) -> LockState.LOCKED
        else -> LockState.UNLOCKED
    }
