package com.dhruv.settings.contribution

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.settings.SettingsRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `SET-BR-005` / FR-032: turning an optional module off then on retains and restores its stored
 * preferences — the module-enabled flag is its own key (`module_enabled_<moduleKey>`,
 * data-model.md §3), never a reset switch over the module's other preferences.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ModuleToggleTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    // Robolectric's `preferencesDataStore` file backing "app_settings" is not reset between test
    // methods in this class (it's keyed by file path, not by Context instance) — each test uses
    // its own unique moduleKey so a write in one test can never leak into another's assertions.

    @Test
    fun `a module not yet toggled defaults to enabled`() =
        runTest {
            val repo = SettingsRepositoryImpl(context, NoOpCrashReporter)
            assertTrue(repo.isModuleEnabled("module_toggle_test_default").first())
        }

    @Test
    fun `turning a module off then on retains other stored preferences`() =
        runTest {
            val repo = SettingsRepositoryImpl(context, NoOpCrashReporter)
            val moduleKey = "module_toggle_test_retain"

            // An unrelated preference, standing in for "the module's own stored settings".
            repo.update { copy(hideAmounts = true) }

            repo.setModuleEnabled(moduleKey, enabled = false)
            assertEquals(false, repo.isModuleEnabled(moduleKey).first())

            repo.setModuleEnabled(moduleKey, enabled = true)
            assertEquals(true, repo.isModuleEnabled(moduleKey).first())

            // Disabling and re-enabling the module touched only its own key — nothing else reset.
            assertEquals(true, repo.observe().first().hideAmounts)
        }

    @Test
    fun `two modules' enabled flags are independent`() =
        runTest {
            val repo = SettingsRepositoryImpl(context, NoOpCrashReporter)

            repo.setModuleEnabled("module_toggle_test_indep_a", enabled = false)

            assertEquals(false, repo.isModuleEnabled("module_toggle_test_indep_a").first())
            assertEquals(true, repo.isModuleEnabled("module_toggle_test_indep_b").first())
        }

    @Test
    fun `a stored flag for a module no longer registered is inert, never an orphan entry`() =
        runTest {
            // `SettingsRegistry` only ever enumerates the `SettingsContribution` list Koin hands it
            // (`SettingsRegistryTest`) — it never reads `module_enabled_*` keys directly, so a key
            // left behind by a module removed from the build has nothing to attach an entry to.
            // This is satisfied by construction; this test only proves the repository side is
            // equally indifferent to an unknown moduleKey (no crash, no special-casing needed).
            val repo = SettingsRepositoryImpl(context, NoOpCrashReporter)
            repo.setModuleEnabled("a_module_removed_from_the_build", enabled = false)
            assertEquals(false, repo.isModuleEnabled("a_module_removed_from_the_build").first())
        }
}
