package com.dhruv.core.flags

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HardcodedFeatureFlagResolverTest {

    private val flags = mapOf(
        "calculator" to FeatureFlag(enabled = true),
        "date" to FeatureFlag(enabled = false),
        "assistant" to FeatureFlag(enabled = true, minVersion = "1.2.0", requiresConsent = true),
    )

    @Test
    fun `enabled flag with no minVersion is on at any version`() {
        assertTrue(HardcodedFeatureFlagResolver(flags, "1.0").isEnabled("calculator"))
    }

    @Test
    fun `disabled flag is off regardless of version`() {
        assertFalse(HardcodedFeatureFlagResolver(flags, "9.9.9").isEnabled("date"))
    }

    @Test
    fun `enabled flag below minVersion stays gated`() {
        assertFalse(HardcodedFeatureFlagResolver(flags, "1.0").isEnabled("assistant"))
        assertFalse(HardcodedFeatureFlagResolver(flags, "1.1.9").isEnabled("assistant"))
    }

    @Test
    fun `enabled flag at or above minVersion surfaces`() {
        assertTrue(HardcodedFeatureFlagResolver(flags, "1.2.0").isEnabled("assistant"))
        assertTrue(HardcodedFeatureFlagResolver(flags, "2.0").isEnabled("assistant"))
    }

    @Test
    fun `unknown key is disabled and consent-free`() {
        val resolver = HardcodedFeatureFlagResolver(flags, "1.0")
        assertFalse(resolver.isEnabled("nope"))
        assertFalse(resolver.requiresConsent("nope"))
    }

    @Test
    fun `requiresConsent reflects the flag`() {
        val resolver = HardcodedFeatureFlagResolver(flags, "1.0")
        assertTrue(resolver.requiresConsent("assistant"))
        assertFalse(resolver.requiresConsent("calculator"))
    }

    @Test
    fun `semver compares numeric head and ignores suffix`() {
        assertTrue(SemVer.parse("1.2.0") > SemVer.parse("1.1.9"))
        assertEquals(0, SemVer.parse("1.2").compareTo(SemVer.parse("1.2.0")))
        assertTrue(SemVer.parse("1.2.0-beta") >= SemVer.parse("1.2.0"))
    }
}
