package com.dhruv.finance.money

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.tracker.model.Account
import com.dhruv.finance.data.tracker.model.AccountType
import com.dhruv.finance.data.tracker.repo.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val ACCOUNT_MASK_LENGTH = 4
private const val ACCOUNT_NAME_MAX_LENGTH = 60
private const val MONTH_DAY_MIN = 1
private const val MONTH_DAY_MAX = 31

/** D6's add/edit account form state (FR-016). [editingId] tracks whether [save] creates or
 * updates — set by [open], never mutated afterwards. */
data class AccountFormUiState(
    val name: String = "",
    val type: AccountType = AccountType.BANK,
    val mask: String = "",
    val isPrimary: Boolean = false,
    val openingBalancePaise: Long = 0,
    val limitPaise: Long? = null,
    val dueDay: Int? = null,
    val isSaving: Boolean = false,
    val savedAccountId: String? = null,
    val validationError: String? = null,
    val isEditing: Boolean = false,
)

/**
 * Add/edit account (US3, FR-016) — name, type, a last-4-only mask (validated/truncated at every
 * keystroke, data-model.md), a single primary flag, opening balance, and credit-only limit/due-day
 * fields the screen shows only when [AccountFormUiState.type] is [AccountType.CREDIT_CARD].
 */
class AccountFormViewModel(
    private val accountRepository: AccountRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "money") {
    private val _uiState = MutableStateFlow(AccountFormUiState())
    val uiState: StateFlow<AccountFormUiState> = _uiState.asStateFlow()

    private var editingId: String? = null

    /** Pass an existing [Account] to edit it, or `null` to start a fresh create form. */
    fun open(existing: Account?) {
        editingId = existing?.id
        _uiState.value =
            if (existing != null) {
                AccountFormUiState(
                    name = existing.name,
                    type = existing.type,
                    mask = existing.mask.orEmpty(),
                    isPrimary = existing.isPrimary,
                    openingBalancePaise = existing.openingBalancePaise,
                    limitPaise = existing.limitPaise,
                    dueDay = existing.dueDay,
                    isEditing = true,
                )
            } else {
                AccountFormUiState()
            }
    }

    fun setName(name: String) = update { it.copy(name = name.take(ACCOUNT_NAME_MAX_LENGTH), validationError = null) }

    fun setType(type: AccountType) =
        update {
            it.copy(
                type = type,
                limitPaise = if (type == AccountType.CREDIT_CARD) it.limitPaise else null,
                dueDay = if (type == AccountType.CREDIT_CARD) it.dueDay else null,
                validationError = null,
            )
        }

    /** Last 4 digits only, never a full account number (data-model.md "Account"). */
    fun setMask(rawInput: String) = update { it.copy(mask = rawInput.filter(Char::isDigit).takeLast(ACCOUNT_MASK_LENGTH)) }

    fun setPrimary(primary: Boolean) = update { it.copy(isPrimary = primary) }

    fun setOpeningBalance(paise: Long) = update { it.copy(openingBalancePaise = paise) }

    fun setLimit(paise: Long?) = update { it.copy(limitPaise = paise) }

    fun setDueDay(day: Int?) = update { it.copy(dueDay = day) }

    private inline fun update(block: (AccountFormUiState) -> AccountFormUiState) {
        _uiState.value = block(_uiState.value)
    }

    fun save() {
        val state = _uiState.value
        val error = validate(state)
        if (error != null) {
            _uiState.value = state.copy(validationError = error)
            return
        }

        performanceTracer.trace("money_account_form_save") { _uiState.value = state.copy(isSaving = true, validationError = null) }
        viewModelScope.launch(exceptionHandler) {
            val account =
                Account(
                    id = editingId ?: "",
                    name = state.name.trim(),
                    type = state.type,
                    mask = state.mask.ifBlank { null },
                    isPrimary = state.isPrimary,
                    limitPaise = if (state.type == AccountType.CREDIT_CARD) state.limitPaise else null,
                    dueDay = if (state.type == AccountType.CREDIT_CARD) state.dueDay else null,
                    openingBalancePaise = state.openingBalancePaise,
                    reconciledAt = null,
                )
            val result = if (editingId != null) accountRepository.updateAccount(account) else accountRepository.createAccount(account)
            result
                .onSuccess { saved -> _uiState.value = state.copy(isSaving = false, savedAccountId = saved.id) }
                .onFailure { thr -> _uiState.value = state.copy(isSaving = false, validationError = thr.message) }
        }
    }

    private fun validate(state: AccountFormUiState): String? =
        when {
            state.name.isBlank() -> "Enter an account name"
            state.mask.isNotEmpty() && state.mask.length > ACCOUNT_MASK_LENGTH -> "Mask can only hold the last 4 digits"
            state.type == AccountType.CREDIT_CARD &&
                state.dueDay != null &&
                (state.dueDay < MONTH_DAY_MIN || state.dueDay > MONTH_DAY_MAX) ->
                "Due day must be between 1 and 31"
            else -> null
        }
}
