package com.dhruv.finance.networth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.format.Paise
import com.dhruv.core.ui.components.DonutChart
import com.dhruv.core.ui.components.DonutSegment
import com.dhruv.core.ui.components.EmptyStateCard
import com.dhruv.core.ui.components.MoneyText
import com.dhruv.core.ui.components.MoneyTextVariant
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.components.NxFab
import com.dhruv.core.ui.components.Pill
import com.dhruv.core.ui.components.RetryErrorCard
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.components.SignedOutCard
import com.dhruv.core.ui.components.SkeletonBlock
import com.dhruv.core.ui.components.StatItem
import com.dhruv.core.ui.components.ThreeUpStatRow
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.tracker.auth.SessionState
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.model.NetWorthSummary
import com.dhruv.finance.data.tracker.model.Sector
import com.dhruv.finance.data.tracker.model.SectorBreakdown

/** C1 — net-worth overview (spec.md Story 1's independent test, NW-UI-001). */
@Composable
fun NetWorthOverviewScreen(
    viewModel: NetWorthOverviewViewModel,
    onOpenSector: (HoldingKind, Sector) -> Unit,
    onAddHolding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val consentState by viewModel.consentState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        val screenPadding = Modifier.align(Alignment.Center).padding(DhruvNextSpacing.screenGutter)
        when {
            sessionState !is SessionState.Active ->
                SignedOutCard(
                    message = stringResource(R.string.c1_signed_out_message),
                    actionLabel = stringResource(R.string.networth_action_go_to_account),
                    onAction = {},
                    modifier = screenPadding,
                )
            !consentState.syncFinancialRecords ->
                SignedOutCard(
                    message = stringResource(R.string.networth_consent_off_message),
                    actionLabel = stringResource(R.string.networth_action_go_to_settings),
                    onAction = {},
                    modifier = screenPadding,
                )
            uiState.isLoading && uiState.summary == null ->
                Column(modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(DhruvNextSpacing.screenGutter)) {
                    SkeletonBlock(height = 220.dp)
                }
            uiState.errorMessage != null && uiState.summary == null ->
                RetryErrorCard(
                    message = uiState.errorMessage ?: stringResource(R.string.c1_error_message),
                    onRetry = viewModel::load,
                    modifier = screenPadding,
                )
            uiState.summary == null || uiState.summary?.bySector?.isEmpty() == true ->
                EmptyStateCard(
                    message = stringResource(R.string.c1_empty_message),
                    modifier = screenPadding,
                )
            else ->
                NetWorthOverviewContent(
                    summary = uiState.summary!!,
                    onOpenSector = onOpenSector,
                    modifier = Modifier.fillMaxSize(),
                )
        }

        NxFab(
            icon = Icons.Default.Add,
            onClick = onAddHolding,
            contentDescription = stringResource(R.string.networth_fab_add_holding),
            modifier = Modifier.align(Alignment.BottomEnd).padding(DhruvNextSpacing.screenGutter),
        )
    }
}

@Composable
private fun NetWorthOverviewContent(
    summary: NetWorthSummary,
    onOpenSector: (HoldingKind, Sector) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        NxCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                val segments =
                    summary.bySector.mapIndexed { index, breakdown ->
                        DonutSegment(
                            label = sectorLabel(breakdown.sector.name),
                            value = breakdown.valuePaise.toFloat(),
                            displayValue = "",
                            color = chartColorForIndex(index, colors),
                        )
                    }
                // Phase 10, T069: DESIGN-SYSTEM §9 requires a contentDescription on every chart at
                // the design's stated verbosity — this donut previously had none at all.
                val netWorthDescription =
                    stringResource(R.string.c1_networth_chart_description, Paise.formatCompact(summary.netPaise))
                DonutChart(
                    segments = segments,
                    modifier = Modifier.size(160.dp).semantics { contentDescription = netWorthDescription },
                ) {
                    MoneyText(paise = summary.netPaise, variant = MoneyTextVariant.Hero)
                }
                Spacer(Modifier.height(DhruvNextSpacing.interCardGap))
                ThreeUpStatRow(
                    items =
                        listOf(
                            StatItem(label = stringResource(R.string.c1_stat_net), value = Paise.formatCompact(summary.netPaise)),
                            StatItem(
                                label = stringResource(R.string.c1_stat_assets),
                                value = Paise.formatCompact(summary.assetsPaise),
                            ),
                            StatItem(
                                label = stringResource(R.string.c1_stat_liabilities),
                                value = Paise.formatCompact(summary.liabilitiesPaise),
                            ),
                        ),
                )
            }
        }

        NxCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionLabel(text = stringResource(R.string.c1_by_sector_label))
                Spacer(Modifier.height(DhruvNextSpacing.interCardGap))
                summary.bySector.forEachIndexed { index, breakdown ->
                    SectorRow(
                        breakdown = breakdown,
                        color = chartColorForIndex(index, colors),
                        onClick = { onOpenSector(breakdown.kind, breakdown.sector) },
                    )
                    if (index != summary.bySector.lastIndex) {
                        Spacer(Modifier.height(DhruvNextSpacing.inputGroupGap))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectorRow(
    breakdown: SectorBreakdown,
    color: Color,
    onClick: () -> Unit,
) {
    val colors = LocalDhruvNextColors.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(
            text = sectorLabel(breakdown.sector.name),
            color = colors.tx,
            fontSize = DhruvNextType.body,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 10.dp),
        )
        // Phase 10, T074: the design's C1 legend tags each row with its ASSET/LIABILITY kind —
        // this row previously showed only the sector, ambiguous once both kinds share a screen.
        Pill(
            label =
                stringResource(
                    if (breakdown.kind == HoldingKind.ASSET) {
                        R.string.networth_kind_asset_label
                    } else {
                        R.string.networth_kind_liability_label
                    },
                ),
            modifier = Modifier.padding(start = 8.dp),
        )
        Spacer(Modifier.weight(1f))
        MoneyText(paise = breakdown.valuePaise, variant = MoneyTextVariant.Inline)
    }
}
