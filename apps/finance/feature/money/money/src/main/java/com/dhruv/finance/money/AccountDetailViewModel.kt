package com.dhruv.finance.money

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.tracker.model.Account
import com.dhruv.finance.data.tracker.model.Transaction
import com.dhruv.finance.data.tracker.model.TransactionType
import com.dhruv.finance.data.tracker.net.ErrorMapper
import com.dhruv.finance.data.tracker.net.TrackerError
import com.dhruv.finance.data.tracker.repo.AccountRepository
import com.dhruv.finance.data.tracker.repo.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth

/** One row of D7's "recent activity", paired with the account's own running balance immediately
 * after that transaction posted (FR-019). */
data class ActivityRow(
    val transaction: Transaction,
    val runningBalancePaise: Long,
)

/** D7's screen state (spec.md Story 3 Acceptance Scenario 4/5; FR-019/FR-020/FR-021/FR-032). */
sealed interface AccountDetailUiState {
    data object Loading : AccountDetailUiState

    data class Loaded(
        val account: Account,
        /** Running-balance progression across [activity], oldest first — feeds [com.dhruv.core.ui.components.TrendSparkline]. */
        val trend: List<Float>,
        val monthInPaise: Long,
        val monthOutPaise: Long,
        val activity: List<ActivityRow>,
        val isStale: Boolean,
        val staleMessage: String,
    ) : AccountDetailUiState

    data class Error(
        val message: String,
    ) : AccountDetailUiState

    data object Offline : AccountDetailUiState

    data object SignedOut : AccountDetailUiState
}

/** D7's delete confirmation (FR-021a, Edge Cases). A zero-transaction account confirms directly;
 * one with transactions still confirms — its transactions are unaffected by a soft-delete, they
 * simply no longer belong to a visible account — but names the exact count first so nothing is a
 * surprise. */
sealed interface AccountDeletePrompt {
    data object None : AccountDeletePrompt

    data class Confirm(
        val transactionCount: Int,
    ) : AccountDeletePrompt
}

/**
 * D7 (account detail, US3) — balance, trend, month in/out and a running-balance activity feed, all
 * derived from [TransactionRepository.listForMonth] (the current month only, mirroring D1/US1's
 * scope) rather than a dedicated per-account endpoint. [reconcile] delegates to
 * [AccountRepository.reconcileAccount] (FR-021) and reloads on success so the staleness flag and
 * balance reflect the reconciliation immediately (spec.md Story 3 Acceptance Scenario 5).
 */
class AccountDetailViewModel(
    private val accountId: String,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "money") {
    private val _uiState = MutableStateFlow<AccountDetailUiState>(AccountDetailUiState.Loading)
    val uiState: StateFlow<AccountDetailUiState> = _uiState.asStateFlow()

    private val _deletePrompt = MutableStateFlow<AccountDeletePrompt>(AccountDeletePrompt.None)
    val deletePrompt: StateFlow<AccountDeletePrompt> = _deletePrompt.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    private var isReconciling = false

    init {
        load()
    }

    fun load() {
        performanceTracer.trace("money_account_detail_load") { _uiState.value = AccountDetailUiState.Loading }
        viewModelScope.launch(exceptionHandler) {
            val accountsResult = accountRepository.listAccounts()
            val account = accountsResult.getOrNull()?.firstOrNull { it.id == accountId }

            if (accountsResult.isFailure) {
                _uiState.value = accountsResult.exceptionOrNull()!!.toUiState()
                return@launch
            }
            if (account == null) {
                _uiState.value = AccountDetailUiState.Error("This account is no longer available")
                return@launch
            }

            transactionRepository
                .listForMonth(YearMonth.now())
                .onSuccess { transactions -> _uiState.value = buildLoadedState(account, transactions) }
                .onFailure { error -> _uiState.value = error.toUiState() }
        }
    }

    /** FR-021: reconciles with the user-stated real balance, then reloads so D7 reflects the
     * cleared staleness flag and any adjustment transaction immediately. */
    fun reconcile(statedBalancePaise: Long) {
        if (isReconciling) return
        performanceTracer.trace("money_account_reconcile") { isReconciling = true }
        viewModelScope.launch(exceptionHandler) {
            accountRepository
                .reconcileAccount(accountId, statedBalancePaise)
                .onSuccess { load() }
                .onFailure { error -> _uiState.value = error.toUiState() }
            isReconciling = false
        }
    }

    /** FR-021a — resolves the exact transaction count before showing the confirmation. */
    fun requestDelete() {
        viewModelScope.launch(exceptionHandler) {
            accountRepository.countTransactionsForAccount(accountId).onSuccess { count ->
                _deletePrompt.value = AccountDeletePrompt.Confirm(count)
            }
        }
    }

    fun confirmDelete() {
        viewModelScope.launch(exceptionHandler) {
            accountRepository.softDeleteAccount(accountId).onSuccess {
                _deletePrompt.value = AccountDeletePrompt.None
                _deleted.value = true
            }
        }
    }

    fun dismissDeletePrompt() {
        _deletePrompt.value = AccountDeletePrompt.None
    }

    private fun buildLoadedState(
        account: Account,
        monthTransactions: List<Transaction>,
    ): AccountDetailUiState.Loaded {
        val related = monthTransactions.filter { it.accountId == accountId || it.toAccountId == accountId }
        val windowNetPaise = related.sumOf { it.effectOn(accountId) }
        val currentBalancePaise = account.balancePaise ?: account.openingBalancePaise
        val balanceBeforeWindow = currentBalancePaise - windowNetPaise

        var running = balanceBeforeWindow
        val ascending = related.sortedBy { it.occurredAt }
        val runningById = LinkedHashMap<String, Long>()
        ascending.forEach { txn ->
            running += txn.effectOn(accountId)
            runningById[txn.id] = running
        }

        val activity =
            related
                .sortedByDescending { it.occurredAt }
                .map { txn -> ActivityRow(txn, runningById.getValue(txn.id)) }

        val monthInPaise =
            related
                .filter { it.type == TransactionType.INCOME && it.accountId == accountId }
                .sumOf { it.amountPaise } +
                related.filter { it.type == TransactionType.TRANSFER && it.toAccountId == accountId }.sumOf { it.amountPaise }
        val monthOutPaise =
            related
                .filter { (it.type == TransactionType.EXPENSE || it.type == TransactionType.TRANSFER) && it.accountId == accountId }
                .sumOf { it.amountPaise }

        return AccountDetailUiState.Loaded(
            account = account,
            trend = listOf(balanceBeforeWindow.toFloat()) + ascending.map { runningById.getValue(it.id).toFloat() },
            monthInPaise = monthInPaise,
            monthOutPaise = monthOutPaise,
            activity = activity,
            isStale = account.isStale(),
            staleMessage = account.staleMessage(),
        )
    }

    /** This transaction's signed effect on [accountId]'s own balance (mirrors
     * `finance.v_account_balances`'s server-side arithmetic, data-model.md). */
    private fun Transaction.effectOn(accountId: String): Long =
        when {
            type == TransactionType.INCOME && this.accountId == accountId -> amountPaise
            type == TransactionType.EXPENSE && this.accountId == accountId -> -amountPaise
            type == TransactionType.TRANSFER && this.accountId == accountId -> -amountPaise
            type == TransactionType.TRANSFER && this.toAccountId == accountId -> amountPaise
            else -> 0L
        }

    private fun Throwable.toUiState(): AccountDetailUiState =
        when (ErrorMapper.map(this)) {
            TrackerError.NetworkUnavailable -> AccountDetailUiState.Offline
            TrackerError.NotAuthenticated -> AccountDetailUiState.SignedOut
            else -> AccountDetailUiState.Error(message ?: "Couldn't load this account")
        }
}
