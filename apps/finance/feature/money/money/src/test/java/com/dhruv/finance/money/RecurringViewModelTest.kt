package com.dhruv.finance.money

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.tracker.model.PendingEntry
import com.dhruv.finance.data.tracker.model.RecurringTemplate
import com.dhruv.finance.data.tracker.model.SuggestionStatus
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
import java.time.Instant
import java.time.LocalDate

/** MNY-UI-008: MONTHLY IN/OUT totals, NEXT 30 DAYS ordered by date with correct auto-debit vs
 * variable-amount tags, and the PAUSED section. */
@OptIn(ExperimentalCoroutinesApi::class)
class RecurringViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun template(
        id: String,
        type: String,
        amountPaise: Long,
        nextRun: LocalDate,
        amountIsVariable: Boolean = false,
        paused: Boolean = false,
    ) = RecurringTemplate(
        id = id,
        template = mapOf("type" to type, "amountPaise" to amountPaise, "payee" to id),
        rrule = "FREQ=MONTHLY",
        nextRun = nextRun,
        amountIsVariable = amountIsVariable,
        paused = paused,
        pausedAt = if (paused) Instant.now() else null,
    )

    @Test
    fun `monthly IN and OUT sum active templates by type`() =
        runTest {
            val salary = template("t1", "INCOME", 50_000_00, LocalDate.now().plusDays(5))
            val rent = template("t2", "EXPENSE", 15_000_00, LocalDate.now().plusDays(10))
            val recurringRepository = FakeRecurringRepository(listOf(salary, rent))
            val vm = RecurringViewModel(recurringRepository, FakeSuggestionRepository(), NoOpCrashReporter, NoOpPerformanceTracer)
            advanceUntilIdle()

            val loaded = vm.uiState.value as RecurringUiState.Loaded
            assertEquals(50_000_00L, loaded.monthlyInPaise)
            assertEquals(15_000_00L, loaded.monthlyOutPaise)
        }

    @Test
    fun `next 30 days list is ordered by date and excludes entries beyond 30 days`() =
        runTest {
            val soon = template("t1", "EXPENSE", 1_00, LocalDate.now().plusDays(20))
            val sooner = template("t2", "EXPENSE", 1_00, LocalDate.now().plusDays(5))
            val farAway = template("t3", "EXPENSE", 1_00, LocalDate.now().plusDays(45))
            val recurringRepository = FakeRecurringRepository(listOf(soon, sooner, farAway))
            val vm = RecurringViewModel(recurringRepository, FakeSuggestionRepository(), NoOpCrashReporter, NoOpPerformanceTracer)
            advanceUntilIdle()

            val loaded = vm.uiState.value as RecurringUiState.Loaded
            assertEquals(listOf("t2", "t1"), loaded.next30Days.map { it.id })
        }

    @Test
    fun `a paused template is excluded from next 30 days and listed under paused`() =
        runTest {
            val active = template("t1", "EXPENSE", 1_00, LocalDate.now().plusDays(5))
            val paused = template("t2", "EXPENSE", 1_00, LocalDate.now().plusDays(5), paused = true)
            val recurringRepository = FakeRecurringRepository(listOf(active, paused))
            val vm = RecurringViewModel(recurringRepository, FakeSuggestionRepository(), NoOpCrashReporter, NoOpPerformanceTracer)
            advanceUntilIdle()

            val loaded = vm.uiState.value as RecurringUiState.Loaded
            assertEquals(listOf("t1"), loaded.next30Days.map { it.id })
            assertEquals(listOf("t2"), loaded.paused.map { it.id })
        }

    @Test
    fun `pending review count reflects listPending`() =
        runTest {
            val recurringRepository = FakeRecurringRepository()
            val pending =
                listOf(
                    PendingEntry("s1", "t1", LocalDate.now(), mapOf("type" to "EXPENSE"), SuggestionStatus.PENDING),
                    PendingEntry("s2", "t1", LocalDate.now(), mapOf("type" to "EXPENSE"), SuggestionStatus.PENDING),
                )
            val vm =
                RecurringViewModel(
                    recurringRepository,
                    FakeSuggestionRepository(pending),
                    NoOpCrashReporter,
                    NoOpPerformanceTracer,
                )
            advanceUntilIdle()

            val loaded = vm.uiState.value as RecurringUiState.Loaded
            assertEquals(2, loaded.pendingCount)
        }

    @Test
    fun `load materialises due occurrences before rendering`() =
        runTest {
            var materialised = false
            val recurringRepository =
                object : com.dhruv.finance.data.tracker.repo.RecurringRepository by FakeRecurringRepository() {
                    override suspend fun materialiseDue(
                        suggestionRepository: com.dhruv.finance.data.tracker.repo.SuggestionRepository,
                        today: LocalDate,
                    ): Result<Unit> {
                        materialised = true
                        return Result.success(Unit)
                    }
                }
            val vm = RecurringViewModel(recurringRepository, FakeSuggestionRepository(), NoOpCrashReporter, NoOpPerformanceTracer)
            advanceUntilIdle()

            assertTrue(materialised)
        }

    // FR-031b: deleting removes the template from state (via the repository's delete + reload).
    @Test
    fun `delete removes the template and reloads`() =
        runTest {
            val active = template("t1", "EXPENSE", 1_00, LocalDate.now().plusDays(5))
            val recurringRepository = FakeRecurringRepository(listOf(active))
            val vm = RecurringViewModel(recurringRepository, FakeSuggestionRepository(), NoOpCrashReporter, NoOpPerformanceTracer)
            advanceUntilIdle()

            vm.delete("t1")
            advanceUntilIdle()

            assertEquals(listOf("t1"), recurringRepository.deleted)
            val loaded = vm.uiState.value as RecurringUiState.Loaded
            assertTrue(loaded.next30Days.isEmpty())
        }

    // FR-031a: edit reaches the repository with the new fields and reloads.
    @Test
    fun `edit updates the template's amount and schedule`() =
        runTest {
            val active = template("t1", "EXPENSE", 1_00, LocalDate.now().plusDays(5))
            val recurringRepository = FakeRecurringRepository(listOf(active))
            val vm = RecurringViewModel(recurringRepository, FakeSuggestionRepository(), NoOpCrashReporter, NoOpPerformanceTracer)
            advanceUntilIdle()

            vm.edit(
                templateId = "t1",
                type = com.dhruv.finance.data.tracker.model.TransactionType.EXPENSE,
                amountPaise = 9_00,
                accountId = "acc-1",
                categoryId = "cat-1",
                payee = "t1",
                note = null,
                rrule = "FREQ=WEEKLY",
                nextRun = LocalDate.now().plusDays(7),
                amountIsVariable = false,
            )
            advanceUntilIdle()

            val loaded = vm.uiState.value as RecurringUiState.Loaded
            assertEquals(9_00L, loaded.monthlyOutPaise)
        }
}
