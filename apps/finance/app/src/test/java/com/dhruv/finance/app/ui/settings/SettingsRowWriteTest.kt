package com.dhruv.finance.app.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dhruv.finance.app.R
import com.dhruv.settings.contribution.SettingsRow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `SET-BR-007` / FR-042 / contract §2 rule 9: a row write persists immediately with no save
 * action, and a failing write reverts the displayed value and states why.
 *
 * `application = Application::class` — the real `CalculatorApplication`'s `startKoin()` collides
 * across Robolectric test classes in the same JVM fork (`KoinAppAlreadyStartedException`); this
 * test doesn't need the real DI graph. See `ModuleEntryIsolationTest`'s identical note.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class SettingsRowWriteTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `a toggle write persists immediately with no save action`() {
        val persisted = MutableStateFlow(false)
        val row =
            SettingsRow.Toggle(
                key = "test_toggle",
                label = R.string.settings_app_lock_quick_row_label,
                description = R.string.settings_app_lock_description,
                value = persisted,
                onChange = { persisted.value = it },
            )

        composeTestRule.setContent { SettingsRowRenderer(row = row) }

        // Targeted by toggle role, not the "App lock" text: SwitchRow's outer Row isn't
        // clickable — only the Switch itself is (SwitchRow.kt) — so the merged-tree text node
        // does not reliably carry the click action.
        composeTestRule.onNode(isToggleable()).performClick()
        // The write runs on rememberCoroutineScope(), not the Compose frame clock — Robolectric's
        // idle-waiting doesn't drain it automatically, so it's flushed explicitly before asserting.
        composeTestRule.waitForIdle()

        // No separate "Save" action exists anywhere in this composable tree — the write already
        // landed in the backing flow the instant the row was tapped.
        assertTrue(persisted.value)
    }

    @Test
    fun `a failing write reverts the displayed value and states why`() {
        val row =
            SettingsRow.Toggle(
                key = "test_toggle",
                label = R.string.settings_app_lock_quick_row_label,
                description = R.string.settings_app_lock_description,
                value = MutableStateFlow(false),
                onChange = { throw IllegalStateException("write failed") },
            )

        composeTestRule.setContent { SettingsRowRenderer(row = row) }

        // Targeted by toggle role, not the "App lock" text: SwitchRow's outer Row isn't
        // clickable — only the Switch itself is (SwitchRow.kt) — so the merged-tree text node
        // does not reliably carry the click action.
        composeTestRule.onNode(isToggleable()).performClick()
        composeTestRule.waitForIdle()

        // Reverted to the (unchanged) persisted value …
        composeTestRule.onNode(isToggleable()).assertIsOff()
        // … and the row states why, replacing its description with the failure message.
        composeTestRule.onNodeWithText("Couldn't save — try again").assertIsDisplayed()
    }
}
