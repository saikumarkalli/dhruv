package com.dhruv.finance.money

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.tracker.model.Transaction
import com.dhruv.finance.data.tracker.model.TransactionSource
import com.dhruv.finance.data.tracker.model.TransactionType
import com.dhruv.finance.data.tracker.repo.AccountRepository
import com.dhruv.finance.data.tracker.repo.CategoryRepository
import com.dhruv.finance.data.tracker.repo.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

/** D2's editable state — pre-guessed on open (FR-002/T030), both guesses stay editable before
 * save, and everything here carries over verbatim into D3 via "more options" (Acceptance
 * Scenario 4). */
data class QuickAddUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountPaise: Long = 0,
    val accountId: String? = null,
    val categoryId: String? = null,
    val note: String? = null,
    val isSaving: Boolean = false,
    val savedTransactionId: String? = null,
    val accountOptions: List<com.dhruv.core.ui.components.SelectionOption> = emptyList(),
    val categoryOptions: List<com.dhruv.core.ui.components.SelectionOption> = emptyList(),
)

/**
 * D2 (quick add) — amount-first entry that reaches a saved transaction in three taps after the
 * amount (FR-002). Pre-guesses category/account from [TransactionRepository.guessFor] on open;
 * a guess failure never blocks entry (spec.md Story 1) — the fields simply start empty.
 */
class QuickAddViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "money") {
    private val _uiState = MutableStateFlow(QuickAddUiState())
    val uiState: StateFlow<QuickAddUiState> = _uiState.asStateFlow()

    fun open(payee: String? = null) {
        viewModelScope.launch(exceptionHandler) {
            transactionRepository.guessFor(payee).onSuccess { guess ->
                _uiState.value = _uiState.value.copy(accountId = guess.accountId, categoryId = guess.categoryId)
            }
            accountRepository.listAccounts().onSuccess { accounts ->
                _uiState.value =
                    _uiState.value.copy(
                        accountOptions = accounts.map { com.dhruv.core.ui.components.SelectionOption(it.id, it.name) },
                    )
            }
            categoryRepository.listCategories().onSuccess { categories ->
                _uiState.value =
                    _uiState.value.copy(
                        categoryOptions = categories.map { com.dhruv.core.ui.components.SelectionOption(it.id, it.name) },
                    )
            }
        }
    }

    fun setType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(type = type)
    }

    fun setAmount(paise: Long) {
        _uiState.value = _uiState.value.copy(amountPaise = paise)
    }

    fun setAccount(accountId: String) {
        _uiState.value = _uiState.value.copy(accountId = accountId)
    }

    fun setCategory(categoryId: String) {
        _uiState.value = _uiState.value.copy(categoryId = categoryId)
    }

    fun setNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note.ifBlank { null })
    }

    fun save() {
        val state = _uiState.value
        if (state.amountPaise <= 0 || state.accountId == null) return
        if (state.type != TransactionType.TRANSFER && state.categoryId == null) return

        performanceTracer.trace("money_quick_add_save") { _uiState.value = state.copy(isSaving = true) }
        viewModelScope.launch(exceptionHandler) {
            val transaction =
                Transaction(
                    id = "",
                    type = state.type,
                    amountPaise = state.amountPaise,
                    accountId = state.accountId,
                    toAccountId = null,
                    categoryId = state.categoryId,
                    payee = null,
                    note = state.note,
                    occurredAt = Instant.now(),
                    cleared = true,
                    receiptPath = null,
                    goalId = null,
                    recurringId = null,
                    splitGroupId = null,
                    source = TransactionSource.MANUAL,
                )
            transactionRepository
                .createTransaction(transaction)
                .onSuccess { created ->
                    _uiState.value = state.copy(isSaving = false, savedTransactionId = created.id)
                }.onFailure { error ->
                    _uiState.value = state.copy(isSaving = false)
                    reportFeatureError(error)
                }
        }
    }
}
