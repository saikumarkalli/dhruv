package com.dhruv.finance.money

import androidx.lifecycle.viewModelScope
import com.dhruv.core.format.Paise
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.tracker.model.Transaction
import com.dhruv.finance.data.tracker.model.TransactionEvent
import com.dhruv.finance.data.tracker.model.TransactionEventKind
import com.dhruv.finance.data.tracker.repo.AccountRepository
import com.dhruv.finance.data.tracker.repo.CategoryRepository
import com.dhruv.finance.data.tracker.repo.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Instant

/**
 * One rendered line of a transaction's audit trail (FR-007/FR-009). [lines] holds more than one
 * entry only for [TransactionEventKind.EDITED] — one line per changed field, per T051/spec.md's
 * worked example ("EDITED -> one line per changed field").
 */
data class TransactionHistoryItem(
    val at: Instant,
    val kind: TransactionEventKind,
    val lines: List<String>,
)

/**
 * D4's screen state (spec.md Story 4; FR-009/FR-032). Deliberately carries **no** budget-impact
 * field this phase — `budgets` doesn't exist until Phase 4 (spec.md Assumptions "Budget impact
 * deferred"); the omission is the correct behaviour, not a gap to fill in later on this class.
 */
sealed interface TransactionDetailUiState {
    data object Loading : TransactionDetailUiState

    data class Loaded(
        val transaction: Transaction,
        val categoryName: String?,
        val accountName: String,
        val toAccountName: String?,
        val history: List<TransactionHistoryItem>,
    ) : TransactionDetailUiState

    data class Error(val message: String) : TransactionDetailUiState

    /** No connectivity and nothing cached for this transaction yet (FR-032's offline state). */
    data object Offline : TransactionDetailUiState
}

/**
 * D4 (transaction detail) — read-first: the transaction plus its full append-only history
 * (FR-009), rendered from [TransactionRepository.getTransaction]/[TransactionRepository.listEvents]
 * with no edit-mode entry point. Duplicate ([duplicateDraft]) and "make it recurring" are the only
 * write-adjacent affordances this phase ships; make-recurring itself is a plain
 * `onMakeRecurring: (Transaction) -> Unit` callback on [TransactionDetailScreen] (US6/recurring
 * templates don't exist yet), so it needs no ViewModel-side method beyond exposing [transaction]
 * via [uiState].
 */
class TransactionDetailViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "money") {
    private val _uiState = MutableStateFlow<TransactionDetailUiState>(TransactionDetailUiState.Loading)
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    fun load(transactionId: String) {
        performanceTracer.trace("money_transaction_detail_load") {
            _uiState.value = TransactionDetailUiState.Loading
        }
        viewModelScope.launch(exceptionHandler) {
            val transactionResult = transactionRepository.getTransaction(transactionId)
            val failure = transactionResult.exceptionOrNull()
            if (failure != null) {
                _uiState.value =
                    if (failure is IOException) {
                        TransactionDetailUiState.Offline
                    } else {
                        TransactionDetailUiState.Error(failure.message ?: "Couldn't load this transaction")
                    }
                return@launch
            }

            val transaction = transactionResult.getOrNull()
            if (transaction == null) {
                _uiState.value = TransactionDetailUiState.Error("This transaction couldn't be found")
                return@launch
            }

            val events = transactionRepository.listEvents(transactionId).getOrDefault(emptyList())
            val categories = categoryRepository.listCategories().getOrDefault(emptyList())
            val accounts = accountRepository.listAccounts().getOrDefault(emptyList())
            val categoryNames = categories.associate { it.id to it.name }
            val accountNames = accounts.associate { it.id to it.name }

            _uiState.value =
                TransactionDetailUiState.Loaded(
                    transaction = transaction,
                    categoryName = transaction.categoryId?.let { categoryNames[it] },
                    accountName = accountNames[transaction.accountId] ?: transaction.accountId,
                    toAccountName = transaction.toAccountId?.let { accountNames[it] ?: it },
                    history = events.map { renderEvent(it, categoryNames) },
                )
        }
    }

    /**
     * FR-010 / spec Edge Cases: an unsaved draft pre-filled from [transaction] for D3's Duplicate
     * action. A brand-new [TransactionFormUiState] — never [transaction]'s id, source or history —
     * so once the caller actually saves it, the copy's own history starts fresh at CREATED rather
     * than chaining from the original. This method itself writes nothing; the caller (D3) decides
     * if and when to save.
     */
    fun duplicateDraft(transaction: Transaction): TransactionFormUiState =
        TransactionFormUiState(
            type = transaction.type,
            amountPaise = transaction.amountPaise,
            accountId = transaction.accountId,
            toAccountId = transaction.toAccountId,
            categoryId = transaction.categoryId,
            payee = transaction.payee.orEmpty(),
            note = transaction.note.orEmpty(),
            cleared = transaction.cleared,
        )

    private fun renderEvent(
        event: TransactionEvent,
        categoryNames: Map<String, String>,
    ): TransactionHistoryItem {
        val detail = event.detail.orEmpty()
        val lines =
            when (event.kind) {
                TransactionEventKind.CREATED -> listOf("Created")
                TransactionEventKind.ACCEPTED_FROM_RECURRING -> listOf("Created from a recurring entry")
                TransactionEventKind.RECONCILED -> listOf("Created as a reconciliation adjustment")
                TransactionEventKind.DELETED -> listOf("Deleted")
                TransactionEventKind.CATEGORY_CHANGED -> listOf(renderCategoryChanged(detail, categoryNames))
                TransactionEventKind.EDITED -> renderEditedLines(detail)
            }
        return TransactionHistoryItem(at = event.at, kind = event.kind, lines = lines)
    }

    private fun renderCategoryChanged(
        detail: Map<String, Any?>,
        categoryNames: Map<String, String>,
    ): String {
        val old = categoryNames[detail["old_category_id"]?.toString()] ?: "no category"
        val new = categoryNames[detail["new_category_id"]?.toString()] ?: "no category"
        return "Category changed from $old to $new"
    }

    private fun renderEditedLines(detail: Map<String, Any?>): List<String> =
        detail.mapNotNull { (field, change) ->
            val pair = change as? Map<*, *>
            val old = pair?.get("old")
            val new = pair?.get("new")
            when (field) {
                "amount_paise" -> "Amount changed from ${formatPaiseDetail(old)} to ${formatPaiseDetail(new)}"
                "payee" -> "Payee changed from ${old ?: "none"} to ${new ?: "none"}"
                "note" -> "Note changed"
                "occurred_at" -> "Date changed"
                "cleared" -> if (new == true) "Marked cleared" else "Marked uncleared"
                "account_id" -> "Account changed"
                else -> null
            }
        }

    private fun formatPaiseDetail(value: Any?): String {
        val paise = (value as? Number)?.toLong() ?: return "—"
        return Paise.format(paise)
    }
}
