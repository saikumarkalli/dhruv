package com.dhruv.finance.networth

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.tracker.model.CreateHoldingRequest
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.repo.HoldingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/** C4 — add a holding (asset or liability) with its first value. Never a client-side atomic-write
 * simulation: [save] makes exactly one call to
 * [HoldingRepository.createWithFirstValuation] (BR-C2/NW-BR-001). */
class AddEditHoldingViewModel(
    private val holdingRepository: HoldingRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "networth") {
    data class UiState(
        val name: String = "",
        val kind: HoldingKind = HoldingKind.ASSET,
        val sectorCode: String? = null,
        val amountText: String = "",
        val nameError: String? = null,
        val sectorError: String? = null,
        val amountError: String? = null,
        val isSaving: Boolean = false,
        val savedHoldingId: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null) }
    }

    fun onKindChange(value: HoldingKind) {
        _uiState.update { it.copy(kind = value) }
    }

    fun onSectorChange(code: String) {
        _uiState.update { it.copy(sectorCode = code, sectorError = null) }
    }

    fun onAmountChange(value: String) {
        _uiState.update { it.copy(amountText = value, amountError = null) }
    }

    fun save() {
        val state = _uiState.value
        val name = state.name.trim()
        val sectorCode = state.sectorCode
        val paise = parseRupeesToPaise(state.amountText)

        val nameError = if (name.isEmpty()) "Enter a name" else null
        val sectorError = if (sectorCode == null) "Choose a category" else null
        val amountError = if (paise == null || paise <= 0L) "Enter a valid amount" else null
        if (nameError != null || sectorError != null || amountError != null) {
            _uiState.update { it.copy(nameError = nameError, sectorError = sectorError, amountError = amountError) }
            return
        }

        performanceTracer.trace("networth_add_holding") { Unit }
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isSaving = true) }
            holdingRepository
                .createWithFirstValuation(
                    CreateHoldingRequest(
                        name = name,
                        kind = state.kind,
                        sectorCode = sectorCode!!,
                        valuePaise = paise!!,
                        asOf = LocalDate.now().toString(),
                        requestId = UUID.randomUUID().toString(),
                    ),
                )
                .onSuccess { id -> _uiState.update { it.copy(isSaving = false, savedHoldingId = id) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, amountError = e.message ?: "Couldn't save. Try again.") }
                }
        }
    }
}

/** Parses a user-typed rupee amount (e.g. "50,000" or "50000.50") into paise. Returns null for
 * anything that isn't a valid non-negative number — the caller renders that as a validation
 * error, never a silent zero. */
internal fun parseRupeesToPaise(text: String): Long? =
    text
        .trim()
        .replace(",", "")
        .toDoubleOrNull()
        ?.takeIf { it >= 0 }
        ?.let { Math.round(it * 100) }
