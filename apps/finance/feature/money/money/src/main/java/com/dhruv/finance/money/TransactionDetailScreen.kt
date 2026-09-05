package com.dhruv.finance.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dhruv.core.ui.components.DisclaimerFooter
import com.dhruv.core.ui.components.ListGroup
import com.dhruv.core.ui.components.ListGroupRow
import com.dhruv.core.ui.components.MoneyText
import com.dhruv.core.ui.components.MoneyTextVariant
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxButtonVariant
import com.dhruv.core.ui.components.NxTopBar
import com.dhruv.core.ui.components.OfflineStateCard
import com.dhruv.core.ui.components.RetryErrorCard
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.components.SkeletonBlock
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.tracker.model.Transaction
import com.dhruv.finance.data.tracker.model.TransactionType
import com.dhruv.finance.money.MoneyConfig.transactionTypeLabels
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * D4 (transaction detail) — read-first (FR-009): every field readable without an edit mode, plus
 * the append-only HISTORY section (FR-007/FR-008). No budget-impact line this phase (spec.md
 * Assumptions "Budget impact deferred" — `budgets` doesn't exist until Phase 4).
 *
 * [onDuplicate]/[onMakeRecurring] are hand-off callbacks only — this screen never navigates or
 * writes on its own. [onDuplicate] is handed an already-built, unsaved [TransactionFormUiState]
 * (via [TransactionDetailViewModel.duplicateDraft]) so the caller can push D3 pre-filled; wiring
 * that push, and "make it recurring"'s own screen (US6, not built yet), is integration work outside
 * this module's boundary.
 */
@Composable
fun TransactionDetailScreen(
    viewModel: TransactionDetailViewModel,
    transactionId: String,
    onBack: () -> Unit,
    onDuplicate: (TransactionFormUiState) -> Unit,
    onMakeRecurring: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(transactionId) { viewModel.load(transactionId) }

    Column(modifier = modifier.fillMaxSize()) {
        NxTopBar(title = "Transaction", onBack = onBack)

        when (val current = state) {
            is TransactionDetailUiState.Loading ->
                Column(modifier = Modifier.padding(DhruvNextSpacing.screenGutter)) {
                    repeat(SKELETON_ROW_COUNT) {
                        SkeletonBlock(modifier = Modifier.padding(bottom = DhruvNextSpacing.interCardGap), height = 48.dp)
                    }
                }
            is TransactionDetailUiState.Offline ->
                OfflineStateCard(
                    onRetry = { viewModel.load(transactionId) },
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            is TransactionDetailUiState.Error ->
                RetryErrorCard(
                    message = current.message,
                    onRetry = { viewModel.load(transactionId) },
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            is TransactionDetailUiState.Loaded ->
                TransactionDetailContent(
                    state = current,
                    onDuplicate = { onDuplicate(viewModel.duplicateDraft(current.transaction)) },
                    onMakeRecurring = { onMakeRecurring(current.transaction) },
                )
        }
    }
}

@Composable
private fun TransactionDetailContent(
    state: TransactionDetailUiState.Loaded,
    onDuplicate: () -> Unit,
    onMakeRecurring: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val context = LocalContext.current
    val receiptStore = remember(context) { ReceiptStore(context) }
    val txn = state.transaction
    val zone = remember { ZoneId.systemDefault() }
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a").withZone(zone) }
    val historyDateFormatter = remember { DateTimeFormatter.ofPattern("d MMM, h:mm a").withZone(zone) }
    val receiptUri = remember(txn.receiptPath) { receiptStore.resolve(txn.receiptPath) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.sectionGap),
    ) {
        item {
            Column {
                MoneyText(
                    paise = txn.signedAmountPaise,
                    variant = MoneyTextVariant.Hero,
                    color = if (txn.type == TransactionType.INCOME) colors.pos else colors.tx,
                )
                androidx.compose.material3.Text(
                    text = txn.payee ?: transactionTypeLabels.getValue(txn.type.name),
                    color = colors.tx2,
                    fontSize = DhruvNextType.body,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        item {
            val rows =
                buildList<@Composable () -> Unit> {
                    add { ListGroupRow(title = "Date & time", subtitle = dateTimeFormatter.format(txn.occurredAt), showChevron = false) }
                    add {
                        ListGroupRow(
                            title = "Status",
                            subtitle = if (txn.cleared) "Cleared" else "Pending",
                            showChevron = false,
                        )
                    }
                    if (txn.type != TransactionType.TRANSFER) {
                        add {
                            ListGroupRow(
                                title = "Category",
                                subtitle = state.categoryName ?: "Uncategorised",
                                showChevron = false,
                            )
                        }
                    }
                    add { ListGroupRow(title = "Account", subtitle = state.accountName, showChevron = false) }
                    state.toAccountName?.let { to ->
                        add { ListGroupRow(title = "To account", subtitle = to, showChevron = false) }
                    }
                    if (!txn.note.isNullOrBlank()) {
                        add { ListGroupRow(title = "Note", subtitle = txn.note, showChevron = false) }
                    }
                }
            ListGroup(rows = rows)
        }

        if (receiptUri != null) {
            item {
                Column {
                    SectionLabel(text = "Receipt")
                    AsyncImage(
                        model = receiptUri,
                        contentDescription = "Attached receipt",
                        modifier =
                            Modifier
                                .padding(top = DhruvNextSpacing.interCardGap)
                                .fillMaxWidth()
                                .size(180.dp)
                                .clip(RoundedCornerShape(DhruvNextRadii.card)),
                    )
                    DisclaimerFooter(
                        text = "Stays on this device — not uploaded anywhere.",
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap)) {
                NxButton(
                    text = "Duplicate",
                    onClick = onDuplicate,
                    variant = NxButtonVariant.Outline,
                    modifier = Modifier.weight(1f),
                )
                NxButton(
                    text = "Make recurring",
                    onClick = onMakeRecurring,
                    variant = NxButtonVariant.Ghost,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item { SectionLabel(text = "History") }

        if (state.history.isEmpty()) {
            item {
                androidx.compose.material3.Text(
                    text = "No history yet",
                    color = colors.tx3,
                    fontSize = DhruvNextType.body,
                )
            }
        } else {
            items(state.history) { entry ->
                Column(modifier = Modifier.padding(bottom = DhruvNextSpacing.inputGroupGap)) {
                    androidx.compose.material3.Text(
                        text = historyDateFormatter.format(entry.at),
                        color = colors.tx3,
                        fontSize = DhruvNextType.meta,
                    )
                    entry.lines.forEach { line ->
                        androidx.compose.material3.Text(
                            text = line,
                            color = colors.tx,
                            fontSize = DhruvNextType.body,
                        )
                    }
                }
            }
        }

        item {
            DisclaimerFooter(text = "This history can't be edited or removed.")
        }
    }
}

private const val SKELETON_ROW_COUNT = 6
