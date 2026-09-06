package com.dhruv.finance.money

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.tracker.model.PendingEntry
import com.dhruv.finance.data.tracker.model.RecurringTemplate
import com.dhruv.finance.data.tracker.repo.RecurringRepository
import com.dhruv.finance.data.tracker.repo.RecurringTemplateKeys
import com.dhruv.finance.data.tracker.repo.SuggestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** D9's screen state (spec.md Story 6; FR-030/FR-032). */
sealed interface RecurringUiState {
    data object Loading : RecurringUiState

    data class Loaded(
        val pendingCount: Int,
        val monthlyInPaise: Long,
        val monthlyOutPaise: Long,
        val next30Days: List<RecurringTemplate>,
        val paused: List<RecurringTemplate>,
    ) : RecurringUiState

    data class Error(
        val message: String,
    ) : RecurringUiState
}

/**
 * D9 (recurring) — review banner, MONTHLY IN/OUT, NEXT 30 DAYS, PAUSED (FR-030). Materialises due
 * occurrences into pending review entries on open (research R7) before rendering, so the review
 * banner's count is always current.
 */
class RecurringViewModel(
    private val recurringRepository: RecurringRepository,
    private val suggestionRepository: SuggestionRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "money") {
    private val _uiState = MutableStateFlow<RecurringUiState>(RecurringUiState.Loading)
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        performanceTracer.trace("money_recurring_load") { _uiState.value = RecurringUiState.Loading }
        viewModelScope.launch(exceptionHandler) {
            recurringRepository.materialiseDue(suggestionRepository)

            val templatesResult = recurringRepository.listActive()
            val pendingResult = suggestionRepository.listPending()

            templatesResult
                .onSuccess { templates ->
                    val pending = pendingResult.getOrDefault(emptyList())
                    val active = templates.filter { !it.paused }
                    val within30 = LocalDate.now().plusDays(30)
                    _uiState.value =
                        RecurringUiState.Loaded(
                            pendingCount = pending.size,
                            monthlyInPaise = active.filter { it.isIncome() }.sumOf { it.amountPaise() },
                            monthlyOutPaise = active.filter { !it.isIncome() }.sumOf { it.amountPaise() },
                            next30Days = active.filter { !it.nextRun.isAfter(within30) }.sortedBy { it.nextRun },
                            paused = templates.filter { it.paused },
                        )
                }.onFailure { error ->
                    _uiState.value = RecurringUiState.Error(error.message ?: "Couldn't load recurring entries")
                }
        }
    }

    fun pause(templateId: String) {
        viewModelScope.launch(exceptionHandler) {
            recurringRepository.pause(templateId).onSuccess { load() }
        }
    }

    fun resume(templateId: String) {
        viewModelScope.launch(exceptionHandler) {
            recurringRepository.resume(templateId).onSuccess { load() }
        }
    }

    /** FR-031a. No screen currently offers an edit form (account/category pickers this would need
     * are not loaded by this ViewModel today) — exposed here, tested, and ready for whichever
     * polish pass builds the sheet, the same "repository/VM done, screen not" pattern already
     * recorded for the saved-view feature (data-model.md). */
    fun edit(
        templateId: String,
        type: com.dhruv.finance.data.tracker.model.TransactionType,
        amountPaise: Long,
        accountId: String,
        categoryId: String?,
        payee: String?,
        note: String?,
        rrule: String,
        nextRun: LocalDate,
        amountIsVariable: Boolean,
    ) {
        viewModelScope.launch(exceptionHandler) {
            recurringRepository
                .edit(templateId, type, amountPaise, accountId, categoryId, payee, note, rrule, nextRun, amountIsVariable)
                .onSuccess { load() }
        }
    }

    /** FR-031b — deleting withdraws every pending entry this template produced too
     * (`RecurringRepository.delete`), so [load] never shows a stale actionable row for it. */
    fun delete(templateId: String) {
        viewModelScope.launch(exceptionHandler) {
            recurringRepository.delete(templateId).onSuccess { load() }
        }
    }
}

private fun RecurringTemplate.isIncome(): Boolean = template[RecurringTemplateKeys.TYPE] == "INCOME"

private fun RecurringTemplate.amountPaise(): Long = (template[RecurringTemplateKeys.AMOUNT_PAISE] as? Number)?.toLong() ?: 0L

/** D9-review's screen state (spec.md Story 6; FR-029/FR-032). */
sealed interface RecurringReviewUiState {
    data object Loading : RecurringReviewUiState

    data class Loaded(
        val pending: List<PendingEntry>,
    ) : RecurringReviewUiState

    data class Error(
        val message: String,
    ) : RecurringReviewUiState
}

/** D9-review — accept writes the transaction (FR-029), dismiss writes nothing. */
class RecurringReviewViewModel(
    private val suggestionRepository: SuggestionRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "money") {
    private val _uiState = MutableStateFlow<RecurringReviewUiState>(RecurringReviewUiState.Loading)
    val uiState: StateFlow<RecurringReviewUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        performanceTracer.trace("money_recurring_review_load") { _uiState.value = RecurringReviewUiState.Loading }
        viewModelScope.launch(exceptionHandler) {
            suggestionRepository
                .listPending()
                .onSuccess { pending -> _uiState.value = RecurringReviewUiState.Loaded(pending) }
                .onFailure { error ->
                    _uiState.value = RecurringReviewUiState.Error(error.message ?: "Couldn't load pending entries")
                }
        }
    }

    fun accept(entry: PendingEntry) {
        viewModelScope.launch(exceptionHandler) {
            suggestionRepository.accept(entry).onSuccess { load() }
        }
    }

    fun dismiss(entry: PendingEntry) {
        viewModelScope.launch(exceptionHandler) {
            suggestionRepository.dismiss(entry.id).onSuccess { load() }
        }
    }
}
