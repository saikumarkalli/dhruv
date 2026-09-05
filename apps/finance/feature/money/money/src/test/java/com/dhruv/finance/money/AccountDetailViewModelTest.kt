package com.dhruv.finance.money

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.tracker.model.Account
import com.dhruv.finance.data.tracker.model.AccountType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * MNY-UI-005 (spec.md Story 3, Acceptance Scenarios 3/5): an account whose balance was last
 * confirmed beyond `MoneyConfig.STALENESS_THRESHOLD_DAYS` raises D7's reconcile banner, and
 * reconciling it clears the flag.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val staleAccount =
        Account(
            id = "acc-1",
            name = "Checking",
            type = AccountType.BANK,
            mask = "1234",
            isPrimary = true,
            limitPaise = null,
            dueDay = null,
            openingBalancePaise = 10_000_00,
            reconciledAt = Instant.now().minus(MoneyConfig.STALENESS_THRESHOLD_DAYS + 15L, ChronoUnit.DAYS),
            balancePaise = 10_000_00,
        )

    private fun viewModel(accountRepository: FakeAccountRepository) =
        AccountDetailViewModel(
            accountId = "acc-1",
            accountRepository = accountRepository,
            transactionRepository = FakeTransactionRepository(),
            crashReporter = NoOpCrashReporter,
            performanceTracer = NoOpPerformanceTracer,
        )

    @Test
    fun `an account past the staleness threshold raises the reconcile banner`() =
        runTest {
            val vm = viewModel(FakeAccountRepository(listOf(staleAccount)))

            advanceUntilIdle()

            val state = vm.uiState.value as AccountDetailUiState.Loaded
            assertTrue(state.isStale)
        }

    @Test
    fun `an account reconciled within the threshold does not raise the banner`() =
        runTest {
            val freshAccount = staleAccount.copy(reconciledAt = Instant.now().minus(5, ChronoUnit.DAYS))
            val vm = viewModel(FakeAccountRepository(listOf(freshAccount)))

            advanceUntilIdle()

            val state = vm.uiState.value as AccountDetailUiState.Loaded
            assertFalse(state.isStale)
        }

    @Test
    fun `a never-reconciled account is also flagged stale`() =
        runTest {
            val neverReconciled = staleAccount.copy(reconciledAt = null)
            val vm = viewModel(FakeAccountRepository(listOf(neverReconciled)))

            advanceUntilIdle()

            val state = vm.uiState.value as AccountDetailUiState.Loaded
            assertTrue(state.isStale)
        }

    @Test
    fun `reconciling clears the staleness flag`() =
        runTest {
            val accountRepository = FakeAccountRepository(listOf(staleAccount))
            val vm = viewModel(accountRepository)
            advanceUntilIdle()
            assertTrue((vm.uiState.value as AccountDetailUiState.Loaded).isStale)

            vm.reconcile(10_000_00)
            advanceUntilIdle()

            val state = vm.uiState.value as AccountDetailUiState.Loaded
            assertFalse(state.isStale)
            assertEquals(1, accountRepository.reconcileCalls.size)
            assertEquals("acc-1" to 10_000_00L, accountRepository.reconcileCalls.single())
        }

    // FR-021a: the confirmation names the exact transaction count, resolved before it's shown.
    @Test
    fun `requestDelete resolves the exact transaction count before confirming`() =
        runTest {
            val accountRepository = FakeAccountRepository(listOf(staleAccount), transactionCounts = mapOf("acc-1" to 5))
            val vm = viewModel(accountRepository)
            advanceUntilIdle()

            vm.requestDelete()
            advanceUntilIdle()

            assertEquals(AccountDeletePrompt.Confirm(5), vm.deletePrompt.value)
        }

    @Test
    fun `confirmDelete soft-deletes the account and signals deleted`() =
        runTest {
            val accountRepository = FakeAccountRepository(listOf(staleAccount))
            val vm = viewModel(accountRepository)
            advanceUntilIdle()

            vm.confirmDelete()
            advanceUntilIdle()

            assertEquals(listOf("acc-1"), accountRepository.deletedIds)
            assertTrue(vm.deleted.value)
            assertEquals(AccountDeletePrompt.None, vm.deletePrompt.value)
        }
}
