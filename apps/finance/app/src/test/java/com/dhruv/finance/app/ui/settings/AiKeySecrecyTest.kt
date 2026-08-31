package com.dhruv.finance.app.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dhruv.finance.app.R
import com.dhruv.settings.contribution.SettingsRow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `SET-BR-012` / FR-038: a saved key never appears in full in any screen state, and is removable
 * in one action. The masked representation is a fixed constant (`settings_secret_masked`), not
 * computed from the real value, so it structurally cannot leak length or characters (CHK034).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class AiKeySecrecyTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val realKey = "AIzaSyABCDEFGHIJKLMNOPQRSTUVWXYZ1234567"

    @Test
    fun `a stored key never appears in full on screen, only the fixed mask`() {
        val row =
            SettingsRow.SecretText(
                key = "gemini_api_key",
                label = R.string.settings_module_enable_label,
                description = R.string.settings_module_enable_description,
                value = MutableStateFlow(realKey),
                onSave = {},
                onRemove = {},
            )
        composeTestRule.setContent { SettingsRowRenderer(row = row) }
        composeTestRule.waitForIdle()

        assertThrows(AssertionError::class.java) {
            composeTestRule.onNodeWithText(realKey, substring = true).assertIsDisplayed()
        }
        composeTestRule.onNodeWithText("••••••••").assertIsDisplayed()
    }

    @Test
    fun `remove is a single action`() {
        var removed = false
        val row =
            SettingsRow.SecretText(
                key = "gemini_api_key",
                label = R.string.settings_module_enable_label,
                description = R.string.settings_module_enable_description,
                value = MutableStateFlow(realKey),
                onSave = {},
                onRemove = { removed = true },
            )
        composeTestRule.setContent { SettingsRowRenderer(row = row) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Remove").performClick()
        composeTestRule.waitForIdle()

        assertEquals(true, removed)
    }

    @Test
    fun `an unset key shows the placeholder field, not a mask`() {
        val row =
            SettingsRow.SecretText(
                key = "gemini_api_key",
                label = R.string.settings_module_enable_label,
                description = R.string.settings_module_enable_description,
                value = MutableStateFlow(null),
                onSave = {},
                onRemove = {},
            )
        composeTestRule.setContent { SettingsRowRenderer(row = row) }
        composeTestRule.waitForIdle()

        assertThrows(AssertionError::class.java) {
            composeTestRule.onNodeWithText("••••••••").assertIsDisplayed()
        }
    }
}
