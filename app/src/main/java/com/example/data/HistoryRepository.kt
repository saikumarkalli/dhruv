package com.example.data

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    suspend fun insert(history: HistoryEntity) {
        historyDao.insertHistory(history)
    }

    suspend fun delete(id: Long) {
        historyDao.deleteHistoryById(id)
    }

    suspend fun clear() {
        historyDao.clearAllHistory()
    }
}
