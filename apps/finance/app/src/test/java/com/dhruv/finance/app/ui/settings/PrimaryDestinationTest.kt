package com.dhruv.finance.app.ui.settings

import com.dhruv.core.navigation.TabKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `SET-UI-010` / FR-033: primary navigation destinations MUST NOT be user-hideable.
 *
 * `ModuleSettingsScreen` offers its on/off control generically for every registered contribution,
 * so the guard against ever hiding a primary destination is this: no contribution's `moduleKey`
 * may equal a [TabKey] name. Checked against the **real** contributions
 * ([realSettingsContributions]) rather than a hand-typed key set — the earlier version listed the
 * four keys inline, so a fifth module could have collided with a tab and this test would have
 * passed without ever looking at it.
 */
class PrimaryDestinationTest {
    @Test
    fun `no registered module key collides with a primary tab`() {
        val moduleKeys = realSettingsContributions().map { it.moduleKey }.toSet()
        val tabKeyNames = TabKey.entries.map { it.name.lowercase() }.toSet()

        assertTrue(
            "a moduleKey must never equal a TabKey — that module's on/off control would be " +
                "hiding a primary navigation destination: ${moduleKeys intersect tabKeyNames}",
            (moduleKeys intersect tabKeyNames).isEmpty(),
        )
    }

    @Test
    fun `the guard is not vacuous — real contributions were actually resolved`() {
        // Without this, an empty contribution list would make the intersection check above pass
        // trivially and silently.
        assertTrue(realSettingsContributions().isNotEmpty())
    }

    /**
     * FR-033, the half the name-collision check above cannot see: `calculator` is the **content of
     * the Calc tab**, yet its `moduleKey` collides with no `TabKey` name. Only `optional = false`
     * keeps `ModuleSettingsScreen` from offering it an on/off control — so that flag, not the name,
     * is what actually prevents a primary destination becoming user-hideable.
     */
    @Test
    fun `a module that is a tab's own content is not marked optional`() {
        val calculator = realSettingsContributions().single { it.moduleKey == "calculator" }
        assertFalse(
            "calculator is the Calc tab's content — marking it optional would offer a control that " +
                "hides a primary navigation destination (FR-033)",
            calculator.optional,
        )
    }

    /** The inverse: the detail-route modules genuinely are hideable, so FR-032's control has real
     * subjects and `rememberModuleEnabled`'s gate is not dead code. */
    @Test
    fun `the shell detail-route modules are marked optional`() {
        val optionalKeys = realSettingsContributions().filter { it.optional }.map { it.moduleKey }.toSet()
        assertEquals(setOf("currency", "unit", "assistant"), optionalKeys)
    }
}
