package com.dhruv.finance.data

import com.dhruv.finance.data.tracker.auth.ConsentRepository
import com.dhruv.finance.data.tracker.auth.ConsentState
import com.dhruv.finance.data.tracker.auth.SessionState
import com.dhruv.finance.data.tracker.auth.SessionStore
import com.dhruv.finance.data.tracker.auth.SessionTokens
import com.dhruv.finance.data.tracker.dto.GoTrueSessionDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/**
 * In-memory [SessionStore] fake for tracker auth tests (Task 2's OnboardingViewModelTest and
 * this task's AuthInterceptorTest both consume it). Mirrors [SessionStoreImpl]'s save/clear/derive
 * shape without any DataStore/encryption involved.
 */
class FakeSessionStore : SessionStore {
    private val _state = MutableStateFlow<SessionState>(SessionState.SignedOut)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    private var tokens: SessionTokens? = null

    override suspend fun save(session: GoTrueSessionDto) {
        tokens = SessionTokens(session.accessToken, session.refreshToken, session.expiresAt)
        _state.value =
            SessionState.Active(
                session.user.id,
                session.user.email,
                session.user.userMetadata?.displayName,
                session.user.userMetadata?.resolvedAvatarUrl,
            )
    }

    override suspend fun clear() {
        tokens = null
        _state.value = SessionState.SignedOut
    }

    override fun currentTokens(): SessionTokens? = tokens
}

/** In-memory [ConsentRepository] fake — Task 2's OnboardingViewModelTest consumes it. */
class FakeConsentRepository : ConsentRepository {
    private val _state = MutableStateFlow(ConsentState())
    override val state: StateFlow<ConsentState> = _state.asStateFlow()

    override suspend fun setSyncFinancialRecords(enabled: Boolean) {
        _state.value = _state.value.copy(syncFinancialRecords = enabled)
    }

    override suspend fun setReadTransactionSms(enabled: Boolean) {
        _state.value = _state.value.copy(readTransactionSms = enabled)
    }

    override suspend fun setAskDhruvAboutMoney(enabled: Boolean) {
        _state.value = _state.value.copy(askDhruvAboutMoney = enabled)
    }

    override suspend fun setHasCompletedOnboarding(completed: Boolean) {
        _state.value = _state.value.copy(hasCompletedOnboarding = completed)
    }
}
