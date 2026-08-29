package com.dhruv.finance.app.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.finance.calculator.R as CalculatorR
import com.dhruv.finance.currency.R as CurrencyR
import com.dhruv.settings.contribution.SettingsContribution
import com.dhruv.settings.contribution.SettingsGroup
import com.dhruv.settings.contribution.SettingsRow
import kotlinx.coroutines.flow.flow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `SET-ARCH-007` / contract §4 rule 12 / constitution IV: a contribution that throws while
 * producing its rows degrades to that module's own error card, and every other entry keeps
 * working. Proven at the level where the failure actually surfaces — a row's `Flow` throwing
 * during collection (`SettingsRowRenderer`'s `onError` → `ModuleSettingsScreen`'s `FeatureHost`) —
 * since `SettingsContribution` is a plain data class with no lazy computation of its own for a
 * "contribution that throws" to mean anything else.
 */
// application = Application::class: these tests don't need the real DI graph, and the real
// CalculatorApplication.onCreate() calls startKoin() — Robolectric re-instantiates the Application
// per test class within the same JVM fork, so the real Application collides across test classes
// with KoinAppAlreadyStartedException (found running this suite; see also QuickRowMirrorTest).
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ModuleEntryIsolationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val throwingContribution =
        SettingsContribution(
            moduleKey = "throwing_module",
            title = CalculatorR.string.settings_calculator_title,
            summary = CalculatorR.string.settings_calculator_summary,
            order = 0,
            groups =
                listOf(
                    SettingsGroup(
                        label = null,
                        rows =
                            listOf(
                                SettingsRow.Info(
                                    key = "broken_row",
                                    label = CalculatorR.string.settings_calculator_precision_preview_label,
                                    description = CalculatorR.string.settings_calculator_precision_description,
                                    value = flow { throw IllegalStateException("boom") },
                                ),
                            ),
                    ),
                ),
        )

    private val healthyContribution =
        SettingsContribution(
            moduleKey = "healthy_module",
            title = CurrencyR.string.settings_currency_title,
            summary = CurrencyR.string.settings_currency_summary,
            order = 1,
            groups =
                listOf(
                    SettingsGroup(
                        label = null,
                        rows =
                            listOf(
                                SettingsRow.Info(
                                    key = "healthy_row",
                                    label = CurrencyR.string.settings_currency_supported_label,
                                    description = CurrencyR.string.settings_currency_supported_description,
                                    value = flow { emit("13") },
                                ),
                            ),
                    ),
                ),
        )

    @Test
    fun `a row whose flow throws degrades to this entry's error card, not a crash`() {
        composeTestRule.setContent {
            ModuleSettingsScreen(
                contribution = throwingContribution,
                crashReporter = NoOpCrashReporter,
                settingsRepository = FakeSettingsRepository(),
            )
        }

        // FeatureErrorCard's own fixed copy (FeatureHost.kt) — not a raw crash, not a blank screen.
        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
    }

    @Test
    fun `a throwing entry's error card does not take down a healthy entry rendered alongside it`() {
        // Two ModuleSettingsScreen instances in one composition, each owning only its own
        // contribution's failure state — this is what "the tier and every other entry keep
        // working" actually means: one entry's caught exception must not propagate past its own
        // FeatureHost boundary into a sibling entry's render tree.
        composeTestRule.setContent {
            // Each ModuleSettingsScreen fills its available height — real usage never stacks two,
            // it shows one full-screen instance behind its own route at a time. A bounded Column
            // with each child weighted 1f splits the space between them so both are actually
            // visible for this test's assertions (an un-weighted fillMaxSize child claims the
            // whole bound for itself and pushes the next one outside the Column's own bounds).
            Column(modifier = Modifier.height(2000.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    ModuleSettingsScreen(
                        contribution = throwingContribution,
                        crashReporter = NoOpCrashReporter,
                        settingsRepository = FakeSettingsRepository(),
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ModuleSettingsScreen(
                        contribution = healthyContribution,
                        crashReporter = NoOpCrashReporter,
                        settingsRepository = FakeSettingsRepository(),
                    )
                }
            }
        }
        // Both rows resolve via a LaunchedEffect collecting their Flow, not the Compose frame
        // clock alone — flushed explicitly before asserting (same reason as SettingsRowWriteTest).
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
        composeTestRule.onNodeWithText("13").assertIsDisplayed()
    }
}
