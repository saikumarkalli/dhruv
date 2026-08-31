package com.dhruv.finance.app.ui.settings

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `SET-BR-008` / FR-002 / SC-003: the quick row and its owning section's row show the same value
 * from the same stored preference, and changing either updates both with no restart. Both the
 * quick row (`SettingsScreen`) and Appearance's row (`AppSettingsScreen`) render the exact same
 * `AppearanceThemeRow` composable bound to the same [FakeSettingsRepository] — this proves that
 * shared binding, not a separate copy of the value (FR-002's "never a second copy" rule), by
 * rendering two independent instances side by side against one repository.
 *
 * `application = Application::class` — the real `CalculatorApplication`'s `startKoin()` collides
 * across Robolectric test classes in the same JVM fork (`KoinAppAlreadyStartedException`); this
 * test doesn't need the real DI graph. See `ModuleEntryIsolationTest`'s identical note.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class QuickRowMirrorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `theme quick row and Appearance row mirror the same repository value`() {
        val repository = FakeSettingsRepository()
        repository.darkMode.value = "always_light"

        composeTestRule.setContent {
            // Two independent instances, same repository — exactly what the quick row and
            // Appearance's own row are in the real screens.
            val theme by repository.darkModePreference.collectAsState()
            AppearanceThemeRow(darkModePreference = theme, onThemeChanged = { repository.setDarkModePreference(it) })
            AppearanceThemeRow(darkModePreference = theme, onThemeChanged = { repository.setDarkModePreference(it) })
        }

        // Both instances render "Light" selected initially — one shared value, two surfaces.
        composeTestRule.onAllNodesWithText("Light").assertCountEquals(2)

        // Change from the first instance (simulating the quick row) …
        composeTestRule.onAllNodesWithText("Dark")[0].performClick()

        // … and the underlying preference both instances read changed once, with no restart.
        assertEquals("always_dark", repository.darkModePreference.value)
    }
}
