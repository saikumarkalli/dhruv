package com.dhruv.finance.money

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.tracker.model.PendingEntry
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
import java.time.LocalDate

/** FR-029: accepting a pending entry writes the transaction and removes it from the list;
 * dismissing writes nothing and also removes it. */
@OptIn(ExperimentalCoroutinesApi::class)
class RecurringReviewViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun entry(id: String) =
        PendingEntry(id, "rec-1", LocalDate.now(), mapOf("type" to "EXPENSE", "amountPaise" to 5_00L), SuggestionStatus.PENDING)

    @Test
    fun `accept calls the repository and removes the entry from the visible list`() =
        runTest {
            val suggestionRepository = FakeSuggestionRepository(listOf(entry("s1"), entry("s2")))
            val vm = RecurringReviewViewModel(suggestionRepository, NoOpCrashReporter, NoOpPerformanceTracer)
            advanceUntilIdle()

            vm.accept(entry("s1"))
            advanceUntilIdle()

            assertEquals(listOf("s1"), suggestionRepository.accepted.map { it.id })
            val loaded = vm.uiState.value as RecurringReviewUiState.Loaded
            assertEquals(listOf("s2"), loaded.pending.map { it.id })
        }

    @Test
    fun `dismiss removes the entry without calling accept`() =
        runTest {
            val suggestionRepository = FakeSuggestionRepository(listOf(entry("s1")))
            val vm = RecurringReviewViewModel(suggestionRepository, NoOpCrashReporter, NoOpPerformanceTracer)
            advanceUntilIdle()

            vm.dismiss(entry("s1"))
            advanceUntilIdle()

            assertEquals(listOf("s1"), suggestionRepository.dismissed)
            assertTrue(suggestionRepository.accepted.isEmpty())
            val loaded = vm.uiState.value as RecurringReviewUiState.Loaded
            assertTrue(loaded.pending.isEmpty())
        }
}
