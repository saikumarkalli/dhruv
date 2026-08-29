package com.dhruv.finance.app.ui.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * `SET-UI-013`: the reported version matches the installed build, including build number.
 * `SET-UI-014`: a failed update check reports the failure and never silently reports "current".
 * No real [UpdateChecker] is wired in production yet (T102 — the update channel doesn't exist),
 * so this proves the view model's own logic against a fake checker; `SET-UI-014` itself closes
 * **deferred** in the catalog, with that reason (T107).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppDetailsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `reports the exact version name and build number it was given`() {
        val vm = AppDetailsViewModel(versionName = "2.0.4", versionCode = 42L, updateChecker = null)
        assertEquals("2.0.4", vm.versionName)
        assertEquals(42L, vm.versionCode)
    }

    @Test
    fun `no update checker means no update check is offered`() {
        val vm = AppDetailsViewModel(versionName = "2.0.4", versionCode = 42L, updateChecker = null)
        assertEquals(false, vm.updateCheckAvailable)
    }

    @Test
    fun `a successful check with no newer version reports current`() =
        runTest(dispatcher) {
            val vm =
                AppDetailsViewModel(
                    versionName = "2.0.4",
                    versionCode = 42L,
                    updateChecker = UpdateChecker { UpdateCheckResult.Current },
                )
            vm.checkForUpdate()
            advanceUntilIdle()
            assertEquals(UpdateCheckResult.Current, vm.updateCheckResult.value)
        }

    @Test
    fun `a successful check with a newer version reports it available`() =
        runTest(dispatcher) {
            val vm =
                AppDetailsViewModel(
                    versionName = "2.0.4",
                    versionCode = 42L,
                    updateChecker = UpdateChecker { UpdateCheckResult.Available("2.1.0") },
                )
            vm.checkForUpdate()
            advanceUntilIdle()
            assertEquals(UpdateCheckResult.Available("2.1.0"), vm.updateCheckResult.value)
        }

    @Test
    fun `a failed check reports the failure, never silently current`() =
        runTest(dispatcher) {
            val vm =
                AppDetailsViewModel(
                    versionName = "2.0.4",
                    versionCode = 42L,
                    updateChecker = UpdateChecker { throw java.io.IOException("no network") },
                )
            vm.checkForUpdate()
            advanceUntilIdle()
            val result = vm.updateCheckResult.value
            assert(result is UpdateCheckResult.Failed) { "expected Failed but was $result" }
        }
}
