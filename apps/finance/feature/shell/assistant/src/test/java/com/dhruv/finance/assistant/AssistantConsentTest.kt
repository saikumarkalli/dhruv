package com.dhruv.finance.assistant

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.GeminiRepository
import com.dhruv.settings.AppSettings
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
 * `SET-BR-011`/FR-036: a granted consent survives a force-stop and the assistant does not re-ask.
 * Run against the pre-fix build first and watch it fail — this is a defect fix (the consent flag
 * was in-memory only), not new behaviour.
 *
 * NOTE: `tasks.md` names this file's path as `apps/finance/data/src/test/.../AssistantConsentTest.kt`,
 * but `AssistantViewModel` — the thing that actually holds the defect FR-036 describes — lives in
 * `:apps:finance:feature:assistant`, not `:apps:finance:data`. Written here instead, where the
 * behaviour under test actually is; recorded as a deviation in spec.md's Implementation record
 * rather than silently relocated.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AssistantConsentTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a previously granted consent survives a fresh view model instance, no re-ask`() {
        // "Force-stop and relaunch" is exactly this: the process dies, a brand new AssistantViewModel
        // is constructed on next launch, reading whatever the repository persisted last time.
        val repository = FakeSettingsRepository(initial = AppSettings(assistantConsentGranted = true))
        val vm = AssistantViewModel(GeminiRepository(apiKey = ""), NoOpCrashReporter, NoOpPerformanceTracer, repository)

        assertEquals(AssistantUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `no prior consent still gates on a fresh instance`() {
        val repository = FakeSettingsRepository(initial = AppSettings(assistantConsentGranted = false))
        val vm = AssistantViewModel(GeminiRepository(apiKey = ""), NoOpCrashReporter, NoOpPerformanceTracer, repository)

        assertEquals(AssistantUiState.ConsentNeeded, vm.uiState.value)
    }

    @Test
    fun `granting consent persists it for the next instance`() =
        runTest(dispatcher) {
            val repository = FakeSettingsRepository()
            val first = AssistantViewModel(GeminiRepository(apiKey = ""), NoOpCrashReporter, NoOpPerformanceTracer, repository)

            first.grantConsent()
            advanceUntilIdle()

            val second = AssistantViewModel(GeminiRepository(apiKey = ""), NoOpCrashReporter, NoOpPerformanceTracer, repository)
            assertEquals(AssistantUiState.Idle, second.uiState.value)
        }

    @Test
    fun `withdrawing consent returns to the gate and makes no request until granted again`() =
        runTest(dispatcher) {
            val repository = FakeSettingsRepository(initial = AppSettings(assistantConsentGranted = true))
            val vm = AssistantViewModel(GeminiRepository(apiKey = ""), NoOpCrashReporter, NoOpPerformanceTracer, repository)

            vm.withdrawConsent()
            assertEquals(AssistantUiState.ConsentNeeded, vm.uiState.value)

            // FR-037: no request fires between withdrawal and the next grant — ask() is a no-op
            // while gated, proven the same way the pre-existing consent-gate tests prove it.
            vm.ask("explain 2 + 2")
            advanceUntilIdle()
            assertEquals(AssistantUiState.ConsentNeeded, vm.uiState.value)

            advanceUntilIdle()
            val persisted = AssistantViewModel(GeminiRepository(apiKey = ""), NoOpCrashReporter, NoOpPerformanceTracer, repository)
            assertEquals(AssistantUiState.ConsentNeeded, persisted.uiState.value)
        }
}
