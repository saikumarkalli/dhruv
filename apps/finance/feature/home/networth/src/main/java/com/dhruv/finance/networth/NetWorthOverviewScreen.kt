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
                    message = "Sign in to see your net worth.",
                    actionLabel = "Go to Account",
                    onAction = {},
                    modifier = screenPadding,
                )
            !consentState.syncFinancialRecords ->
                SignedOutCard(
                    message = "Turn on “Sync my financial records” in Settings to use the tracker.",
                    actionLabel = "Go to Settings",
                    onAction = {},
                    modifier = screenPadding,
                )
            uiState.isLoading && uiState.summary == null ->
                Column(modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(DhruvNextSpacing.screenGutter)) {
                    SkeletonBlock(height = 220.dp)
                }
            uiState.errorMessage != null && uiState.summary == null ->
                RetryErrorCard(
                    message = uiState.errorMessage ?: "Couldn't load your net worth.",
                    onRetry = viewModel::load,
                    modifier = screenPadding,
                )
            uiState.summary == null || uiState.summary?.bySector?.isEmpty() == true ->
                EmptyStateCard(
                    message = "Add your first asset or liability to see your net worth.",
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
            contentDescription = "Add holding",
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
                DonutChart(segments = segments) {
                    MoneyText(paise = summary.netPaise, variant = MoneyTextVariant.Hero)
                }
                Spacer(Modifier.height(DhruvNextSpacing.interCardGap))
                ThreeUpStatRow(
                    items =
                        listOf(
                            StatItem(label = "Net", value = Paise.formatCompact(summary.netPaise)),
                            StatItem(label = "Assets", value = Paise.formatCompact(summary.assetsPaise)),
                            StatItem(label = "Liabilities", value = Paise.formatCompact(summary.liabilitiesPaise)),
                        ),
                )
            }
        }

        NxCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionLabel(text = "By sector")
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
            modifier = Modifier.padding(start = 10.dp).weight(1f),
        )
        MoneyText(paise = breakdown.valuePaise, variant = MoneyTextVariant.Inline)
    }
}
