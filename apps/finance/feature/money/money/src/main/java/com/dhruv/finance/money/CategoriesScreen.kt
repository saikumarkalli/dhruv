package com.dhruv.finance.money

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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.ui.components.ConfirmDangerDialog
import com.dhruv.core.ui.components.DisclaimerFooter
import com.dhruv.core.ui.components.EmptyStateCard
import com.dhruv.core.ui.components.ListGroupRow
import com.dhruv.core.ui.components.MoneyText
import com.dhruv.core.ui.components.MoneyTextVariant
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxButtonSize
import com.dhruv.core.ui.components.NxButtonVariant
import com.dhruv.core.ui.components.NxIconButton
import com.dhruv.core.ui.components.NxTextField
import com.dhruv.core.ui.components.RetryErrorCard
import com.dhruv.core.ui.components.SegmentedRow
import com.dhruv.core.ui.components.SelectionOption
import com.dhruv.core.ui.components.SelectionSheet
import com.dhruv.core.ui.components.SkeletonBlock
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

private const val TAB_EXPENSE = 0

/** D8 (categories, US5, spec.md Story 5) — Expense/Income tabs with counts, per-row spend/share,
 * the excluded-from-spend and Uncategorised special renderings, safe rename, and the irreversible
 * merge confirmation (FR-022..FR-026, MNY-UI-007, MNY-BR-003/004). */
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mergePrompt by viewModel.mergePrompt.collectAsStateWithLifecycle()
    val mergeError by viewModel.mergeError.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(TAB_EXPENSE) }
    var renameTarget by remember { mutableStateOf<CategoryRow?>(null) }
    var mergeSource by remember { mutableStateOf<CategoryRow?>(null) }

    val colors = LocalDhruvNextColors.current

    Box(modifier = modifier.fillMaxSize()) {
        when (val current = state) {
            is CategoriesUiState.Loading ->
                Column(modifier = Modifier.padding(DhruvNextSpacing.screenGutter)) {
                    repeat(5) {
                        SkeletonBlock(modifier = Modifier.padding(bottom = DhruvNextSpacing.interCardGap), height = 56.dp)
                    }
                }
            is CategoriesUiState.Error ->
                RetryErrorCard(
                    message = current.message,
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            is CategoriesUiState.Loaded -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    SegmentedRow(
                        options = listOf("Expense (${current.expenseCount})", "Income (${current.incomeCount})"),
                        selectedIndex = selectedTab,
                        onSelected = { selectedTab = it },
                        modifier = Modifier.fillMaxWidth().padding(DhruvNextSpacing.screenGutter),
                    )
                    val rows = if (selectedTab == TAB_EXPENSE) current.expenseRows else current.incomeRows

                    if (rows.isEmpty()) {
                        EmptyStateCard(
                            message = "No categories yet",
                            modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = DhruvNextSpacing.screenGutter),
                        ) {
                            items(rows, key = { it.id }) { row ->
                                CategoryListRow(
                                    row = row,
                                    modifier = Modifier.padding(bottom = DhruvNextSpacing.interCardGap),
                                    onRename = { renameTarget = row },
                                    onToggleExcluded = { viewModel.setExcludedFromSpend(row.id, !row.excludedFromSpend) },
                                    onMergeInto = { mergeSource = row },
                                )
                            }
                        }
                    }
                    DisclaimerFooter(
                        text = "Renaming keeps history. Merging moves every transaction and cannot be undone.",
                        modifier = Modifier.fillMaxWidth().padding(DhruvNextSpacing.screenGutter),
                    )
                    if (mergeError != null) {
                        Text(
                            text = mergeError.orEmpty(),
                            color = colors.neg,
                            fontSize = DhruvNextType.meta,
                            modifier = Modifier.padding(horizontal = DhruvNextSpacing.screenGutter),
                        )
                    }

                    mergeSource?.let { source ->
                        val candidates = rows.filter { it.id != source.id }
                        SelectionSheet(
                            title = "Merge \"${source.name}\" into…",
                            options = candidates.map { SelectionOption(it.id, it.name) },
                            selectedIds = emptySet(),
                            onDismissRequest = { mergeSource = null },
                            onSelectionChanged = { picked ->
                                val targetId = picked.firstOrNull()
                                val target = candidates.firstOrNull { it.id == targetId }
                                mergeSource = null
                                if (target != null) {
                                    viewModel.requestMerge(source.id, source.name, target.id, target.name)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    renameTarget?.let { row ->
        RenameCategoryDialog(
            currentName = row.name,
            onConfirm = { newName ->
                viewModel.rename(row.id, newName)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    when (val prompt = mergePrompt) {
        is MergePrompt.Confirm ->
            ConfirmDangerDialog(
                title = "Merge \"${prompt.sourceName}\" into \"${prompt.targetName}\"?",
                body =
                    "This moves ${prompt.sourceCount} transaction" +
                        (if (prompt.sourceCount == 1) "" else "s") +
                        " from \"${prompt.sourceName}\" into \"${prompt.targetName}\" — which currently has " +
                        "${prompt.targetCount}, for ${prompt.sourceCount + prompt.targetCount} total afterwards. " +
                        "This cannot be undone.",
                confirmLabel = "Merge",
                onConfirm = { viewModel.confirmMerge() },
                onDismiss = { viewModel.dismissMergePrompt() },
            )
        MergePrompt.None -> Unit
    }
}

@Composable
private fun CategoryListRow(
    row: CategoryRow,
    onRename: () -> Unit,
    onToggleExcluded: () -> Unit,
    onMergeInto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    var menuExpanded by remember { mutableStateOf(false) }
    ListGroupRow(
        title = row.name,
        subtitle = row.subtitle,
        icon = Icons.Default.Category,
        showChevron = false,
        modifier = modifier,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    row.spendPaise?.let { MoneyText(paise = it, variant = MoneyTextVariant.Row) }
                    row.sharePercentTenths?.let {
                        Text(text = "${it / 10}.${it % 10}%", color = colors.tx3, fontSize = DhruvNextType.meta)
                    }
                }
                Box {
                    NxIconButton(
                        icon = Icons.Default.MoreVert,
                        onClick = { menuExpanded = true },
                        contentDescription = "More actions for ${row.name}",
                    )
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { menuExpanded = false; onRename() })
                        DropdownMenuItem(
                            text = { Text(if (row.excludedFromSpend) "Include in spend" else "Exclude from spend") },
                            onClick = { menuExpanded = false; onToggleExcluded() },
                        )
                        DropdownMenuItem(text = { Text("Merge into…") }, onClick = { menuExpanded = false; onMergeInto() })
                    }
                }
            }
        },
    )
}

@Composable
private fun RenameCategoryDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename category") },
        text = { NxTextField(value = text, onValueChange = { text = it }, placeholder = "Category name") },
        confirmButton = {
            NxButton(
                text = "Save",
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                size = NxButtonSize.Small,
            )
        },
        dismissButton = {
            NxButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = NxButtonVariant.Ghost,
                size = NxButtonSize.Small,
            )
        },
    )
}
