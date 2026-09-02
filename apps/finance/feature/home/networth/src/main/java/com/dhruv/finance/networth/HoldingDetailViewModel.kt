package com.dhruv.finance.networth

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.tracker.model.Holding
import com.dhruv.finance.data.tracker.model.ValuationHistoryEntry
import com.dhruv.finance.data.tracker.repo.HoldingRepository
import com.dhruv.finance.data.tracker.repo.ValuationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** C3 — holding detail: current value, trend, INVESTED/GAIN, and full valuation history (NW-UI-002). */
class HoldingDetailViewModel(
    private val holdingRepository: HoldingRepository,
    private val valuationRepository: ValuationRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "networth") {
    /** Matches C3's design copy exactly (3M/6M/1Y/All) — [days] is the trend window; null = All. */
    enum class TrendRange(val days: Long?) {
        THREE_MONTHS(90L),
        SIX_MONTHS(182L),
        ONE_YEAR(365L),
        ALL(null),
    }

    data class UiState(
        val isLoading: Boolean = true,
        val holding: Holding? = null,
        val history: List<ValuationHistoryEntry> = emptyList(),
        val trendRange: TrendRange = TrendRange.ALL,
        val errorMessage: String? = null,
        val isDeleted: Boolean = false,
        val deleteError: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load(holdingId: String) {
        performanceTracer.trace("networth_holding_detail_load") { Unit }
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val holdingResult = holdingRepository.get(holdingId)
            val historyResult = valuationRepository.listHistory(holdingId)
            val failure = holdingResult.exceptionOrNull() ?: historyResult.exceptionOrNull()
            if (failure != null) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = failure.message ?: "Couldn't load this holding.")
                }
                return@launch
            }
            _uiState.update {
                it.copy(isLoading = false, holding = holdingResult.getOrNull(), history = historyResult.getOrNull().orEmpty())
            }
        }
    }

    fun setTrendRange(range: TrendRange) {
        _uiState.update { it.copy(trendRange = range) }
    }

    /** C4's edit path is titled "Add / edit holding" but had no delete counterpart until Phase 9,
     * T051-T053 — a mistakenly-added holding was previously removable only by full-account erasure.
     * Soft-delete only (BR-C1-adjacent: never a hard delete from this screen) — see
     * [HoldingRepository.softDelete]'s own doc for what "recoverable" means in this phase. */
    fun delete(holdingId: String) {
        performanceTracer.trace("networth_holding_delete") { Unit }
        viewModelScope.launch(exceptionHandler) {
            holdingRepository
                .softDelete(holdingId)
                .onSuccess { _uiState.update { it.copy(isDeleted = true, deleteError = null) } }
                .onFailure { e -> _uiState.update { it.copy(deleteError = e.message ?: "Couldn't delete. Try again.") } }
        }
    }

    /** Reverses [delete] within the `UndoSnackbarHost`'s window (DESIGN-SYSTEM §8). */
    fun undoDelete(holdingId: String) {
        viewModelScope.launch(exceptionHandler) {
            holdingRepository
                .restore(holdingId)
                .onSuccess { _uiState.update { it.copy(isDeleted = false, deleteError = null) } }
                .onFailure { e -> _uiState.update { it.copy(deleteError = e.message ?: "Couldn't undo. Try again.") } }
        }
    }

    /** [UiState.history] within [UiState.trendRange], oldest-first — the list itself stays
     * newest-first for display; a chart reads left-to-right. */
    fun trendValuesPaise(state: UiState): List<Long> {
        val cutoffDays = state.trendRange.days
        val filtered =
            if (cutoffDays == null) {
                state.history
            } else {
                val cutoff = LocalDate.now().minusDays(cutoffDays)
                state.history.filter { entry -> LocalDate.parse(entry.valuation.asOf) >= cutoff }
            }
        return filtered.map { it.valuation.valuePaise }.asReversed()
    }
}
