package com.dhruv.finance.networth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.format.Paise
import com.dhruv.core.ui.components.EmptyStateCard
import com.dhruv.core.ui.components.MoneyText
import com.dhruv.core.ui.components.MoneyTextVariant
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.components.NxTopBar
import com.dhruv.core.ui.components.OfflineStateCard
import com.dhruv.core.ui.components.ProgressRing
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.components.SignedOutCard
import com.dhruv.core.ui.components.SkeletonBlock
import com.dhruv.core.ui.components.StatItem
import com.dhruv.core.ui.components.ThreeUpStatRow
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.tracker.auth.SessionState
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DEBT_FREE_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)

/** C6 — liabilities overview, grouped by type (spec.md Story 4 Scenario 1, FR-008).
 * Signed-out/consent-off gating (NW-UI-005, added Phase 9) mirrors [NetWorthOverviewScreen]'s
 * pattern — this screen had none until found by the Phase 8 QA pass. */
@Composable
fun LiabilitiesScreen(
    viewModel: LiabilitiesViewModel,
    onBack: () -> Unit,
    onOpenLiability: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val consentState by viewModel.consentState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        NxTopBar(title = "Liabilities", onBack = onBack)

        when {
            sessionState !is SessionState.Active ->
                SignedOutCard(
                    message = "Sign in to see your liabilities.",
                    actionLabel = "Go to Account",
                    onAction = {},
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            !consentState.syncFinancialRecords ->
                SignedOutCard(
                    message = "Turn on “Sync my financial records” in Settings to use the tracker.",
                    actionLabel = "Go to Settings",
                    onAction = {},
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            uiState.isLoading && uiState.rows.isEmpty() ->
                Column(modifier = Modifier.padding(DhruvNextSpacing.screenGutter)) {
                    SkeletonBlock(height = 120.dp)
                }
            uiState.errorMessage != null && uiState.rows.isEmpty() ->
                OfflineStateCard(
                    message = uiState.errorMessage ?: "Couldn't load your liabilities.",
                    onRetry = viewModel::load,
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            uiState.rows.isEmpty() ->
                EmptyStateCard(
                    message = "Add a loan, card, or BNPL line to track what you owe.",
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            else ->
                LiabilitiesContent(
                    viewModel = viewModel,
                    uiState = uiState,
                    onOpenLiability = onOpenLiability,
                    modifier = Modifier.fillMaxSize(),
                )
        }
    }
}

@Composable
private fun LiabilitiesContent(
    viewModel: LiabilitiesViewModel,
    uiState: LiabilitiesViewModel.UiState,
    onOpenLiability: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val debtFreeBy = viewModel.debtFreeBy(uiState)
    val grouped = viewModel.groupedByType(uiState)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        item {
            NxCard(modifier = Modifier.fillMaxWidth()) {
                ThreeUpStatRow(
                    items =
                        listOf(
                            StatItem(label = "Outstanding", value = Paise.formatCompact(viewModel.totalOutstandingPaise(uiState))),
                            StatItem(label = "Monthly outgo", value = Paise.formatCompact(viewModel.monthlyOutgoPaise(uiState))),
                            StatItem(
                                label = "Debt-free by",
                                value = debtFreeBy?.format(DEBT_FREE_DATE_FORMAT) ?: "—",
                            ),
                        ),
                )
            }
        }

        grouped.forEach { (type, rows) ->
            item(key = "header-${type?.name ?: "other"}") {
                SectionLabel(text = type?.let { liabilityTypeLabel(it.name) } ?: "Other")
            }
            items(rows, key = { it.holdingWithValue.holding.id }) { row ->
                LiabilityRowCard(
                    row = row,
                    progress = viewModel.payoffProgress(row),
                    onClick = { onOpenLiability(row.holdingWithValue.holding.id) },
                )
            }
        }
    }
}

@Composable
private fun LiabilityRowCard(
    row: LiabilityRow,
    progress: Float?,
    onClick: () -> Unit,
) {
    val colors = LocalDhruvNextColors.current
    NxCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (progress != null) {
                    ProgressRing(progress = progress)
                }
                Column(modifier = Modifier.padding(start = if (progress != null) 12.dp else 0.dp)) {
                    Text(
                        text = row.holdingWithValue.holding.name,
                        color = colors.tx,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = DhruvNextType.cardTitle,
                    )
                    val meta = row.meta
                    if (meta != null) {
                        val rateLabel = "%.2f%% p.a.".format(Locale.US, meta.rateBps / 100.0)
                        val emiLabel = meta.emiPaise?.let { " · ${Paise.formatCompact(it)}/mo" }.orEmpty()
                        Text(
                            text = rateLabel + emiLabel,
                            color = colors.tx3,
                            fontSize = DhruvNextType.meta,
                        )
                    }
                }
            }
            val currentValuePaise = row.holdingWithValue.currentValuePaise
            if (currentValuePaise != null) {
                MoneyText(paise = currentValuePaise, variant = MoneyTextVariant.Row)
            } else {
                Text(text = "—", color = colors.tx3, fontSize = DhruvNextType.cardTitle)
            }
        }
    }
}
