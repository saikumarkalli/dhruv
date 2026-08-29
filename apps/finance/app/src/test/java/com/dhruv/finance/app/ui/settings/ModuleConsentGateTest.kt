package com.dhruv.finance.app.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.dhruv.core.flags.FeatureFlagResolver
import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.finance.assistant.R as AssistantR
import com.dhruv.settings.contribution.SettingsContribution
import com.dhruv.settings.contribution.SettingsGroup
import com.dhruv.settings.contribution.SettingsRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val requiresConsentResolver =
    object : FeatureFlagResolver {
        override fun isEnabled(key: String) = true

        override fun requiresConsent(key: String) = key == "gated_module"
    }

/**
 * `SET-UI-008` / FR-035: a module entry whose controls need an ungranted consent states which
 * consent and offers the route to grant it, rather than rendering inert controls.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ModuleConsentGateTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val gatedContribution =
        SettingsContribution(
            moduleKey = "gated_module",
            title = AssistantR.string.settings_assistant_title,
            summary = AssistantR.string.settings_assistant_summary,
            order = 0,
            groups =
                listOf(
                    SettingsGroup(
                        label = null,
                        rows =
                            listOf(
                                SettingsRow.Info(
                                    key = "row",
                                    label = AssistantR.string.settings_assistant_title,
                                    description = AssistantR.string.settings_assistant_summary,
                                    value = flowOf("value"),
                                ),
                            ),
                    ),
                ),
            consentGranted = MutableStateFlow(false),
            consentRequiredMessage = AssistantR.string.settings_assistant_consent_needed_message,
            // Must be optional for the off-state to be reachable at all: since FR-032's control
            // is offered only to optional modules (FR-033 — a non-optional one is the content of
            // a tab and is always on), a non-optional fixture can never show the disabled state.
            optional = true,
        )

    @Test
    fun `an ungranted consent shows what's needed and hides the module's rows`() {
        composeTestRule.setContent {
            ModuleSettingsScreen(
                contribution = gatedContribution,
                crashReporter = NoOpCrashReporter,
                settingsRepository = FakeSettingsRepository(),
                resolver = requiresConsentResolver,
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Consent needed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Requires the 'Ask Dhruv about my money' consent").assertIsDisplayed()
        composeTestRule.onNodeWithText("value").assertIsNotDisplayed()
    }

    @Test
    fun `a granted consent shows the module's rows normally`() {
        val granted =
            gatedContribution.copy(consentGranted = MutableStateFlow(true))
        composeTestRule.setContent {
            ModuleSettingsScreen(
                contribution = granted,
                crashReporter = NoOpCrashReporter,
                settingsRepository = FakeSettingsRepository(),
                resolver = requiresConsentResolver,
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("value").assertIsDisplayed()
    }

    /**
     * Regression: the consent flag used to be collected *inside* the `when`'s branch condition,
     * behind a short-circuiting `&&`, so this exact combination (module turned off, which matches
     * the earlier branch, on a contribution that also happens to be consent-gated) skipped the
     * `collectAsState` call entirely — a conditionally-invoked @Composable. Toggling the module
     * back on then re-invoked it at the same slot, which is the slot-table corruption case.
     * Both reads are hoisted above the `when` now; this drives the off→on transition to prove it.
     */
    @Test
    fun `a disabled module that also needs consent shows the disabled state, then its gate when re-enabled`() {
        val repository = FakeSettingsRepository()
        composeTestRule.setContent {
            ModuleSettingsScreen(
                contribution = gatedContribution,
                crashReporter = NoOpCrashReporter,
                settingsRepository = repository,
                resolver = requiresConsentResolver,
            )
        }
        composeTestRule.waitForIdle()

        // Turn the module off — the disabled empty state wins over the consent gate. Written
        // straight to the backing flow, never through the `suspend` setter (see the fake's own
        // note: `runBlocking` here deadlocks against `waitForIdle`).
        repository.moduleEnabledMap.value = mapOf("gated_module" to false)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("This module is turned off. Turn it back on above to see its settings again.")
            .assertIsDisplayed()

        // Back on — the consent gate is now what shows. Recomposing across this transition is
        // exactly what used to change the composable-call count at this slot.
        repository.moduleEnabledMap.value = mapOf("gated_module" to true)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Consent needed").assertIsDisplayed()
    }

    @Test
    fun `a module that doesn't require consent is unaffected by an ungranted flag`() {
        val ungatedButUngranted = gatedContribution.copy(moduleKey = "ungated_module")
        composeTestRule.setContent {
            ModuleSettingsScreen(
                contribution = ungatedButUngranted,
                crashReporter = NoOpCrashReporter,
                settingsRepository = FakeSettingsRepository(),
                resolver = requiresConsentResolver,
            )
        }
        composeTestRule.waitForIdle()

        // requiresConsentResolver only gates "gated_module" — this one renders normally.
        composeTestRule.onNodeWithText("value").assertIsDisplayed()
    }
}
