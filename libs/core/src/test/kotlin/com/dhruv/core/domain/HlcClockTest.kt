package com.dhruv.core.domain

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the Hybrid Logical Clock (ADR-0004) that keys Last-Write-Wins conflict
 * resolution. [HlcClock] is a process-wide singleton with monotonic state, so every assertion here
 * is order-independent: logical time only ever advances, and stamps are always well-formed.
 * `init(Context)` is not exercised (it touches Android Settings) — the node id defaults to 8 zeros.
 */
class HlcClockTest {
    private data class Stamp(
        val l: Long,
        val c: Int,
        val node: String,
    )

    private val stampFormat = Regex("""\d+-\d{4}-.{8}""")

    private fun parse(s: String): Stamp {
        val (l, c, node) = s.split('-')
        return Stamp(l.toLong(), c.toInt(), node)
    }

    private fun Stamp.after(other: Stamp) = l > other.l || (l == other.l && c > other.c)

    @Test
    fun `now produces a well-formed epoch-counter-node stamp`() {
        assertTrue(HlcClock.now().matches(stampFormat))
    }

    @Test
    fun `successive now calls are strictly increasing in logical order`() {
        val a = parse(HlcClock.now())
        val b = parse(HlcClock.now())
        assertTrue("$b must be logically after $a", b.after(a))
    }

    @Test
    fun `a burst of stamps is monotonic and never repeats`() {
        val stamps = (1..100).map { parse(HlcClock.now()) }
        stamps.zipWithNext().forEach { (x, y) ->
            assertTrue("HLC must never regress: $y after $x", y.after(x))
        }
    }

    @Test
    fun `receive advances logical time to a future remote stamp`() {
        val future = System.currentTimeMillis() + 10_000_000L
        val out = parse(HlcClock.receive("$future-0005-remote01"))
        assertTrue("received future L must not regress", out.l >= future)
    }

    @Test
    fun `receive with a malformed remote falls back to a valid stamp`() {
        assertTrue(HlcClock.receive("not-a-real-stamp").matches(stampFormat))
        assertTrue(HlcClock.receive("").matches(stampFormat))
    }

    @Test
    fun `logical time never regresses below a received future clock`() {
        val future = System.currentTimeMillis() + 5_000_000L
        HlcClock.receive("$future-0000-nodeAAAA")
        assertTrue(parse(HlcClock.now()).l >= future)
    }
}
