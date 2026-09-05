package com.dhruv.finance.money

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.format.Paise
import com.dhruv.core.ui.components.DhruvModalSheet
import com.dhruv.core.ui.components.NumericKeypad
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxSelect
import com.dhruv.core.ui.components.NxTextField
import com.dhruv.core.ui.components.SegmentedRow
import com.dhruv.core.ui.components.SelectionSheet
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.money.MoneyConfig.transactionTypeLabels
import com.dhruv.finance.data.tracker.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

private const val MAX_RUPEE_DIGITS = 9

/**
 * D2 (quick add) — amount-first, reaches a saved transaction in three taps after the amount
 * (FR-002, SC-001): amount via [NumericKeypad], a type [SegmentedRow], pre-guessed category/
 * account (both editable), an optional note, and "More options" hand-off to D3 (Acceptance
 * Scenario 4) that carries over everything already entered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    viewModel: QuickAddViewModel,
    onDismissRequest: () -> Unit,
    onSaved: () -> Unit,
    onMoreOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalDhruvNextColors.current
    var digits by remember { mutableStateOf("") }
    var pickerTarget by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.open() }
    LaunchedEffect(state.savedTransactionId) { if (state.savedTransactionId != null) onSaved() }

    DhruvModalSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(DhruvNextSpacing.screenGutter)) {
            Text(
                text = Paise.format(state.amountPaise),
                color = colors.tx,
                fontSize = DhruvNextType.hero,
                fontWeight = FontWeight.Bold,
            )

            SegmentedRow(
                options = TransactionType.entries.map { transactionTypeLabels.getValue(it.name) },
                selectedIndex = TransactionType.entries.indexOf(state.type),
                onSelected = { index -> viewModel.setType(TransactionType.entries[index]) },
                modifier = Modifier.padding(top = DhruvNextSpacing.interCardGap),
            )

            Row(modifier = Modifier.padding(top = DhruvNextSpacing.interCardGap)) {
                NxSelect(
                    label = "Category",
                    value = state.categoryOptions.firstOrNull { it.id == state.categoryId }?.label.orEmpty(),
                    onClick = { pickerTarget = "category" },
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                NxSelect(
                    label = "Account",
                    value = state.accountOptions.firstOrNull { it.id == state.accountId }?.label.orEmpty(),
                    onClick = { pickerTarget = "account" },
                    modifier = Modifier.weight(1f),
                )
            }

            NxTextField(
                value = state.note.orEmpty(),
                onValueChange = { viewModel.setNote(it) },
                label = "Note (optional)",
                modifier = Modifier.fillMaxWidth().padding(top = DhruvNextSpacing.interCardGap),
            )

            NumericKeypad(
                rows = quickAddKeypadRows(),
                onKeyPress = { key ->
                    when (key) {
                        "⌫" -> {
                            digits = digits.dropLast(1)
                            viewModel.setAmount(digitsToPaise(digits))
                        }
                        else -> if (digits.length < MAX_RUPEE_DIGITS) {
                            digits += key
                            viewModel.setAmount(digitsToPaise(digits))
                        }
                    }
                },
                modifier = Modifier.padding(top = DhruvNextSpacing.interCardGap),
            )

            Row(modifier = Modifier.padding(top = DhruvNextSpacing.interCardGap)) {
                NxButton(
                    text = "More options",
                    onClick = onMoreOptions,
                    variant = com.dhruv.core.ui.components.NxButtonVariant.Ghost,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                NxButton(
                    text = "Save",
                    onClick = { viewModel.save() },
                    loading = state.isSaving,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (pickerTarget == "category") {
        SelectionSheet(
            title = "Category",
            options = state.categoryOptions,
            selectedIds = setOfNotNull(state.categoryId),
            onDismissRequest = { pickerTarget = null },
            onSelectionChanged = { ids -> ids.firstOrNull()?.let { viewModel.setCategory(it) } },
        )
    } else if (pickerTarget == "account") {
        SelectionSheet(
            title = "Account",
            options = state.accountOptions,
            selectedIds = setOfNotNull(state.accountId),
            onDismissRequest = { pickerTarget = null },
            onSelectionChanged = { ids -> ids.firstOrNull()?.let { viewModel.setAccount(it) } },
        )
    }
}

private fun digitsToPaise(digits: String): Long {
    if (digits.isEmpty()) return 0
    val rupees = BigDecimal(digits).movePointLeft(2)
    return rupees.setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()
}

private fun quickAddKeypadRows(): List<List<com.dhruv.core.ui.components.KeypadKey>> =
    listOf(
        listOf(
            com.dhruv.core.ui.components.KeypadKey("1"),
            com.dhruv.core.ui.components.KeypadKey("2"),
            com.dhruv.core.ui.components.KeypadKey("3"),
        ),
        listOf(
            com.dhruv.core.ui.components.KeypadKey("4"),
            com.dhruv.core.ui.components.KeypadKey("5"),
            com.dhruv.core.ui.components.KeypadKey("6"),
        ),
        listOf(
            com.dhruv.core.ui.components.KeypadKey("7"),
            com.dhruv.core.ui.components.KeypadKey("8"),
            com.dhruv.core.ui.components.KeypadKey("9"),
        ),
        listOf(
            com.dhruv.core.ui.components.KeypadKey("0"),
            com.dhruv.core.ui.components.KeypadKey("00"),
            com.dhruv.core.ui.components.KeypadKey(
                label = "⌫",
                icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Backspace,
            ),
        ),
    )
