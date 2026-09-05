package com.dhruv.finance.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.format.Paise
import com.dhruv.core.ui.components.DayGroupHeader
import com.dhruv.core.ui.components.EmptyStateCard
import com.dhruv.core.ui.components.LedgerRow
import com.dhruv.core.ui.components.NxFab
import com.dhruv.core.ui.components.RetryErrorCard
import com.dhruv.core.ui.components.SkeletonBlock
import com.dhruv.core.ui.components.StatItem
import com.dhruv.core.ui.components.ThreeUpStatRow
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.finance.data.tracker.model.TransactionType
import org.koin.androidx.compose.koinViewModel

/**
 * D1 (Money tab root) — day-grouped ledger, pinned month summary, FAB into D2 (spec.md Story 1/2).
 * Replaces the tab's former `NotConfiguredCard` placeholder (Phase 0).
 */
@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel,
    onOpenFullForm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showQuickAdd by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        when (val current = state) {
            is LedgerUiState.Loading -> SkeletonBlock(modifier = Modifier.fillMaxSize())
            is LedgerUiState.Error ->
                RetryErrorCard(message = current.message, onRetry = { viewModel.refresh() })
            is LedgerUiState.Loaded -> {
                if (current.dayGroups.isEmpty()) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        EmptyStateCard(message = "Add your first transaction")
                        com.dhruv.core.ui.components.NxButton(
                            text = "Add",
                            onClick = { showQuickAdd = true },
                            modifier = Modifier.padding(top = 12.dp).padding(horizontal = DhruvNextSpacing.screenGutter),
                            block = true,
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        current.summary?.let { summary ->
                            ThreeUpStatRow(
                                modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                                items =
                                    listOf(
                                        StatItem("INCOME", Paise.format(summary.incomePaise, showDecimals = false)),
                                        StatItem("EXPENSE", Paise.format(summary.expensePaise, showDecimals = false)),
                                        StatItem("SAVED", "${summary.savedPercent}%", highlighted = true),
                                    ),
                            )
                        }
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = DhruvNextSpacing.screenGutter),
                            verticalArrangement = Arrangement.Top,
                        ) {
                            current.dayGroups.forEach { group ->
                                item(key = "header-${group.date}") {
                                    DayGroupHeader(
                                        dateLabel = group.label,
                                        netText = Paise.format(group.netPaise),
                                    )
                                }
                                items(group.transactions, key = { it.id }) { txn ->
                                    LedgerRow(
                                        title = txn.payee ?: txn.type.name,
                                        subtitle = txn.note.orEmpty(),
                                        amountPaise = txn.amountPaise,
                                        isPositive = txn.type == TransactionType.INCOME,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        NxFab(
            icon = Icons.Default.Add,
            onClick = { showQuickAdd = true },
            contentDescription = "Add transaction",
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        )
    }

    if (showQuickAdd) {
        val quickAddViewModel: QuickAddViewModel = koinViewModel()
        QuickAddSheet(
            viewModel = quickAddViewModel,
            onDismissRequest = { showQuickAdd = false },
            onSaved = {
                showQuickAdd = false
                viewModel.refresh()
            },
            onMoreOptions = {
                showQuickAdd = false
                onOpenFullForm()
            },
        )
    }
}
