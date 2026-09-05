package com.dhruv.finance.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.format.Paise
import com.dhruv.core.ui.components.DayGroupHeader
import com.dhruv.core.ui.components.EmptyStateCard
import com.dhruv.core.ui.components.LedgerRow
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxIconButton
import com.dhruv.core.ui.components.NxFab
import com.dhruv.core.ui.components.RetryErrorCard
import com.dhruv.core.ui.components.SearchField
import com.dhruv.core.ui.components.SelectionOption
import com.dhruv.core.ui.components.SignedOutCard
import com.dhruv.core.ui.components.SkeletonBlock
import com.dhruv.core.ui.components.StatItem
import com.dhruv.core.ui.components.ThreeUpStatRow
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.tracker.model.TransactionType
import com.dhruv.finance.data.tracker.repo.AccountRepository
import com.dhruv.finance.data.tracker.repo.CategoryRepository
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * D1 (Money tab root) — day-grouped ledger, pinned month summary, month selector, search, filter,
 * FAB into D2 (spec.md Story 1/2). Replaces the tab's former `NotConfiguredCard` placeholder
 * (Phase 0).
 */
@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel,
    onOpenFullForm: () -> Unit,
    modifier: Modifier = Modifier,
    onSignInRequested: () -> Unit = {},
    onOpenTransaction: (String) -> Unit = {},
    onOpenAccounts: () -> Unit = {},
    onOpenCategories: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    var showQuickAdd by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val colors = LocalDhruvNextColors.current

    val accountRepository: AccountRepository = koinInject()
    val categoryRepository: CategoryRepository = koinInject()
    var accountOptions by remember { mutableStateOf<List<SelectionOption>>(emptyList()) }
    var categoryOptions by remember { mutableStateOf<List<SelectionOption>>(emptyList()) }
    LaunchedEffect(Unit) {
        accountRepository.listAccounts().onSuccess { accounts ->
            accountOptions = accounts.map { SelectionOption(it.id, it.name) }
        }
        categoryRepository.listCategories().onSuccess { categories ->
            categoryOptions = categories.map { SelectionOption(it.id, it.name) }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val current = state) {
            is LedgerUiState.Loading -> SkeletonBlock(modifier = Modifier.fillMaxSize())
            is LedgerUiState.Error ->
                RetryErrorCard(message = current.message, onRetry = { viewModel.refresh() })
            is LedgerUiState.SignedOut ->
                SignedOutCard(
                    message = "Sign in to see your ledger",
                    actionLabel = "Sign in",
                    onAction = onSignInRequested,
                )
            is LedgerUiState.Loaded -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    MonthSelector(
                        month = current.month,
                        onPrevious = { viewModel.load(current.month.minusMonths(1)) },
                        onNext = { viewModel.load(current.month.plusMonths(1)) },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = DhruvNextSpacing.screenGutter),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SearchField(
                            query = searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            placeholder = "Search transactions",
                            modifier = Modifier.weight(1f),
                        )
                        NxIconButton(
                            icon = Icons.Default.FilterList,
                            onClick = { showFilterSheet = true },
                            contentDescription = "Filter",
                            tint = if (filter.isEmpty) colors.tx2 else colors.acc,
                        )
                        NxIconButton(
                            icon = Icons.Default.Wallet,
                            onClick = onOpenAccounts,
                            contentDescription = "Accounts",
                        )
                        NxIconButton(
                            icon = Icons.Default.Sell,
                            onClick = onOpenCategories,
                            contentDescription = "Categories",
                        )
                    }
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
                    if (current.dayGroups.isEmpty()) {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                            EmptyStateCard(
                                message =
                                    if (searchQuery.isBlank() && filter.isEmpty) {
                                        "Add your first transaction"
                                    } else {
                                        "No transactions match"
                                    },
                            )
                            if (searchQuery.isBlank() && filter.isEmpty) {
                                NxButton(
                                    text = "Add",
                                    onClick = { showQuickAdd = true },
                                    modifier = Modifier.padding(top = 12.dp).padding(horizontal = DhruvNextSpacing.screenGutter),
                                    block = true,
                                )
                            }
                        }
                    } else {
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
                                        onClick = { onOpenTransaction(txn.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state is LedgerUiState.Loaded) {
            NxFab(
                icon = Icons.Default.Add,
                onClick = { showQuickAdd = true },
                contentDescription = "Add transaction",
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            )
        }
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

    if (showFilterSheet) {
        LedgerFilterSheet(
            initialFilter = filter,
            categoryOptions = categoryOptions,
            accountOptions = accountOptions,
            previewCount = { candidate -> viewModel.previewCount(candidate) },
            onDismissRequest = { showFilterSheet = false },
            onApply = { viewModel.setFilter(it) },
        )
    }
}

@Composable
private fun MonthSelector(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = DhruvNextSpacing.screenGutter, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        NxIconButton(icon = Icons.Default.ChevronLeft, onClick = onPrevious, contentDescription = "Previous month")
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 16.dp)) {
            androidx.compose.material3.Text(
                text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                color = colors.tx,
                fontSize = DhruvNextType.cardTitle,
                fontWeight = FontWeight.Bold,
            )
        }
        NxIconButton(icon = Icons.Default.ChevronRight, onClick = onNext, contentDescription = "Next month")
    }
}
