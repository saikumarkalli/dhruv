package com.dhruv.finance.app.di

import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.NoOpCrashReporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeCrashReporter : CrashReporter {
    var lastException: Throwable? = null
    override fun setModule(name: String) = Unit
    override fun recordException(t: Throwable) {
        lastException = t
    }
    override fun log(message: String) = Unit
}

// Mirrors platform/feature-flags/dhruv-finance.json — kept in sync manually since this test
// guards against the schema (not the literal values) drifting unnoticed.
private val FINANCE_FLAGS_JSON = """
{
  "app": "finance",
  "features": {
    "calculator":  { "enabled": true,  "minVersion": "1.0.0" },
    "loans":       { "enabled": true,  "minVersion": "1.0.0" },
    "investments": { "enabled": true,  "minVersion": "1.0.0" },
    "tax":         { "enabled": true,  "minVersion": "1.0.0" },
    "everyday":    { "enabled": true,  "minVersion": "1.0.0" },
    "currency":    { "enabled": true,  "minVersion": "1.0.0" },
    "unit":        { "enabled": true,  "minVersion": "1.0.0" },
    "date":        { "enabled": false, "minVersion": "1.0.0" },
    "time":        { "enabled": false, "minVersion": "1.0.0" },
    "assistant":   { "enabled": true,  "minVersion": "1.2.0", "requiresConsent": true }
  }
}
""".trimIndent()

class FeatureFlagAssetLoaderTest {

    @Test
    fun `parses all flags from the finance JSON schema`() {
        val flags = parseFeatureFlags(FINANCE_FLAGS_JSON, NoOpCrashReporter)

        assertEquals(10, flags.size)
        assertTrue(flags.getValue("calculator").enabled)
        assertEquals("1.0.0", flags.getValue("calculator").minVersion)
        assertFalse(flags.getValue("date").enabled)
        assertFalse(flags.getValue("time").enabled)

        val assistant = flags.getValue("assistant")
        assertTrue(assistant.enabled)
        assertEquals("1.2.0", assistant.minVersion)
        assertTrue(assistant.requiresConsent)
    }

    @Test
    fun `falls back to calculator-only defaults on malformed JSON`() {
        val crashReporter = FakeCrashReporter()
        val flags = parseFeatureFlags("not valid json", crashReporter)

        assertEquals(1, flags.size)
        assertTrue(flags.getValue("calculator").enabled)
        assertNotNull(crashReporter.lastException)
    }
}
