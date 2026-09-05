package com.dhruv.finance.networth

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.tracker.model.CreateHoldingRequest
import com.dhruv.finance.data.tracker.model.CreateLiabilityMetaRequest
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.model.UpdateHoldingRequest
import com.dhruv.finance.data.tracker.repo.HoldingRepository
import com.dhruv.finance.data.tracker.repo.LiabilityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

private const val BASIS_POINTS_PER_PERCENT = 100.0

/** C4 — add a holding (asset or liability) with its first value. Never a client-side atomic-write
 * simulation: [save] makes exactly one call to [HoldingRepository.createWithFirstValuation]
 * (BR-C2/NW-BR-001).
 *
 * A `LIABILITY`-kind holding additionally collects loan terms (type/rate/EMI/tenure, tasks.md
 * Phase 6 scope addition — the spec's Phase 6 tasks list building C6/C7 but never says where a
 * liability's `liabilities_meta` row is created; this screen's existing "I owe this" path is the
 * natural point). That is a **second** call to [LiabilityRepository.createMeta], deliberately not
 * folded into the same RPC as the holding+valuation write: loan terms are optional metadata on top
 * of the pair BR-C2 already guarantees atomically, not a third leg of that same guarantee. Its
 * failure is surfaced via [UiState.liabilityMetaError] but does not block navigating away — the
 * holding itself is already safely saved by that point. */
class AddEditHoldingViewModel(
    private val holdingRepository: HoldingRepository,
    private val liabilityRepository: LiabilityRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "networth") {
    data class UiState(
        val editingHoldingId: String? = null,
        val isLoadingForEdit: Boolean = false,
        val name: String = "",
        val kind: HoldingKind = HoldingKind.ASSET,
        val sectorCode: String? = null,
        val amountText: String = "",
        val investedAmountText: String = "",
        val notesText: String = "",
        val liabilityTypeCode: String? = null,
        val rateText: String = "",
        val emiText: String = "",
        val tenureMonthsText: String = "",
        val nameError: String? = null,
        val sectorError: String? = null,
        val amountError: String? = null,
        val liabilityTypeError: String? = null,
        val rateError: String? = null,
        val liabilityMetaError: String? = null,
        val isSaving: Boolean = false,
        val savedHoldingId: String? = null,
    ) {
        val isEditing: Boolean get() = editingHoldingId != null
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** C4's edit path (Phase 9, T051/T052) — prefills from the existing holding. Liability terms
     * (rate/EMI/tenure/collateral) are deliberately not editable here: this phase never built an
     * `updateMeta()` UI path, only the repository method (tracked as a known gap, not this task's
     * scope) — editing a liability holding here only changes its name/sector/invested/notes. The
     * "current value" field is create-only (recording a new value is C5's job, not an edit). */
    fun startEditing(holdingId: String) {
        _uiState.update { it.copy(editingHoldingId = holdingId, isLoadingForEdit = true) }
        viewModelScope.launch(exceptionHandler) {
            holdingRepository
                .get(holdingId)
                .onSuccess { holding ->
                    _uiState.update {
                        it.copy(
                            isLoadingForEdit = false,
                            name = holding.name,
                            kind = holding.kind,
                            sectorCode = holding.sector.name,
                            investedAmountText = holding.investedPaise?.let(::formatPaiseAsRupeesText) ?: "",
                            notesText = holding.notes ?: "",
                        )
                    }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoadingForEdit = false, nameError = e.message ?: "Couldn't load this holding.") }
                }
        }
    }

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

    fun onInvestedAmountChange(value: String) {
        _uiState.update { it.copy(investedAmountText = value) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notesText = value) }
    }

    fun onLiabilityTypeChange(code: String) {
        _uiState.update { it.copy(liabilityTypeCode = code, liabilityTypeError = null) }
    }

    fun onRateChange(value: String) {
        _uiState.update { it.copy(rateText = value, rateError = null) }
    }

    fun onEmiChange(value: String) {
        _uiState.update { it.copy(emiText = value) }
    }

    fun onTenureMonthsChange(value: String) {
        _uiState.update { it.copy(tenureMonthsText = value) }
    }

    fun save() {
        val state = _uiState.value
        if (state.isEditing) {
            saveEdit(state)
        } else {
            saveCreate(state)
        }
    }

    /** Edit mode validates only name/sector — the amount/liability fields aren't shown or editable
     * here (see [startEditing]'s own doc). */
    private fun saveEdit(state: UiState) {
        val name = state.name.trim()
        val sectorCode = state.sectorCode
        val nameError = if (name.isEmpty()) "Enter a name" else null
        val sectorError = if (sectorCode == null) "Choose a category" else null
        if (nameError != null || sectorError != null) {
            _uiState.update { it.copy(nameError = nameError, sectorError = sectorError) }
            return
        }

        performanceTracer.trace("networth_edit_holding") { Unit }
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isSaving = true) }
            holdingRepository
                .update(
                    holdingId = state.editingHoldingId!!,
                    request =
                        UpdateHoldingRequest(
                            name = name,
                            sectorCode = sectorCode!!,
                            investedPaise = state.investedAmountText.takeIf { it.isNotBlank() }?.let(::parseRupeesToPaise),
                            notes = state.notesText.trim().ifBlank { null },
                        ),
                ).onSuccess { _uiState.update { it.copy(isSaving = false, savedHoldingId = state.editingHoldingId) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, nameError = e.message ?: "Couldn't save. Try again.") }
                }
        }
    }

    private fun saveCreate(state: UiState) {
        val name = state.name.trim()
        val sectorCode = state.sectorCode
        val paise = parseRupeesToPaise(state.amountText)
        val isLiability = state.kind == HoldingKind.LIABILITY
        val rateBps = if (isLiability) parseRatePercentToBps(state.rateText) else null

        val errors = validate(name, sectorCode, paise, isLiability, state.liabilityTypeCode, rateBps)
        if (!errors.isClean()) {
            _uiState.update {
                it.copy(
                    nameError = errors.nameError,
                    sectorError = errors.sectorError,
                    amountError = errors.amountError,
                    liabilityTypeError = errors.liabilityTypeError,
                    rateError = errors.rateError,
                )
            }
            return
        }

        performanceTracer.trace("networth_add_holding") { Unit }
        viewModelScope.launch(exceptionHandler) {
            submit(state = state, name = name, sectorCode = sectorCode!!, paise = paise!!, isLiability = isLiability, rateBps = rateBps)
        }
    }

    private suspend fun submit(
        state: UiState,
        name: String,
        sectorCode: String,
        paise: Long,
        isLiability: Boolean,
        rateBps: Int?,
    ) {
        _uiState.update { it.copy(isSaving = true) }
        holdingRepository
            .createWithFirstValuation(
                CreateHoldingRequest(
                    name = name,
                    kind = state.kind,
                    sectorCode = sectorCode,
                    valuePaise = paise,
                    asOf = LocalDate.now().toString(),
                    investedPaise = state.investedAmountText.takeIf { it.isNotBlank() }?.let(::parseRupeesToPaise),
                    notes = state.notesText.trim().ifBlank { null },
                    requestId = UUID.randomUUID().toString(),
                ),
            ).onSuccess { id ->
                if (isLiability) {
                    createLiabilityMeta(holdingId = id, state = state, rateBps = rateBps!!, valuePaise = paise)
                } else {
                    _uiState.update { it.copy(isSaving = false, savedHoldingId = id) }
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, amountError = e.message ?: "Couldn't save. Try again.") }
            }
    }

    private suspend fun createLiabilityMeta(
        holdingId: String,
        state: UiState,
        rateBps: Int,
        valuePaise: Long,
    ) {
        val emiPaise = state.emiText.takeIf { it.isNotBlank() }?.let(::parseRupeesToPaise)
        val tenureMonths = state.tenureMonthsText.trim().toIntOrNull()
        liabilityRepository
            .createMeta(
                CreateLiabilityMetaRequest(
                    holdingId = holdingId,
                    liabilityTypeCode = state.liabilityTypeCode!!,
                    rateBps = rateBps,
                    emiPaise = emiPaise,
                    tenureMonths = tenureMonths,
                    // The value just recorded is this liability's starting outstanding balance
                    // (FR-008), and also stands in for its original sanctioned principal — the
                    // common case of adding a loan at (or near) disbursement, paidMonths=0. A
                    // loan added partway through its term will show a slightly optimistic payoff
                    // projection until its terms are edited; there is no edit-liability screen in
                    // this phase (tracked as a follow-up, not this phase's scope).
                    originalPrincipalPaise = valuePaise,
                    requestId = UUID.randomUUID().toString(),
                ),
            ).onSuccess { _uiState.update { it.copy(isSaving = false, savedHoldingId = holdingId) } }
            .onFailure { e ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        savedHoldingId = holdingId,
                        liabilityMetaError = e.message ?: "Holding saved, but its loan details couldn't be saved.",
                    )
                }
            }
    }
}

private data class ValidationErrors(
    val nameError: String? = null,
    val sectorError: String? = null,
    val amountError: String? = null,
    val liabilityTypeError: String? = null,
    val rateError: String? = null,
) {
    /** A list-based check rather than a chain of `||` — keeps this a plain method call at the call
     * site instead of a five-term boolean condition. */
    fun isClean(): Boolean = listOfNotNull(nameError, sectorError, amountError, liabilityTypeError, rateError).isEmpty()
}

private fun validate(
    name: String,
    sectorCode: String?,
    paise: Long?,
    isLiability: Boolean,
    liabilityTypeCode: String?,
    rateBps: Int?,
): ValidationErrors =
    ValidationErrors(
        nameError = if (name.isEmpty()) "Enter a name" else null,
        sectorError = if (sectorCode == null) "Choose a category" else null,
        amountError = if (paise == null || paise <= 0L) "Enter a valid amount" else null,
        liabilityTypeError = if (isLiability && liabilityTypeCode == null) "Choose a liability type" else null,
        rateError = if (isLiability && rateBps == null) "Enter a valid rate" else null,
    )

/** Parses a user-typed percent (e.g. "8.5") into basis points. Returns null for anything outside
 * 0..100 — the caller renders that as a validation error. */
internal fun parseRatePercentToBps(text: String): Int? =
    text
        .trim()
        .toDoubleOrNull()
        ?.takeIf { it in 0.0..100.0 }
        ?.let { Math.round(it * BASIS_POINTS_PER_PERCENT).toInt() }

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

/** Inverse of [parseRupeesToPaise], for prefilling an edit form from a stored paise value — never
 * shows trailing ".00" for a whole-rupee amount, since the user almost certainly typed a whole
 * number originally. */
internal fun formatPaiseAsRupeesText(paise: Long): String {
    val rupees = paise / 100.0
    return if (paise % 100 == 0L) rupees.toLong().toString() else "%.2f".format(java.util.Locale.US, rupees)
}
