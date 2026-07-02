package com.dhruv.finance.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for [HistoryRepository] over a pure-JVM [FakeHistoryDao] (portable, no native
 * SQLite). Covers the recycle-bin retention window in [HistoryRepository.pruneOldRecycleBin] — the
 * one non-pass-through behaviour — plus the soft-delete lifecycle the DPDP rules rely on
 * (PLATFORM.md §5, §8).
 */
class HistoryRepositoryTest {
    private val retentionMillis = 30L * 24 * 60 * 60 * 1000 // 30 days
    private val repo = HistoryRepository(FakeHistoryDao(), recycleBinRetentionMillis = retentionMillis)

    private fun binned(expr: String, deletedAt: Long) =
        HistoryEntity(expression = expr, result = "=", isInRecycleBin = true, deletedTimestamp = deletedAt)

    @Test
    fun pruneRemovesOnlyItemsOlderThanRetentionWindow() = runBlocking {
        val now = System.currentTimeMillis()
        repo.insert(binned("stale", deletedAt = now - retentionMillis - 1000)) // just past window
        repo.insert(binned("fresh", deletedAt = now)) // well within window

        repo.pruneOldRecycleBin()

        assertEquals(listOf("fresh"), repo.recycleBinHistory.first().map { it.expression })
    }

    @Test
    fun insertThenMoveToRecycleBinSoftDeletes() = runBlocking {
        repo.insert(HistoryEntity(expression = "2+2", result = "4"))
        val id = repo.allHistory.first().first().id
        repo.moveToRecycleBin(id, deletedTime = 123L)

        assertTrue(repo.activeHistory.first().isEmpty())
        val binned = repo.recycleBinHistory.first()
        assertEquals(1, binned.size)
        assertEquals(123L, binned[0].deletedTimestamp)
        // soft, not hard — still present overall
        assertEquals(1, repo.allHistory.first().size)
    }

    @Test
    fun restoreReactivatesABinnedRow() = runBlocking {
        repo.insert(HistoryEntity(expression = "x", result = "="))
        val id = repo.allHistory.first().first().id
        repo.moveToRecycleBin(id, deletedTime = 1L)
        repo.restoreFromRecycleBin(id)

        assertEquals(1, repo.activeHistory.first().size)
        assertTrue(repo.recycleBinHistory.first().isEmpty())
    }

    @Test
    fun emptyRecycleBinClearsBinnedRowsOnly() = runBlocking {
        repo.insert(HistoryEntity(expression = "active", result = "="))
        repo.insert(binned("trash", deletedAt = 1))
        repo.emptyRecycleBin()

        assertEquals(1, repo.activeHistory.first().size)
        assertTrue(repo.recycleBinHistory.first().isEmpty())
    }
}
