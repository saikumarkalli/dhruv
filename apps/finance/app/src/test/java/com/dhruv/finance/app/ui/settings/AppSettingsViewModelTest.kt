package com.dhruv.finance.app.ui.settings

import com.dhruv.core.observability.NoOpCrashReporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsViewModelTest {
    // SET-BR-017: app lock cannot be enabled with no enrolled credential, against a stubbed
    // credential-availability check.
    @Test
    fun `enabling app lock with no enrolled credential fails and does not persist`() =
        runTest {
            val repository = FakeSettingsRepository()
            val viewModel = AppSettingsViewModel(repository, hasEnrolledCredential = { false }, NoOpCrashReporter)

            val result = viewModel.setAppLockEnabled(true)

            assertTrue(result.isFailure)
            assertFalse(repository.observe().first().biometricEnabled)
        }

    @Test
    fun `enabling app lock with an enrolled credential succeeds and persists`() =
        runTest {
            val repository = FakeSettingsRepository()
            val viewModel = AppSettingsViewModel(repository, hasEnrolledCredential = { true }, NoOpCrashReporter)

            val result = viewModel.setAppLockEnabled(true)

            assertTrue(result.isSuccess)
            assertTrue(repository.observe().first().biometricEnabled)
        }

    @Test
    fun `disabling app lock never needs a credential check`() =
        runTest {
            val repository = FakeSettingsRepository()
            val viewModel = AppSettingsViewModel(repository, hasEnrolledCredential = { false }, NoOpCrashReporter)

            val result = viewModel.setAppLockEnabled(false)

            assertTrue(result.isSuccess)
        }

    // SET-BR-010: the notification master off suppresses every module's alerts regardless of that
    // module's own setting.
    @Test
    fun `an alert is effectively enabled only when both the master and the module's own switch are on`() {
        assertTrue(isAlertEffectivelyEnabled(notificationsMaster = true, moduleAlertEnabled = true))
        assertFalse(isAlertEffectivelyEnabled(notificationsMaster = false, moduleAlertEnabled = true))
        assertFalse(isAlertEffectivelyEnabled(notificationsMaster = true, moduleAlertEnabled = false))
        assertFalse(isAlertEffectivelyEnabled(notificationsMaster = false, moduleAlertEnabled = false))
    }
}
