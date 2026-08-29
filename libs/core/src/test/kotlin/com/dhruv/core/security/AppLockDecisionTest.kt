package com.dhruv.core.security

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AppLockDecisionTest {
    // SET-BR-013: cold start with lock enabled is LOCKED, always — no "recently used" credit
    // across process death.
    @Test
    fun `cold start with lock enabled is LOCKED`() {
        val result =
            appLockState(
                enabled = true,
                timeout = LockTimeout.AFTER_1_MIN,
                elapsedSinceBackground = null,
                alreadyAuthenticatedThisForeground = true,
                hasEnrolledCredential = true,
            )
        assertEquals(LockState.LOCKED, result)
    }

    // SET-BR-014: each timeout locks exactly when elapsed >= timeout; Immediate locks on any
    // backgrounding, however brief.
    @Test
    fun `Immediate locks on any backgrounding, however brief`() {
        val result =
            appLockState(
                enabled = true,
                timeout = LockTimeout.IMMEDIATE,
                elapsedSinceBackground = 1.seconds,
                alreadyAuthenticatedThisForeground = true,
                hasEnrolledCredential = true,
            )
        assertEquals(LockState.LOCKED, result)
    }

    @Test
    fun `a timed option stays unlocked while elapsed is below the timeout`() {
        val result =
            appLockState(
                enabled = true,
                timeout = LockTimeout.AFTER_5_MIN,
                elapsedSinceBackground = 4.minutes + 59.seconds,
                alreadyAuthenticatedThisForeground = true,
                hasEnrolledCredential = true,
            )
        assertEquals(LockState.UNLOCKED, result)
    }

    @Test
    fun `a timed option locks exactly at the boundary, elapsed equal to timeout`() {
        val result =
            appLockState(
                enabled = true,
                timeout = LockTimeout.AFTER_5_MIN,
                elapsedSinceBackground = 5.minutes,
                alreadyAuthenticatedThisForeground = true,
                hasEnrolledCredential = true,
            )
        assertEquals(LockState.LOCKED, result)
    }

    @Test
    fun `a timed option locks once elapsed exceeds the timeout`() {
        val result =
            appLockState(
                enabled = true,
                timeout = LockTimeout.AFTER_15_MIN,
                elapsedSinceBackground = 16.minutes,
                alreadyAuthenticatedThisForeground = true,
                hasEnrolledCredential = true,
            )
        assertEquals(LockState.LOCKED, result)
    }

    // SET-BR-015: a successful auth covers the current foreground session only.
    @Test
    fun `unlock does not carry into a session that was never authenticated`() {
        val result =
            appLockState(
                enabled = true,
                timeout = LockTimeout.AFTER_15_MIN,
                elapsedSinceBackground = 1.seconds,
                alreadyAuthenticatedThisForeground = false,
                hasEnrolledCredential = true,
            )
        assertEquals(LockState.LOCKED, result)
    }

    // SET-BR-016: lock off is always UNLOCKED with no stale state.
    @Test
    fun `lock disabled is always UNLOCKED regardless of every other input`() {
        val result =
            appLockState(
                enabled = false,
                timeout = LockTimeout.IMMEDIATE,
                elapsedSinceBackground = null,
                alreadyAuthenticatedThisForeground = false,
                hasEnrolledCredential = true,
            )
        assertEquals(LockState.UNLOCKED, result)
    }

    // Gate §1 rule 5 (CHK005 fix): no enrolled credential -> always UNLOCKED, even cold start with
    // lock enabled — this is what prevents a removed-credential permanent lockout.
    @Test
    fun `no enrolled credential is always UNLOCKED, even on cold start with lock enabled`() {
        val result =
            appLockState(
                enabled = true,
                timeout = LockTimeout.IMMEDIATE,
                elapsedSinceBackground = null,
                alreadyAuthenticatedThisForeground = false,
                hasEnrolledCredential = false,
            )
        assertEquals(LockState.UNLOCKED, result)
    }

    @Test
    fun `LockTimeout fromId round-trips every shipped option id`() {
        LockTimeout.entries.forEach { option ->
            assertEquals(option, LockTimeout.fromId(option.id))
        }
    }

    @Test
    fun `LockTimeout fromId falls back to After1Min for an unknown id`() {
        assertEquals(LockTimeout.AFTER_1_MIN, LockTimeout.fromId("bogus"))
    }
}
