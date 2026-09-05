package com.dhruv.finance.money

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.tracker.auth.SessionState
import com.dhruv.finance.data.tracker.auth.SessionStore
import com.dhruv.finance.data.tracker.model.MonthSummary
import com.dhruv.finance.data.tracker.model.Transaction
import com.dhruv.finance.data.tracker.model.TransactionType
import com.dhruv.finance.data.tracker.repo.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** D1's screen state (spec.md Story 1/2; FR-011/FR-012/FR-032). */
sealed interface LedgerUiState {
    data object Loading : LedgerUiState

    data class Loaded(
        val month: YearMonth,
        val summary: MonthSummary?,
        val dayGroups: List<DayGroup>,
        /** Count before filters/search narrow it — lets the screen say "3 of 24" (FR-014). */
        val totalCount: Int,
    ) : LedgerUiState

    data class Error(val message: String) : LedgerUiState

    /** FR-032: a network-backed surface with no session renders a designed state, never a
     * blank screen or an unresolving spinner. */
    data object SignedOut : LedgerUiState
}

data class DayGroup(
    val date: LocalDate,
    val label: String,
    val netPaise: Long,
    val transactions: List<Transaction>,
)

/** D5's filter combination (FR-014/FR-015) — also the shape a [com.dhruv.finance.data.tracker.repo.SavedViewRepository]
 * entry persists. `categoryIds`/`accountId` are ids, not names, so a rename never invalidates a
 * saved view. */
data class LedgerFilter(
    val type: TransactionType? = null,
    val categoryIds: Set<String> = emptySet(),
    val minPaise: Long? = null,
    val maxPaise: Long? = null,
    val accountId: String? = null,
) {
    val isEmpty: Boolean
        get() = type == null && categoryIds.isEmpty() && minPaise == null && maxPaise == null && accountId == null

    fun matches(transaction: Transaction): Boolean =
        (type == null || transaction.type == type) &&
            (categoryIds.isEmpty() || transaction.categoryId in categoryIds) &&
            (minPaise == null || transaction.amountPaise >= minPaise) &&
            (maxPaise == null || transaction.amountPaise <= maxPaise) &&
            (accountId == null || transaction.accountId == accountId || transaction.toAccountId == accountId)
}

/**
 * D1 (ledger root) — today's transactions plus the pinned month summary, both read server-side
 * (`v_month_summary`, NFR-8: never a client-side sum). Month selection (FR-011), search (FR-013)
 * and filtering (FR-014) narrow the already-fetched month's list client-side — the server has
 * already scoped it to one month, so this is not the "never sum client-side" concern NFR-8 is
 * about (that's specifically about totals, not list narrowing).
 */
class LedgerViewModel(
    private val transactionRepository: TransactionRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
    private val sessionStore: SessionStore? = null,
) : FeatureViewModel(crashReporter, "money") {
    private val _uiState = MutableStateFlow<LedgerUiState>(LedgerUiState.Loading)
    val uiState: StateFlow<LedgerUiState> = _uiState.asStateFlow()

    private var month: YearMonth = YearMonth.now()
    private var summary: MonthSummary? = null
    private val allTransactions = MutableStateFlow<List<Transaction>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(LedgerFilter())
    val filter: StateFlow<LedgerFilter> = _filter.asStateFlow()

    init {
        combine(allTransactions, _searchQuery, _filter) { transactions, query, filter ->
            applyView(transactions, query, filter)
        }.onEach { (dayGroups, totalCount) ->
            if (_uiState.value !is LedgerUiState.Loading && _uiState.value !is LedgerUiState.Error) {
                _uiState.value = LedgerUiState.Loaded(month, summary, dayGroups, totalCount)
            }
        }.launchIn(viewModelScope)

        sessionStore?.state?.onEach { state ->
            if (state is SessionState.SignedOut) {
                _uiState.value = LedgerUiState.SignedOut
            } else if (_uiState.value is LedgerUiState.SignedOut) {
                load(month)
            }
        }?.launchIn(viewModelScope)

        if (sessionStore?.state?.value !is SessionState.SignedOut) {
            load(YearMonth.now())
        } else {
            _uiState.value = LedgerUiState.SignedOut
        }
    }

    fun load(month: YearMonth) {
        this.month = month
        performanceTracer.trace("money_ledger_load") { _uiState.value = LedgerUiState.Loading }
        viewModelScope.launch(exceptionHandler) {
            val transactionsResult = transactionRepository.listForMonth(month)
            val summaryResult = transactionRepository.monthSummary(month)

            transactionsResult
                .onSuccess { transactions ->
                    summary = summaryResult.getOrNull()
                    allTransactions.value = transactions
                    val (dayGroups, totalCount) = applyView(transactions, _searchQuery.value, _filter.value)
                    _uiState.value = LedgerUiState.Loaded(month, summary, dayGroups, totalCount)
                }.onFailure { error ->
                    _uiState.value = LedgerUiState.Error(error.message ?: "Couldn't load the ledger")
                }
        }
    }

    /** Called after a save/edit/delete so the pinned summary and day groups reflect it without a
     * manual refresh (spec.md Story 1, Acceptance Scenario 3). */
    fun refresh() = load(month)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: LedgerFilter) {
        _filter.value = filter
    }

    fun resetFilter() = setFilter(LedgerFilter())

    /** D5's live "Show N results" — the count a candidate filter *would* produce, without
     * committing it (FR-014). */
    fun previewCount(candidate: LedgerFilter): Int = allTransactions.value.count { candidate.matches(it) }

    /** FR-006/DESIGN-SYSTEM §8 — soft-delete + a recoverable location. The ledger itself is that
     * location: [UndoSnackbarHost] the screen shows on success calls [undoDelete] if the user
     * taps Undo, restoring the same row rather than recreating it. */
    fun delete(transactionId: String) {
        viewModelScope.launch(exceptionHandler) {
            transactionRepository.softDeleteTransaction(transactionId).onSuccess { refresh() }
        }
    }

    fun undoDelete(transactionId: String) {
        viewModelScope.launch(exceptionHandler) {
            transactionRepository.restoreTransaction(transactionId).onSuccess { refresh() }
        }
    }

    private fun applyView(
        transactions: List<Transaction>,
        query: String,
        filter: LedgerFilter,
    ): Pair<List<DayGroup>, Int> {
        val filtered =
            transactions.filter { txn ->
                filter.matches(txn) &&
                    (
                        query.isBlank() ||
                            txn.payee?.contains(query, ignoreCase = true) == true ||
                            txn.note?.contains(query, ignoreCase = true) == true
                    )
            }
        return groupByDay(filtered) to filtered.size
    }

    private fun groupByDay(transactions: List<Transaction>): List<DayGroup> {
        val zone = ZoneId.systemDefault()
        return transactions
            .groupBy { it.occurredAt.atZone(zone).toLocalDate() }
            .toSortedMap(compareByDescending { it })
            .map { (date, dayTransactions) ->
                DayGroup(
                    date = date,
                    label = date.format(DateTimeFormatter.ofPattern("EEE, d MMM")),
                    netPaise = dayTransactions.sumOf { it.signedAmountPaise },
                    transactions = dayTransactions,
                )
            }
    }
}
