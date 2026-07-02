package com.dhruv.finance.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Pure-JVM in-memory fakes for the data-layer DAOs, so repository logic can be tested without
 * Robolectric's native SQLite (which does not load on every host — see ADR-0013 / regression plan).
 * Each fake replicates the DAO's query semantics (soft-delete filtering, ordering, REPLACE-on-
 * conflict). The SQL itself is verified by developer-local instrumented tests, not the JVM gate.
 */

class FakeHistoryDao : HistoryDao {
    private val rows = MutableStateFlow<List<HistoryEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllHistory(): Flow<List<HistoryEntity>> = rows.map { list -> list.sortedByDescending { it.timestamp } }

    override fun getActiveHistory(): Flow<List<HistoryEntity>> =
        rows.map { list -> list.filter { !it.isInRecycleBin }.sortedByDescending { it.timestamp } }

    override fun getRecycleBinHistory(): Flow<List<HistoryEntity>> =
        rows.map { list -> list.filter { it.isInRecycleBin }.sortedByDescending { it.deletedTimestamp } }

    override suspend fun insertHistory(history: HistoryEntity) {
        val row = if (history.id == 0L) history.copy(id = nextId++) else history
        rows.value = rows.value.filterNot { it.id == row.id } + row
    }

    override suspend fun updateHistory(history: HistoryEntity) {
        rows.value = rows.value.map { if (it.id == history.id) history else it }
    }

    override suspend fun deleteHistoryById(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun deleteMultipleHistoryByIds(ids: List<Long>) {
        rows.value = rows.value.filterNot { it.id in ids }
    }

    override suspend fun moveToRecycleBin(
        id: Long,
        deletedTime: Long,
    ) {
        rows.value =
            rows.value.map {
                if (it.id == id) it.copy(isInRecycleBin = true, deletedTimestamp = deletedTime) else it
            }
    }

    override suspend fun moveMultipleToRecycleBin(
        ids: List<Long>,
        deletedTime: Long,
    ) {
        rows.value =
            rows.value.map {
                if (it.id in ids) it.copy(isInRecycleBin = true, deletedTimestamp = deletedTime) else it
            }
    }

    override suspend fun restoreFromRecycleBin(id: Long) {
        rows.value = rows.value.map { if (it.id == id) it.copy(isInRecycleBin = false) else it }
    }

    override suspend fun emptyRecycleBin() {
        rows.value = rows.value.filterNot { it.isInRecycleBin }
    }

    override suspend fun autoRemoveRecycleBinOlderThan(beforeTime: Long) {
        rows.value = rows.value.filterNot { it.isInRecycleBin && it.deletedTimestamp < beforeTime }
    }

    override suspend fun clearActiveHistory() {
        rows.value = rows.value.filter { it.isInRecycleBin }
    }

    override suspend fun clearAllHistory() {
        rows.value = emptyList()
    }
}

class FakeCurrencyRateDao : CurrencyRateDao {
    private val store = LinkedHashMap<String, CurrencyRateEntity>()

    override suspend fun getAllRates(): List<CurrencyRateEntity> = store.values.toList()

    override suspend fun insertRates(rates: List<CurrencyRateEntity>) {
        rates.forEach { store[it.currencyCode] = it } // REPLACE-on-conflict keyed by code
    }

    override suspend fun getRateByCode(code: String): CurrencyRateEntity? = store[code]

    override suspend fun clearAllRates() = store.clear()
}
