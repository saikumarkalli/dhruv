package com.dhruv.finance.money

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.core.ui.components.SelectionOption
import com.dhruv.finance.data.tracker.model.Transaction
import com.dhruv.finance.data.tracker.model.TransactionSource
import com.dhruv.finance.data.tracker.model.TransactionType
import com.dhruv.finance.data.tracker.repo.AccountRepository
import com.dhruv.finance.data.tracker.repo.CategoryRepository
import com.dhruv.finance.data.tracker.repo.RecurringRepository
import com.dhruv.finance.data.tracker.repo.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

/** D3's full-form state (FR-004). [isDirty] backs `rememberDiscardGuard`'s confirm-on-discard
 * (FR-005, N4) — every field setter marks it true; only a successful save resets it.
 *
 * The goal-link field is deliberately absent — `goals` does not exist until Phase 4
 * (spec.md Assumptions, T029). [makeRecurring]/[rrule] are US6/T068: when on, [save] writes ONLY
 * a `recurring_templates` row (FR-027 — "no duplicate immediate transaction"), never a
 * transaction. */
data class TransactionFormUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountPaise: Long = 0,
    val accountId: String? = null,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val payee: String = "",
    val note: String = "",
    val cleared: Boolean = true,
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val savedTransactionId: String? = null,
    val accountOptions: List<SelectionOption> = emptyList(),
    val categoryOptions: List<SelectionOption> = emptyList(),
    val validationError: String? = null,
    val makeRecurring: Boolean = false,
    val rrule: String = "FREQ=MONTHLY",
    val savedRecurringTemplateId: String? = null,
)

/** D3 (full transaction form) — every field FR-004 promises beyond D2's quick-add surface. */
class TransactionFormViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringRepository: RecurringRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "money") {
    private val _uiState = MutableStateFlow(TransactionFormUiState())
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()

    fun open(prefill: TransactionFormUiState? = null) {
        if (prefill != null) _uiState.value = prefill.copy(isDirty = false)
        viewModelScope.launch(exceptionHandler) {
            accountRepository.listAccounts().onSuccess { accounts ->
                _uiState.value = _uiState.value.copy(accountOptions = accounts.map { SelectionOption(it.id, it.name) })
            }
            categoryRepository.listCategories().onSuccess { categories ->
                _uiState.value = _uiState.value.copy(categoryOptions = categories.map { SelectionOption(it.id, it.name) })
            }
        }
    }

    fun setType(type: TransactionType) = update { it.copy(type = type, isDirty = true) }

    fun setAmount(paise: Long) = update { it.copy(amountPaise = paise, isDirty = true) }

    fun setAccount(accountId: String) = update { it.copy(accountId = accountId, isDirty = true) }

    fun setToAccount(accountId: String?) = update { it.copy(toAccountId = accountId, isDirty = true) }

    fun setCategory(categoryId: String?) = update { it.copy(categoryId = categoryId, isDirty = true) }

    fun setPayee(payee: String) = update { it.copy(payee = payee, isDirty = true) }

    fun setNote(note: String) = update { it.copy(note = note, isDirty = true) }

    fun setCleared(cleared: Boolean) = update { it.copy(cleared = cleared, isDirty = true) }

    fun setMakeRecurring(makeRecurring: Boolean) = update { it.copy(makeRecurring = makeRecurring, isDirty = true) }

    fun setRrule(rrule: String) = update { it.copy(rrule = rrule, isDirty = true) }

    private inline fun update(block: (TransactionFormUiState) -> TransactionFormUiState) {
        _uiState.value = block(_uiState.value)
    }

    fun save() {
        val state = _uiState.value
        val error = validate(state)
        if (error != null) {
            _uiState.value = state.copy(validationError = error)
            return
        }

        performanceTracer.trace("money_transaction_form_save") {
            _uiState.value = state.copy(isSaving = true, validationError = null)
        }
        viewModelScope.launch(exceptionHandler) {
            val transaction =
                Transaction(
                    id = "",
                    type = state.type,
                    amountPaise = state.amountPaise,
                    accountId = state.accountId!!,
                    toAccountId = state.toAccountId,
                    categoryId = state.categoryId,
                    payee = state.payee.ifBlank { null },
                    note = state.note.ifBlank { null },
                    occurredAt = Instant.now(),
                    cleared = state.cleared,
                    receiptPath = null,
                    goalId = null,
                    recurringId = null,
                    splitGroupId = null,
                    source = TransactionSource.MANUAL,
                )

            if (state.makeRecurring) {
                // FR-027: only the recurring_templates row is written — no immediate duplicate.
                recurringRepository
                    .createFromTransaction(transaction, state.rrule, nextRun = LocalDate.now().plusMonths(1))
                    .onSuccess { template ->
                        _uiState.value = state.copy(isSaving = false, isDirty = false, savedRecurringTemplateId = template.id)
                    }.onFailure { thr ->
                        _uiState.value = state.copy(isSaving = false, validationError = thr.message)
                    }
                return@launch
            }

            transactionRepository
                .createTransaction(transaction)
                .onSuccess { created ->
                    _uiState.value = state.copy(isSaving = false, isDirty = false, savedTransactionId = created.id)
                }.onFailure { thr ->
                    _uiState.value = state.copy(isSaving = false, validationError = thr.message)
                }
        }
    }

    private fun validate(state: TransactionFormUiState): String? =
        when {
            state.amountPaise <= 0 -> "Enter an amount"
            state.accountId == null -> "Choose an account"
            state.type == TransactionType.TRANSFER && state.toAccountId == null -> "Choose a destination account"
            state.type == TransactionType.TRANSFER && state.toAccountId == state.accountId ->
                "Destination must differ from the source account"
            state.type != TransactionType.TRANSFER && state.categoryId == null -> "Choose a category"
            else -> null
        }
}
