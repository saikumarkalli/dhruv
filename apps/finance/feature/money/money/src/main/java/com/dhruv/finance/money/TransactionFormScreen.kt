package com.dhruv.finance.money

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxSelect
import com.dhruv.core.ui.components.NxTextArea
import com.dhruv.core.ui.components.NxTextField
import com.dhruv.core.ui.components.SegmentedRow
import com.dhruv.core.ui.components.SelectionSheet
import com.dhruv.core.ui.components.rememberDiscardGuard
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.tracker.model.TransactionType
import com.dhruv.finance.money.MoneyConfig.transactionTypeLabels

/**
 * D3 (full transaction form) — full-screen modal, close ✕ not back (navigation law N2/routes.md).
 * Every FR-004 field except goal-link (deferred to Phase 4, `goals` doesn't exist yet) and the
 * recurring toggle (US6, T068). [rememberDiscardGuard] confirms before discarding (FR-005, N4).
 *
 * NOTE: `NxTopBar` has no close(✕) variant, only `onBack` (a fixed back-arrow icon) — using it
 * here would render the wrong glyph for a modal. This screen builds its own minimal top row
 * instead of mis-using that component; extending `NxTopBar` with a close variant is a follow-up,
 * not done here to keep this phase's scope to the form itself.
 */
@Composable
fun TransactionFormScreen(
    viewModel: TransactionFormViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalDhruvNextColors.current
    var pickerTarget by remember { mutableStateOf<String?>(null) }
    val currentIsDirty by rememberUpdatedState(state.isDirty)

    val (guard, discardDialog) = rememberDiscardGuard(isDirty = { currentIsDirty }, onDiscardConfirmed = onClose)

    LaunchedEffect(Unit) { viewModel.open() }
    LaunchedEffect(state.savedTransactionId) { if (state.savedTransactionId != null) onClose() }

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { guard.attemptDismiss() }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = colors.tx)
            }
            Text(
                text = "New transaction",
                color = colors.tx,
                fontSize = DhruvNextType.title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(DhruvNextSpacing.screenGutter),
        ) {
            SegmentedRow(
                options = TransactionType.entries.map { transactionTypeLabels.getValue(it.name) },
                selectedIndex = TransactionType.entries.indexOf(state.type),
                onSelected = { index -> viewModel.setType(TransactionType.entries[index]) },
            )

            NxTextField(
                value = if (state.amountPaise == 0L) "" else (state.amountPaise / 100.0).toString(),
                onValueChange = { text ->
                    val rupees = text.toDoubleOrNull() ?: 0.0
                    viewModel.setAmount((rupees * 100).toLong())
                },
                label = "Amount",
                prefix = "₹",
                modifier = Modifier.fillMaxWidth().padding(top = DhruvNextSpacing.interCardGap),
            )

            if (state.type != TransactionType.TRANSFER) {
                NxSelect(
                    label = "Category",
                    value = state.categoryOptions.firstOrNull { it.id == state.categoryId }?.label.orEmpty(),
                    onClick = { pickerTarget = "category" },
                    errorMessage = state.validationError?.takeIf { it.contains("category", ignoreCase = true) },
                    modifier = Modifier.fillMaxWidth().padding(top = DhruvNextSpacing.interCardGap),
                )
            }

            NxSelect(
                label = if (state.type == TransactionType.TRANSFER) "From account" else "Account",
                value = state.accountOptions.firstOrNull { it.id == state.accountId }?.label.orEmpty(),
                onClick = { pickerTarget = "account" },
                modifier = Modifier.fillMaxWidth().padding(top = DhruvNextSpacing.interCardGap),
            )

            if (state.type == TransactionType.TRANSFER) {
                NxSelect(
                    label = "To account",
                    value = state.accountOptions.firstOrNull { it.id == state.toAccountId }?.label.orEmpty(),
                    onClick = { pickerTarget = "toAccount" },
                    errorMessage = state.validationError?.takeIf { it.contains("destination", ignoreCase = true) },
                    modifier = Modifier.fillMaxWidth().padding(top = DhruvNextSpacing.interCardGap),
                )
            }

            NxTextField(
                value = state.payee,
                onValueChange = { viewModel.setPayee(it) },
                label = "Payee",
                modifier = Modifier.fillMaxWidth().padding(top = DhruvNextSpacing.interCardGap),
            )

            NxTextArea(
                value = state.note,
                onValueChange = { viewModel.setNote(it) },
                label = "Note",
                helperText = "Optional",
                modifier = Modifier.fillMaxWidth().padding(top = DhruvNextSpacing.interCardGap),
            )

            if (state.validationError != null) {
                Text(
                    text = state.validationError.orEmpty(),
                    color = colors.neg,
                    fontSize = DhruvNextType.meta,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            NxButton(
                text = "Save",
                onClick = { viewModel.save() },
                loading = state.isSaving,
                block = true,
                modifier = Modifier.padding(top = DhruvNextSpacing.sectionGap),
            )
        }
    }

    discardDialog()

    when (pickerTarget) {
        "category" ->
            SelectionSheet(
                title = "Category",
                options = state.categoryOptions,
                selectedIds = setOfNotNull(state.categoryId),
                onDismissRequest = { pickerTarget = null },
                onSelectionChanged = { ids -> ids.firstOrNull()?.let { viewModel.setCategory(it) } },
            )
        "account" ->
            SelectionSheet(
                title = "Account",
                options = state.accountOptions,
                selectedIds = setOfNotNull(state.accountId),
                onDismissRequest = { pickerTarget = null },
                onSelectionChanged = { ids -> ids.firstOrNull()?.let { viewModel.setAccount(it) } },
            )
        "toAccount" ->
            SelectionSheet(
                title = "To account",
                options = state.accountOptions,
                selectedIds = setOfNotNull(state.toAccountId),
                onDismissRequest = { pickerTarget = null },
                onSelectionChanged = { ids -> ids.firstOrNull()?.let { viewModel.setToAccount(it) } },
            )
    }
}
