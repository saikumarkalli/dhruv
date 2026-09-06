package com.dhruv.finance.networth

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.tracker.model.AmortisationSplit
import com.dhruv.finance.data.tracker.model.Holding
import com.dhruv.finance.data.tracker.model.LiabilityMeta
import com.dhruv.finance.data.tracker.model.amortisationSplit
import com.dhruv.finance.data.tracker.repo.HoldingRepository
import com.dhruv.finance.data.tracker.repo.LiabilityRepository
import com.dhruv.finance.data.tracker.repo.ValuationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** C7 — liability detail: amortisation split, rate/EMI/tenure/collateral, and a prepay-savings
 * projection (spec.md Story 4 Scenarios 2-3, NW-UI-004, FR-008/FR-009). [meta] being null (see
 * [LiabilityRow]'s doc) is a designed, non-blocking state — the screen still shows the outstanding
 * balance, just without a rate/EMI section or a computable projection. */
class LiabilityDetailViewModel(
    private val holdingRepository: HoldingRepository,
    private val liabilityRepository: LiabilityRepository,
    private val valuationRepository: ValuationRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "networth") {
    data class UiState(
        val isLoading: Boolean = true,
        val holding: Holding? = null,
        val meta: LiabilityMeta? = null,
        val outstandingPaise: Long? = null,
        val errorMessage: String? = null,
        val extraPaymentText: String = "",
        val prepayProjection: PrepayProjection? = null,
        val prepayError: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load(holdingId: String) {
        performanceTracer.trace("networth_liability_detail_load") { Unit }
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val holdingResult = holdingRepository.get(holdingId)
            val holding = holdingResult.getOrNull()
            if (holding == null) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = holdingResult.exceptionOrNull()?.message ?: "Couldn't load this liability.")
                }
                return@launch
            }
            // A missing liabilities_meta row (metaResult failure) is not surfaced as
            // UiState.errorMessage — it is a designed non-blocking state, not a load failure.
            val meta = liabilityRepository.get(holdingId).getOrNull()
            val outstanding =
                valuationRepository
                    .listHistory(holdingId)
                    .getOrNull()
                    ?.firstOrNull()
                    ?.valuation
                    ?.valuePaise
            _uiState.update {
                it.copy(isLoading = false, holding = holding, meta = meta, outstandingPaise = outstanding)
            }
        }
    }

    /** spec.md Story 4 Scenario 2 — null when either the liability has no recorded terms or its
     * amortisation split isn't computable (see [amortisationSplit]'s own doc). */
    fun amortisationSplit(state: UiState): AmortisationSplit? =
        state.meta?.let { meta -> state.outstandingPaise?.let { outstanding -> meta.amortisationSplit(outstanding) } }

    fun onExtraPaymentChange(value: String) {
        _uiState.update { it.copy(extraPaymentText = value, prepayProjection = null, prepayError = null) }
    }

    /** FR-009 — a hypothetical, derived-only projection (never presented as a committed change to
     * the loan itself; `platform/DESIGN-SYSTEM.md` §10's "derived output is labelled as derived"). */
    fun computePrepay() {
        val state = _uiState.value
        val meta = state.meta
        val outstanding = state.outstandingPaise
        if (meta == null || outstanding == null) {
            _uiState.update { it.copy(prepayError = "Add this loan's rate and EMI to project a prepayment.") }
            return
        }
        val extraPaise = parseRupeesToPaise(state.extraPaymentText)
        if (extraPaise == null || extraPaise <= 0L) {
            _uiState.update { it.copy(prepayProjection = null, prepayError = "Enter a valid amount") }
            return
        }
        val projection = computePrepayProjection(meta, outstanding, extraPaise)
        if (projection == null) {
            _uiState.update {
                it.copy(prepayProjection = null, prepayError = "Couldn't project this amount — check it against the outstanding balance.")
            }
        } else {
            _uiState.update { it.copy(prepayProjection = projection, prepayError = null) }
        }
    }
}
