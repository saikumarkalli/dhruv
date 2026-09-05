package com.dhruv.finance.money

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.tracker.model.MonthSummary
import com.dhruv.finance.data.tracker.model.Transaction
import com.dhruv.finance.data.tracker.repo.TransactionRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    ) : LedgerUiState

    data class Error(val message: String) : LedgerUiState
}

data class DayGroup(
    val date: LocalDate,
    val label: String,
    val netPaise: Long,
    val transactions: List<Transaction>,
)

/**
 * D1 (ledger root) — today's transactions plus the pinned month summary, both read server-side
 * (`v_month_summary`, NFR-8: never a client-side sum). Story 1 (US1) covers the current month
 * only; month selection/paging is US2 (T033).
 */
class LedgerViewModel(
    private val transactionRepository: TransactionRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "money") {
    private val _uiState = MutableStateFlow<LedgerUiState>(LedgerUiState.Loading)
    val uiState: StateFlow<LedgerUiState> = _uiState.asStateFlow()

    init {
        load(YearMonth.now())
    }

    fun load(month: YearMonth) {
        performanceTracer.trace("money_ledger_load") { _uiState.value = LedgerUiState.Loading }
        viewModelScope.launch(exceptionHandler) {
            val transactionsResult = transactionRepository.listForMonth(month)
            val summaryResult = transactionRepository.monthSummary(month)

            transactionsResult
                .onSuccess { transactions ->
                    val summary = summaryResult.getOrNull()
                    _uiState.value = LedgerUiState.Loaded(month, summary, groupByDay(transactions))
                }.onFailure { error ->
                    _uiState.value = LedgerUiState.Error(error.message ?: "Couldn't load the ledger")
                }
        }
    }

    /** Called after a save/edit/delete so the pinned summary and day groups reflect it without a
     * manual refresh (spec.md Story 1, Acceptance Scenario 3). */
    fun refresh() {
        val current = _uiState.value
        load(if (current is LedgerUiState.Loaded) current.month else YearMonth.now())
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
