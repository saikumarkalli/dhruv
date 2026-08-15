package com.dhruv.finance.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Fix 4 (final whole-branch review, Important) — [shouldShowOnboarding] is `MainActivity`'s
 * onboarding gate, extracted so it's testable without an Activity/Compose host. Covers the bug:
 * `OnboardingViewModel.exitToShell` is an Activity-scoped, one-way latch, so a later Settings >
 * Privacy "Delete my account" (which resets `ConsentRepository.hasCompletedOnboarding` to
 * `false`, `TrackerAccountRepositoryImpl.deleteMyAccount`) must still re-show onboarding rather
 * than staying pinned to the now-deleted account's shell — which requires
 * `OnboardingViewModel.resetForNewOnboardingSession` to have un-latched `exitToShell` back to
 * `false` first (wired in `MainActivity`, exercised by `OnboardingViewModelTest`'s
 * `resetForNewOnboardingSession flips exitToShell back to false`).
 */
class ShouldShowOnboardingTest {
    @Test
    fun `cold install shows onboarding`() {
        assertEquals(true, shouldShowOnboarding(hasCompletedOnboarding = false, exitToShell = false))
    }

    @Test
    fun `returning user with completed onboarding sees the shell`() {
        assertEquals(false, shouldShowOnboarding(hasCompletedOnboarding = true, exitToShell = false))
    }

    @Test
    fun `user who exited onboarding this session sees the shell even before the flag persists`() {
        assertEquals(false, shouldShowOnboarding(hasCompletedOnboarding = false, exitToShell = true))
    }

    @Test
    fun `signed-in user still short of both signals sees onboarding`() {
        assertEquals(true, shouldShowOnboarding(hasCompletedOnboarding = false, exitToShell = false))
    }

    // The exact Fix 4 sequence: hasCompletedOnboarding has flipped back to false post-deletion and
    // resetForNewOnboardingSession has un-latched exitToShell back to false — the gate must not
    // stay stuck on the stale shell.
    @Test
    fun `post account-deletion reset re-shows onboarding instead of the stale shell`() {
        val staleGate = shouldShowOnboarding(hasCompletedOnboarding = false, exitToShell = true)
        val resetGate = shouldShowOnboarding(hasCompletedOnboarding = false, exitToShell = false)

        assertEquals(false, staleGate)
        assertEquals(true, resetGate)
    }
}
