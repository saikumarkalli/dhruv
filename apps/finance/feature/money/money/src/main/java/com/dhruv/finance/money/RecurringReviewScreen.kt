package com.dhruv.finance.money

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.format.Paise
import com.dhruv.core.ui.components.EmptyStateCard
import com.dhruv.core.ui.components.RetryErrorCard
import com.dhruv.core.ui.components.SkeletonBlock
import com.dhruv.core.ui.components.SuggestedRow
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.finance.data.tracker.repo.RecurringTemplateKeys

/** D9-review — the recurring-only review queue (spec.md Story 6 Assumptions: the shared SMS/AA
 * queue is Phase 7). Accept writes the transaction (FR-029); Dismiss writes nothing. */
@Composable
fun RecurringReviewScreen(
    viewModel: RecurringReviewViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val current = state) {
        is RecurringReviewUiState.Loading -> SkeletonBlock(modifier = modifier.fillMaxSize())
        is RecurringReviewUiState.Error -> RetryErrorCard(message = current.message, onRetry = { viewModel.load() })
        is RecurringReviewUiState.Loaded ->
            if (current.pending.isEmpty()) {
                EmptyStateCard(message = "Nothing to review", modifier = modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(DhruvNextSpacing.screenGutter),
                ) {
                    items(current.pending, key = { it.id }) { entry ->
                        val amountPaise = (entry.parsed[RecurringTemplateKeys.AMOUNT_PAISE] as? Number)?.toLong() ?: 0L
                        val payee = entry.parsed[RecurringTemplateKeys.PAYEE] as? String ?: "Recurring entry"
                        SuggestedRow(
                            title = payee,
                            subtitle = "Due ${entry.dueOn}",
                            amountText = Paise.format(amountPaise),
                            onAccept = { viewModel.accept(entry) },
                            onDismiss = { viewModel.dismiss(entry) },
                            modifier = Modifier.padding(bottom = DhruvNextSpacing.interCardGap),
                        )
                    }
                }
            }
    }
}
