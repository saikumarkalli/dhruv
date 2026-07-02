package com.dhruv.finance.data

import kotlinx.coroutines.flow.Flow

class HistoryRepository(
    private val historyDao: HistoryDao,
    private val recycleBinRetentionMillis: Long = 30 * 24 * 60 * 60 * 1000L,
) {
    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()
    val activeHistory: Flow<List<HistoryEntity>> = historyDao.getActiveHistory()
    val recycleBinHistory: Flow<List<HistoryEntity>> = historyDao.getRecycleBinHistory()

    suspend fun insert(history: HistoryEntity) {
        historyDao.insertHistory(history)
    }

    suspend fun update(history: HistoryEntity) {
        historyDao.updateHistory(history)
    }

    suspend fun delete(id: Long) {
        historyDao.deleteHistoryById(id)
    }

    suspend fun deleteMultiple(ids: List<Long>) {
        historyDao.deleteMultipleHistoryByIds(ids)
    }

    suspend fun moveToRecycleBin(
        id: Long,
        deletedTime: Long = System.currentTimeMillis(),
    ) {
        historyDao.moveToRecycleBin(id, deletedTime)
    }

    suspend fun moveMultipleToRecycleBin(
        ids: List<Long>,
        deletedTime: Long = System.currentTimeMillis(),
    ) {
        historyDao.moveMultipleToRecycleBin(ids, deletedTime)
    }

    suspend fun restoreFromRecycleBin(id: Long) {
        historyDao.restoreFromRecycleBin(id)
    }

    suspend fun emptyRecycleBin() {
        historyDao.emptyRecycleBin()
    }

    suspend fun pruneOldRecycleBin() {
        val beforeTime = System.currentTimeMillis() - recycleBinRetentionMillis
        historyDao.autoRemoveRecycleBinOlderThan(beforeTime)
    }

    suspend fun clearActive() {
        historyDao.clearActiveHistory()
    }

    suspend fun clear() {
        historyDao.clearAllHistory()
    }
}
