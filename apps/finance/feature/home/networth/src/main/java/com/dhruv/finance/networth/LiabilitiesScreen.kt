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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
        NxTopBar(title = stringResource(R.string.c6_title), onBack = onBack)

        when {
            sessionState !is SessionState.Active ->
                SignedOutCard(
                    message = stringResource(R.string.c6_signed_out_message),
                    actionLabel = stringResource(R.string.networth_action_go_to_account),
                    onAction = {},
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            !consentState.syncFinancialRecords ->
                SignedOutCard(
                    message = stringResource(R.string.networth_consent_off_message),
                    actionLabel = stringResource(R.string.networth_action_go_to_settings),
                    onAction = {},
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            uiState.isLoading && uiState.rows.isEmpty() ->
                Column(modifier = Modifier.padding(DhruvNextSpacing.screenGutter)) {
                    SkeletonBlock(height = 120.dp)
                }
            uiState.errorMessage != null && uiState.rows.isEmpty() ->
                OfflineStateCard(
                    message = uiState.errorMessage ?: stringResource(R.string.c6_error_message),
                    onRetry = viewModel::load,
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            uiState.rows.isEmpty() ->
                EmptyStateCard(
                    message = stringResource(R.string.c6_empty_message),
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
    val otherLabel = stringResource(R.string.c6_other_group_label)

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
                            StatItem(
                                label = stringResource(R.string.c6_stat_outstanding),
                                value = Paise.formatCompact(viewModel.totalOutstandingPaise(uiState)),
                            ),
                            StatItem(
                                label = stringResource(R.string.c6_stat_monthly_outgo),
                                value = Paise.formatCompact(viewModel.monthlyOutgoPaise(uiState)),
                            ),
                            StatItem(
                                label = stringResource(R.string.c6_stat_debt_free_by),
                                value =
                                    debtFreeBy?.format(DEBT_FREE_DATE_FORMAT)
                                        ?: stringResource(R.string.networth_value_placeholder_dash),
                            ),
                        ),
                )
            }
        }

        grouped.forEach { (type, rows) ->
            item(key = "header-${type?.name ?: "other"}") {
                SectionLabel(text = type?.let { liabilityTypeLabel(it.name) } ?: otherLabel)
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
                    // Phase 10, T069: chart contentDescription — previously absent.
                    val progressDescription =
                        stringResource(R.string.c6_payoff_progress_description, (progress * 100).toInt())
                    ProgressRing(
                        progress = progress,
                        modifier = Modifier.size(24.dp).semantics { contentDescription = progressDescription },
                    )
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
                        val rateLabel = stringResource(R.string.c6_rate_format, "%.2f".format(Locale.US, meta.rateBps / 100.0))
                        val emiLabel =
                            meta.emiPaise
                                ?.let {
                                    stringResource(R.string.c6_emi_suffix_format, Paise.formatCompact(it))
                                }.orEmpty()
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
                Text(
                    text = stringResource(R.string.networth_value_placeholder_dash),
                    color = colors.tx3,
                    fontSize = DhruvNextType.cardTitle,
                )
            }
        }
    }
}
