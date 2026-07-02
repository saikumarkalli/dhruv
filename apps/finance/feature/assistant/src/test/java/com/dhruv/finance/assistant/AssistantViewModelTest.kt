package com.dhruv.finance.assistant

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regression tests for the assistant's **DPDP consent gate** (PLATFORM.md §8, ADR-0005): no data may
 * reach Gemini before the user grants consent.
 *
 * The gate is verified through observable state: while [AssistantUiState.ConsentNeeded], `ask()` must
 * not transition to Loading (the first thing the network coroutine does), proving the Gemini call
 * path is never entered. A blank-API-key [GeminiRepository] short-circuits to a failure Result with
 * no network I/O, so the post-consent path is exercised deterministically and offline.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private fun newVm() =
        AssistantViewModel(GeminiRepository(apiKey = ""), NoOpCrashReporter, NoOpPerformanceTracer)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state gates on consent`() {
        assertEquals(AssistantUiState.ConsentNeeded, newVm().uiState.value)
    }

    @Test
    fun `ask before consent never enters the gemini path`() =
        runTest(dispatcher) {
            val vm = newVm()
            vm.ask("explain 2 + 2")
            advanceUntilIdle()
            // DPDP gate held: no Loading, no network — state is unchanged.
            assertEquals(AssistantUiState.ConsentNeeded, vm.uiState.value)
        }

    @Test
    fun `grantConsent opens the gate to Idle`() {
        val vm = newVm()
        vm.grantConsent()
        assertEquals(AssistantUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `grantConsent only fires from ConsentNeeded`() {
        val vm = newVm()
        vm.grantConsent()
        vm.grantConsent() // no-op the second time
        assertEquals(AssistantUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `blank prompt after consent stays Idle`() =
        runTest(dispatcher) {
            val vm = newVm()
            vm.grantConsent()
            vm.ask("   ")
            advanceUntilIdle()
            assertEquals(AssistantUiState.Idle, vm.uiState.value)
        }

    @Test
    fun `ask after consent enters the gemini path`() =
        runTest(dispatcher) {
            val vm = newVm()
            vm.grantConsent()
            vm.ask("explain 2 + 2")
            advanceUntilIdle()
            // Past the gate the call path runs: Loading is set, and the unconfigured key degrades to
            // a graceful Error (never a crash, never stuck in ConsentNeeded/Idle).
            val state = vm.uiState.value
            assertTrue(
                "expected Loading or Error but was $state",
                state is AssistantUiState.Loading || state is AssistantUiState.Error,
            )
        }
}
