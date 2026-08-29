package com.dhruv.finance.app.ui.settings

import com.dhruv.finance.data.HistoryDao
import com.dhruv.finance.data.HistoryEntity
import kotlinx.coroutines.flow.MutableStateFlow

/** Minimal [HistoryDao] test double — no Room, matching this project's fakes-not-mocks convention. */
class FakeHistoryDao : HistoryDao {
    private val all = MutableStateFlow<List<HistoryEntity>>(emptyList())

    override fun getActiveHistory() = all
    override fun getRecycleBinHistory() = all
    override fun getAllHistory() = all

    override suspend fun insertHistory(history: HistoryEntity) {
        all.value = all.value + history
    }

    override suspend fun updateHistory(history: HistoryEntity) = Unit

    override suspend fun deleteHistoryById(id: Long) {
        all.value = all.value.filterNot { it.id == id }
    }

    override suspend fun deleteMultipleHistoryByIds(ids: List<Long>) {
        all.value = all.value.filterNot { it.id in ids }
    }

    override suspend fun moveToRecycleBin(
        id: Long,
        deletedTime: Long,
    ) = Unit

    override suspend fun moveMultipleToRecycleBin(
        ids: List<Long>,
        deletedTime: Long,
    ) = Unit

    override suspend fun restoreFromRecycleBin(id: Long) = Unit

    override suspend fun emptyRecycleBin() = Unit

    override suspend fun autoRemoveRecycleBinOlderThan(beforeTime: Long) = Unit

    override suspend fun clearActiveHistory() {
        all.value = emptyList()
    }

    override suspend fun clearAllHistory() {
        all.value = emptyList()
    }
}
