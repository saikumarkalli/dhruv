package com.dhruv.core.flags

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for [SemVer], the lenient version comparator behind feature-flag `minVersion`
 * gating. Complements HardcodedFeatureFlagResolverTest (which covers the resolver wiring) by pinning
 * down the parsing/ordering edge cases a malformed remote-config version string could hit — a flag
 * must never crash the app over a bad version.
 */
class SemVerTest {
    @Test
    fun `missing trailing components are treated as zero`() {
        assertEquals(0, SemVer.parse("1.2").compareTo(SemVer.parse("1.2.0")))
        assertEquals(0, SemVer.parse("1").compareTo(SemVer.parse("1.0.0")))
    }

    @Test
    fun `ordering is component-numeric, not lexicographic`() {
        assertTrue(SemVer.parse("1.10.0") > SemVer.parse("1.9.9"))
        assertTrue(SemVer.parse("2.0.0") > SemVer.parse("1.99.99"))
        assertTrue(SemVer.parse("1.2.3") < SemVer.parse("1.2.4"))
    }

    @Test
    fun `prerelease and build suffixes on a component are ignored`() {
        // Only the leading digits of each dot-separated component are kept, so a suffix that has no
        // internal '.' is dropped. (A build tag containing a '.' — e.g. "+ci.5" — would be read as an
        // extra component; app versions are clean semver like "1.2.6", so that path is not asserted.)
        assertEquals(0, SemVer.parse("1.2.0-beta").compareTo(SemVer.parse("1.2.0")))
        assertEquals(0, SemVer.parse("1.2.0+build").compareTo(SemVer.parse("1.2.0")))
        assertEquals(0, SemVer.parse("1.2.3-rc1").compareTo(SemVer.parse("1.2.3")))
    }

    @Test
    fun `garbage components fall back to zero instead of throwing`() {
        assertEquals(0, SemVer.parse("abc").compareTo(SemVer.parse("0.0.0")))
        assertEquals(0, SemVer.parse("").compareTo(SemVer.parse("0")))
        // "x.2.y" → 0.2.0
        assertTrue(SemVer.parse("x.2.y") < SemVer.parse("0.3.0"))
        assertTrue(SemVer.parse("x.2.y") > SemVer.parse("0.1.0"))
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        assertEquals(0, SemVer.parse("  1.2.0  ").compareTo(SemVer.parse("1.2.0")))
    }

    @Test
    fun `equal versions compare equal in both directions`() {
        val a = SemVer.parse("3.4.5")
        val b = SemVer.parse("3.4.5")
        assertEquals(0, a.compareTo(b))
        assertEquals(0, b.compareTo(a))
    }
}
