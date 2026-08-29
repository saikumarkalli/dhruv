package com.dhruv.finance.app.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `SET-BR-021` / FR-015: account erasure requires a **typed** confirmation, not a single tap, and
 * the dialog names what is destroyed.
 *
 * Tests the real shipped dialog rather than `ConfirmDangerDialog` in isolation — `:libs:core` has
 * no Compose test infrastructure, and more importantly the guard that matters is the one a user
 * actually hits, wired to the real [DELETE_MY_ACCOUNT_CONFIRM_TEXT]. This row previously closed in
 * the QA catalog on "verified by reading the composable"; that is what this replaces.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class DeleteMyAccountDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `confirm is disabled until the exact word is typed, and the dialog names the consequence`() {
        var confirmed = false
        composeTestRule.setContent {
            DeleteMyAccountDialog(onConfirm = { confirmed = true }, onDismiss = {})
        }

        // Names what is destroyed and that setup restarts — FR-015 / design system §10.
        composeTestRule.onNodeWithText("permanently erases", substring = true).assertIsDisplayed()

        // A single tap must not be enough.
        composeTestRule.onNodeWithText("Delete account").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Delete account").performClick()
        composeTestRule.waitForIdle()
        assertEquals("a tap alone must never erase the account", false, confirmed)
    }

    @Test
    fun `a near-miss does not enable confirm`() {
        composeTestRule.setContent { DeleteMyAccountDialog(onConfirm = {}, onDismiss = {}) }

        // Targeted by editable-text role, not by text: the dialog body itself contains the
        // word ("Type DELETE to confirm"), so onNodeWithText matches that Text, not the field.
        composeTestRule.onNode(hasSetTextAction()).performTextInput("DELET")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Delete account").assertIsNotEnabled()
    }

    @Test
    fun `typing the exact word enables confirm and it fires once`() {
        var confirmCount = 0
        composeTestRule.setContent {
            DeleteMyAccountDialog(onConfirm = { confirmCount++ }, onDismiss = {})
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput(DELETE_MY_ACCOUNT_CONFIRM_TEXT)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Delete account").assertIsEnabled()
        composeTestRule.onNodeWithText("Delete account").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, confirmCount)
    }
}
