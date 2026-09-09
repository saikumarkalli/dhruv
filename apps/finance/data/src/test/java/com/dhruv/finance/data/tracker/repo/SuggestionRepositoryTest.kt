package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.SuggestionStatusDto
import com.dhruv.finance.data.tracker.model.PendingEntry
import com.dhruv.finance.data.tracker.model.SuggestionStatus
import com.dhruv.finance.data.tracker.model.Transaction
import com.dhruv.finance.data.tracker.model.TransactionSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A fake [TransactionRepository] recording every [createTransaction] call, for asserting
 * accept/dismiss's downstream effect without a real HTTP boundary. */
private class SuggestionRecordingTransactionRepository : TransactionRepository {
    val created = mutableListOf<Transaction>()

    override suspend fun listForMonth(month: java.time.YearMonth) = error("not stubbed for this test")

    override suspend fun monthSummary(month: java.time.YearMonth) = error("not stubbed for this test")

    override suspend fun createTransaction(
        transaction: Transaction,
        requestId: String,
    ): Result<Transaction> {
        val saved = transaction.copy(id = "txn-generated")
        created += saved
        return Result.success(saved)
    }

    override suspend fun updateTransaction(transaction: Transaction) = error("not stubbed for this test")

    override suspend fun softDeleteTransaction(transactionId: String) = error("not stubbed for this test")

    override suspend fun restoreTransaction(transactionId: String) = error("not stubbed for this test")

    override suspend fun getTransaction(transactionId: String) = error("not stubbed for this test")

    override suspend fun listEvents(transactionId: String) = error("not stubbed for this test")

    override suspend fun guessFor(payee: String?) = error("not stubbed for this test")
}

private fun pendingEntry(
    id: String = "sug-1",
    recurringId: String? = "rec-1",
) = PendingEntry(
    id = id,
    recurringId = recurringId,
    dueOn = java.time.LocalDate.of(2026, 9, 1),
    parsed =
        mapOf(
            RecurringTemplateKeys.TYPE to "EXPENSE",
            RecurringTemplateKeys.AMOUNT_PAISE to 5_00L,
            RecurringTemplateKeys.ACCOUNT_ID to "acc-1",
            RecurringTemplateKeys.CATEGORY_ID to "cat-1",
            RecurringTemplateKeys.PAYEE to "Landlord",
        ),
    status = SuggestionStatus.PENDING,
)

class SuggestionRepositoryTest {
    // FR-029: accepting writes the transaction (source = RECURRING, recurring_id set — the DB
    // trigger emits ACCEPTED_FROM_RECURRING for it) and marks the suggestion ACCEPTED.
    @Test
    fun `accept writes a transaction sourced from the parsed suggestion and marks it ACCEPTED`() =
        runTest {
            var statusSet: SuggestionStatusDto? = null
            val api =
                object : com.dhruv.finance.data.tracker.net.MoneyApi by RecurringUnimplementedMoneyApi {
                    override suspend fun setSuggestionStatus(
                        id: String,
                        body: SuggestionStatusDto,
                    ): List<com.dhruv.finance.data.tracker.dto.SuggestionDto> {
                        statusSet = body
                        return emptyList()
                    }
                }
            val transactionRepository = SuggestionRecordingTransactionRepository()
            val repo: SuggestionRepository = SuggestionRepositoryImpl(api, transactionRepository)

            val result = repo.accept(pendingEntry())

            assertTrue(result.isSuccess)
            assertEquals(1, transactionRepository.created.size)
            assertEquals(TransactionSource.RECURRING, transactionRepository.created.single().source)
            assertEquals("rec-1", transactionRepository.created.single().recurringId)
            assertEquals(5_00L, transactionRepository.created.single().amountPaise)
            assertEquals("ACCEPTED", statusSet?.status)
        }

    // FR-029: dismissing writes nothing to transactions, only marks the suggestion IGNORED.
    @Test
    fun `dismiss writes no transaction and marks the suggestion IGNORED`() =
        runTest {
            var statusSet: SuggestionStatusDto? = null
            val api =
                object : com.dhruv.finance.data.tracker.net.MoneyApi by RecurringUnimplementedMoneyApi {
                    override suspend fun setSuggestionStatus(
                        id: String,
                        body: SuggestionStatusDto,
                    ): List<com.dhruv.finance.data.tracker.dto.SuggestionDto> {
                        statusSet = body
                        return emptyList()
                    }
                }
            val transactionRepository = SuggestionRecordingTransactionRepository()
            val repo: SuggestionRepository = SuggestionRepositoryImpl(api, transactionRepository)

            val result = repo.dismiss("sug-1")

            assertTrue(result.isSuccess)
            assertTrue(transactionRepository.created.isEmpty())
            assertEquals("IGNORED", statusSet?.status)
            assertNull(transactionRepository.created.firstOrNull())
        }
}
