package com.dhruv.finance.money

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.tracker.model.Account
import com.dhruv.finance.data.tracker.model.AccountType
import com.dhruv.finance.data.tracker.net.ErrorMapper
import com.dhruv.finance.data.tracker.net.TrackerError
import com.dhruv.finance.data.tracker.repo.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** D6's screen state (spec.md Story 3; FR-016/017/018/020/032). */
sealed interface AccountsUiState {
    data object Loading : AccountsUiState

    data class Loaded(
        /** FR-017: bank + cash + wallet balances only — credit is money owed, never counted here. */
        val spendableNowPaise: Long,
        val bankAccounts: List<Account>,
        val cashAndWalletAccounts: List<Account>,
        val creditAccounts: List<Account>,
    ) : AccountsUiState {
        val isEmpty: Boolean
            get() = bankAccounts.isEmpty() && cashAndWalletAccounts.isEmpty() && creditAccounts.isEmpty()
    }

    data class Error(val message: String) : AccountsUiState

    data object Offline : AccountsUiState

    data object SignedOut : AccountsUiState
}

/**
 * D6 (accounts list) — "spendable now" (FR-017, `MNY-BR-002`) is a filtered client-side sum over
 * `AccountRepository.listAccounts()`'s server-computed balances (`v_account_balances`, NFR-8),
 * never a client re-derivation of *which* accounts count — that flag ([Account.countsAsSpendable])
 * is itself server-truth via the account's [AccountType].
 */
class AccountsViewModel(
    private val accountRepository: AccountRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "money") {
    private val _uiState = MutableStateFlow<AccountsUiState>(AccountsUiState.Loading)
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        performanceTracer.trace("money_accounts_load") { _uiState.value = AccountsUiState.Loading }
        viewModelScope.launch(exceptionHandler) {
            accountRepository
                .listAccounts()
                .onSuccess { accounts -> _uiState.value = accounts.toLoadedState() }
                .onFailure { error -> _uiState.value = error.toUiState() }
        }
    }

    private fun List<Account>.toLoadedState(): AccountsUiState.Loaded =
        AccountsUiState.Loaded(
            spendableNowPaise = filter { it.countsAsSpendable }.sumOf { it.balancePaise ?: 0L },
            bankAccounts = filter { it.type == AccountType.BANK },
            cashAndWalletAccounts = filter { it.type == AccountType.CASH || it.type == AccountType.WALLET },
            creditAccounts = filter { it.type == AccountType.CREDIT_CARD },
        )

    private fun Throwable.toUiState(): AccountsUiState =
        when (ErrorMapper.map(this)) {
            TrackerError.NetworkUnavailable -> AccountsUiState.Offline
            TrackerError.NotAuthenticated -> AccountsUiState.SignedOut
            else -> AccountsUiState.Error(message ?: "Couldn't load your accounts")
        }
}
