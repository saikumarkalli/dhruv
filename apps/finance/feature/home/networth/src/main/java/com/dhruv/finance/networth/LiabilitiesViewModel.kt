package com.dhruv.finance.networth

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.tracker.auth.ConsentRepository
import com.dhruv.finance.data.tracker.auth.ConsentState
import com.dhruv.finance.data.tracker.auth.SessionState
import com.dhruv.finance.data.tracker.auth.SessionStore
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.model.HoldingWithValue
import com.dhruv.finance.data.tracker.model.LiabilityMeta
import com.dhruv.finance.data.tracker.model.LiabilityType
import com.dhruv.finance.data.tracker.repo.HoldingRepository
import com.dhruv.finance.data.tracker.repo.LiabilityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** One liability row for C6 — a holding + its current outstanding value, merged client-side with
 * its loan/card terms by [HoldingWithValue.holding]'s id (`finance.liabilities_meta` has no
 * server-side join view yet, ADR-0033's context notes none exists). [meta] is null only for a
 * liability whose `liabilities_meta` write failed or was never attempted — modelled as nullable
 * rather than assumed always-present. */
data class LiabilityRow(
    val holdingWithValue: HoldingWithValue,
    val meta: LiabilityMeta?,
)

/** C6 — liabilities overview, grouped by type with totals and a projected debt-free date
 * (spec.md Story 4 Scenario 1). [sessionState]/[consentState] (NW-UI-005, added Phase 9 — this
 * screen previously had no signed-out/consent gating at all, found during the Phase 8 QA pass)
 * mirror [NetWorthOverviewViewModel]'s exact pattern. */
class LiabilitiesViewModel(
    private val holdingRepository: HoldingRepository,
    private val liabilityRepository: LiabilityRepository,
    sessionStore: SessionStore,
    consentRepository: ConsentRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "networth") {
    data class UiState(
        val isLoading: Boolean = true,
        val rows: List<LiabilityRow> = emptyList(),
        val errorMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val sessionState: StateFlow<SessionState> = sessionStore.state
    val consentState: StateFlow<ConsentState> = consentRepository.state

    init {
        load()
    }

    fun load() {
        performanceTracer.trace("networth_liabilities_load") { Unit }
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val holdingsResult = holdingRepository.list(HoldingKind.LIABILITY)
            val holdings = holdingsResult.getOrNull()
            if (holdings == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = holdingsResult.exceptionOrNull()?.message ?: "Couldn't load your liabilities.",
                    )
                }
                return@launch
            }
            val metaByHolding = liabilityRepository.listAll().getOrDefault(emptyList()).associateBy { it.holdingId }
            val rows = holdings.map { hw -> LiabilityRow(hw, metaByHolding[hw.holding.id]) }
            _uiState.update { it.copy(isLoading = false, rows = rows) }
        }
    }

    /** Grouped by type for C6's sections — a row with no [LiabilityMeta] (see [LiabilityRow]'s doc)
     * groups under a null key, rendered as its own "Other" section rather than dropped. */
    fun groupedByType(state: UiState): Map<LiabilityType?, List<LiabilityRow>> = state.rows.groupBy { it.meta?.liabilityType }

    fun totalOutstandingPaise(state: UiState): Long = state.rows.sumOf { it.holdingWithValue.currentValuePaise ?: 0L }

    fun monthlyOutgoPaise(state: UiState): Long = state.rows.sumOf { it.meta?.emiPaise ?: 0L }

    /** The latest (furthest-out) projected payoff date across every liability with enough terms to
     * project — "debt-free" means every liability is paid off, so the binding constraint is
     * whichever one takes longest. Null when nothing is computable (e.g. only credit-card/BNPL
     * rows with no EMI terms). */
    fun debtFreeBy(state: UiState): LocalDate? =
        state.rows
            .mapNotNull { row ->
                val meta = row.meta ?: return@mapNotNull null
                val outstanding = row.holdingWithValue.currentValuePaise ?: return@mapNotNull null
                projectedPayoffMonths(meta, outstanding)
            }.maxOrNull()
            ?.let { months -> LocalDate.now().plusMonths(months.toLong()) }

    /** Payoff progress for one row, as a 0f..1f fraction of [LiabilityMeta.tenureMonths] paid —
     * null when tenure isn't known (a credit card or BNPL line, per `liabilities_meta.sql`'s own
     * comment), never a fabricated progress bar. */
    fun payoffProgress(row: LiabilityRow): Float? =
        row.meta?.let { meta ->
            meta.tenureMonths?.takeIf { it > 0 }?.let { tenure ->
                (meta.paidMonths.toFloat() / tenure).coerceIn(0f, 1f)
            }
        }
}
