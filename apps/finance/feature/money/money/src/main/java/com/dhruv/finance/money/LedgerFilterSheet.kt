package com.dhruv.finance.money

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.components.DhruvModalSheet
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxButtonVariant
import com.dhruv.core.ui.components.NxSelect
import com.dhruv.core.ui.components.NxTextField
import com.dhruv.core.ui.components.SegmentedRow
import com.dhruv.core.ui.components.SelectionOption
import com.dhruv.core.ui.components.SelectionSheet
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.tracker.model.TransactionType
import com.dhruv.finance.money.MoneyConfig.transactionTypeLabels

/**
 * D5 (ledger filter sheet, US2) — type/category/amount-range/account, a live "Show N results"
 * count that updates as fields change *before* Apply is tapped (FR-014), Reset. The live count is
 * computed by the caller via [LedgerViewModel.previewCount] against the candidate filter this
 * sheet is building, not yet the committed one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerFilterSheet(
    initialFilter: LedgerFilter,
    categoryOptions: List<SelectionOption>,
    accountOptions: List<SelectionOption>,
    previewCount: (LedgerFilter) -> Int,
    onDismissRequest: () -> Unit,
    onApply: (LedgerFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    var candidate by remember { mutableStateOf(initialFilter) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }

    DhruvModalSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(DhruvNextSpacing.screenGutter)) {
            Text(
                text = "Filter",
                color = colors.tx,
                fontSize = DhruvNextType.title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = DhruvNextSpacing.interCardGap),
            )

            val typeOptions = listOf("All") + TransactionType.entries.map { transactionTypeLabels.getValue(it.name) }
            SegmentedRow(
                options = typeOptions,
                selectedIndex = TransactionType.entries.indexOf(candidate.type) + 1,
                onSelected = { index -> candidate = candidate.copy(type = if (index == 0) null else TransactionType.entries[index - 1]) },
            )

            NxSelect(
                label = "Category",
                value = if (candidate.categoryIds.isEmpty()) "" else "${candidate.categoryIds.size} selected",
                onClick = { showCategoryPicker = true },
                placeholder = "Any",
                modifier = Modifier.fillMaxWidth().padding(top = DhruvNextSpacing.interCardGap),
            )

            NxSelect(
                label = "Account",
                value = accountOptions.firstOrNull { it.id == candidate.accountId }?.label.orEmpty(),
                onClick = { showAccountPicker = true },
                placeholder = "Any",
                modifier = Modifier.fillMaxWidth().padding(top = DhruvNextSpacing.interCardGap),
            )

            Row(modifier = Modifier.padding(top = DhruvNextSpacing.interCardGap)) {
                NxTextField(
                    value = candidate.minPaise?.let { (it / 100).toString() }.orEmpty(),
                    onValueChange = { text -> candidate = candidate.copy(minPaise = text.toLongOrNull()?.times(100)) },
                    label = "Min amount",
                    prefix = "₹",
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                NxTextField(
                    value = candidate.maxPaise?.let { (it / 100).toString() }.orEmpty(),
                    onValueChange = { text -> candidate = candidate.copy(maxPaise = text.toLongOrNull()?.times(100)) },
                    label = "Max amount",
                    prefix = "₹",
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = "Show ${previewCount(candidate)} results",
                color = colors.tx2,
                fontSize = DhruvNextType.meta,
                modifier = Modifier.padding(top = DhruvNextSpacing.interCardGap),
            )

            Row(modifier = Modifier.padding(top = DhruvNextSpacing.sectionGap)) {
                NxButton(
                    text = "Reset",
                    onClick = { candidate = LedgerFilter() },
                    variant = NxButtonVariant.Ghost,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                NxButton(
                    text = "Apply",
                    onClick = {
                        onApply(candidate)
                        onDismissRequest()
                    },
                    block = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showCategoryPicker) {
        SelectionSheet(
            title = "Category",
            options = categoryOptions,
            selectedIds = candidate.categoryIds,
            multiSelect = true,
            onDismissRequest = { showCategoryPicker = false },
            onSelectionChanged = { ids -> candidate = candidate.copy(categoryIds = ids) },
        )
    }
    if (showAccountPicker) {
        SelectionSheet(
            title = "Account",
            options = accountOptions,
            selectedIds = setOfNotNull(candidate.accountId),
            onDismissRequest = { showAccountPicker = false },
            onSelectionChanged = { ids -> candidate = candidate.copy(accountId = ids.firstOrNull()) },
        )
    }
}
