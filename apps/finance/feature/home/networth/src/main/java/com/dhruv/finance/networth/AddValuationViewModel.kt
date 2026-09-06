package com.dhruv.finance.networth

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.tracker.model.ValuationSource
import com.dhruv.finance.data.tracker.repo.ValuationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

private const val BASIS_POINTS_PER_UNIT = 10_000L

/** C5 — add or correct a value (spec.md Story 3). [UiState.correctingValuationId] switches
 * [save] between the two distinct write paths [ValuationRepository.recordValue] (plain append)
 * and [ValuationRepository.correctValue] (the only path that amends an existing row,
 * NW-BR-002/NW-BR-003) — never a client-side choice between an insert and an update, since no
 * update path exists at all. */
class AddValuationViewModel(
    private val valuationRepository: ValuationRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "networth") {
    data class UiState(
        val holdingId: String = "",
        val correctingValuationId: String? = null,
        val lastValuePaise: Long? = null,
        val amountText: String = "",
        val sourceCode: String = ValuationSource.MANUAL.name,
        val amountError: String? = null,
        val isSaving: Boolean = false,
        val savedValuationId: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Called once when the sheet opens — [correctingValuationId] non-null means "amend this
     * specific row", in which case [lastValuePaise] is that row's own value (the sheet shows it
     * as the "before" figure being corrected), not necessarily the holding's overall latest. */
    fun start(
        holdingId: String,
        lastValuePaise: Long?,
        correctingValuationId: String? = null,
    ) {
        _uiState.update {
            it.copy(holdingId = holdingId, lastValuePaise = lastValuePaise, correctingValuationId = correctingValuationId)
        }
    }

    fun onAmountChange(text: String) {
        _uiState.update { it.copy(amountText = text, amountError = null) }
    }

    fun onSourceChange(code: String) {
        _uiState.update { it.copy(sourceCode = code) }
    }

    /** Live (deltaPaise, deltaPercentBps) vs [UiState.lastValuePaise), computed from whatever is
     * currently typed — before submit (NW-UI-003). Null while the amount doesn't parse yet, or
     * there is nothing to compare against. */
    fun previewDelta(state: UiState): Pair<Long, Int>? {
        val newPaise = parseRupeesToPaise(state.amountText)
        val lastPaise = state.lastValuePaise
        if (newPaise == null || lastPaise == null || lastPaise == 0L) return null
        val deltaPaise = newPaise - lastPaise
        val deltaBps = (deltaPaise * BASIS_POINTS_PER_UNIT / lastPaise).toInt()
        return deltaPaise to deltaBps
    }

    fun save() {
        val state = _uiState.value
        val paise = parseRupeesToPaise(state.amountText)
        if (paise == null || paise < 0L) {
            _uiState.update { it.copy(amountError = "Enter a valid amount") }
            return
        }

        performanceTracer.trace("networth_add_valuation") { Unit }
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isSaving = true) }
            val today = LocalDate.now().toString()
            val correctingId = state.correctingValuationId
            val result =
                if (correctingId != null) {
                    valuationRepository.correctValue(valuationId = correctingId, valuePaise = paise, asOf = today)
                } else {
                    valuationRepository.recordValue(
                        holdingId = state.holdingId,
                        valuePaise = paise,
                        asOf = today,
                        sourceCode = state.sourceCode,
                        requestId = UUID.randomUUID().toString(),
                    )
                }
            result
                .onSuccess { id -> _uiState.update { it.copy(isSaving = false, savedValuationId = id) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, amountError = e.message ?: "Couldn't save. Try again.") } }
        }
    }
}
