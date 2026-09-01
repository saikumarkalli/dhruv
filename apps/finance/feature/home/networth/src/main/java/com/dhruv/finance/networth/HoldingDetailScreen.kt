package com.dhruv.finance.networth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.ui.components.EmptyStateCard
import com.dhruv.core.ui.components.MoneyText
import com.dhruv.core.ui.components.MoneyTextVariant
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxButtonSize
import com.dhruv.core.ui.components.NxButtonVariant
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.components.NxTopBar
import com.dhruv.core.ui.components.PeriodChipRow
import com.dhruv.core.ui.components.RetryErrorCard
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.components.SkeletonBlock
import com.dhruv.core.ui.components.StatDeltaChip
import com.dhruv.core.ui.components.StatItem
import com.dhruv.core.ui.components.ThreeUpStatRow
import com.dhruv.core.ui.components.TrendSparkline
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.tracker.model.Holding
import com.dhruv.finance.data.tracker.model.ValuationHistoryEntry
import com.dhruv.finance.networth.HoldingDetailViewModel.TrendRange
import java.util.Locale

private val TREND_RANGE_LABELS = listOf("3M", "6M", "1Y", "All")
private val TREND_RANGES = listOf(TrendRange.THREE_MONTHS, TrendRange.SIX_MONTHS, TrendRange.ONE_YEAR, TrendRange.ALL)

/**
 * C3 — holding detail (spec.md Story 2). [onUpdateValue] opens C5 (`AddValuationSheet`) to append
 * an ordinary new value, given the current value to preview the delta against.
 * [onCorrectEntry] opens the same sheet in correction mode for one specific history row (id +
 * its own value) — spec.md Story 3's "realize it's wrong -> add a corrected value" path
 * (NW-BR-002/NW-BR-003). Link-to-goal is left out entirely — goals don't exist in this design-v1
 * phase at all.
 */
@Composable
fun HoldingDetailScreen(
    viewModel: HoldingDetailViewModel,
    onBack: () -> Unit,
    onUpdateValue: (currentValuePaise: Long?) -> Unit,
    onCorrectEntry: (valuationId: String, valuePaise: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        NxTopBar(title = uiState.holding?.name ?: "Holding", onBack = onBack)

        when {
            uiState.isLoading && uiState.holding == null ->
                Column(modifier = Modifier.padding(DhruvNextSpacing.screenGutter)) {
                    SkeletonBlock(height = 160.dp)
                }
            uiState.errorMessage != null && uiState.holding == null ->
                RetryErrorCard(
                    message = uiState.errorMessage ?: "Couldn't load this holding.",
                    onRetry = { },
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            else -> {
                val holding = uiState.holding
                if (holding == null) {
                    EmptyStateCard(
                        message = "This holding couldn't be found.",
                        modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                    )
                } else {
                    HoldingDetailContent(
                        viewModel = viewModel,
                        holding = holding,
                        uiState = uiState,
                        onUpdateValue = onUpdateValue,
                        onCorrectEntry = onCorrectEntry,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun HoldingDetailContent(
    viewModel: HoldingDetailViewModel,
    holding: Holding,
    uiState: HoldingDetailViewModel.UiState,
    onUpdateValue: (Long?) -> Unit,
    onCorrectEntry: (String, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val currentValuePaise = uiState.history.firstOrNull()?.valuation?.valuePaise

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        NxCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                MoneyText(paise = currentValuePaise ?: 0L, variant = MoneyTextVariant.Hero)
                uiState.history.firstOrNull()?.deltaPercentBps?.let { bps ->
                    val deltaPercent = bps / 100.0
                    Spacer(Modifier.height(DhruvNextSpacing.inputGroupGap))
                    StatDeltaChip(
                        text = "%.1f%% since previous value".format(Locale.US, kotlin.math.abs(deltaPercent)),
                        isPositive = deltaPercent >= 0,
                    )
                }

                Spacer(Modifier.height(DhruvNextSpacing.interCardGap))
                PeriodChipRow(
                    options = TREND_RANGE_LABELS,
                    selectedIndex = TREND_RANGES.indexOf(uiState.trendRange).coerceAtLeast(0),
                    onSelected = { index -> viewModel.setTrendRange(TREND_RANGES[index]) },
                )
                Spacer(Modifier.height(DhruvNextSpacing.inputGroupGap))
                TrendSparkline(values = viewModel.trendValuesPaise(uiState).map { it.toFloat() })

                val investedPaise = holding.investedPaise
                if (investedPaise != null && investedPaise > 0 && currentValuePaise != null) {
                    Spacer(Modifier.height(DhruvNextSpacing.interCardGap))
                    val gainPaise = currentValuePaise - investedPaise
                    val simpleReturnPercent = gainPaise * 100.0 / investedPaise
                    ThreeUpStatRow(
                        items =
                            listOf(
                                StatItem(label = "Invested", value = com.dhruv.core.format.Paise.formatCompact(investedPaise)),
                                StatItem(label = "Gain", value = com.dhruv.core.format.Paise.formatCompact(gainPaise)),
                                StatItem(label = "Return", value = "%.1f%%".format(Locale.US, simpleReturnPercent)),
                            ),
                    )
                    Text(
                        text = "Simple return — not annualised (IRR support is a future phase).",
                        color = colors.tx3,
                        fontSize = DhruvNextType.meta,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        NxButton(text = "Update value", onClick = { onUpdateValue(currentValuePaise) }, block = true)

        NxCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionLabel(text = "Valuation history")
                Spacer(Modifier.height(DhruvNextSpacing.interCardGap))
                if (uiState.history.isEmpty()) {
                    EmptyStateCard(message = "No values recorded yet.")
                } else {
                    uiState.history.forEachIndexed { index, entry ->
                        HistoryRow(entry, onCorrect = { onCorrectEntry(entry.valuation.id, entry.valuation.valuePaise) })
                        if (index != uiState.history.lastIndex) {
                            Spacer(Modifier.height(DhruvNextSpacing.inputGroupGap))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: ValuationHistoryEntry,
    onCorrect: () -> Unit,
) {
    val colors = LocalDhruvNextColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = entry.valuation.asOf,
                color = colors.tx,
                fontWeight = FontWeight.Medium,
                fontSize = DhruvNextType.body,
            )
            Text(
                text = entry.valuation.source.name.lowercase(Locale.US).replaceFirstChar { it.uppercase(Locale.US) },
                color = colors.tx3,
                fontSize = DhruvNextType.meta,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            MoneyText(paise = entry.valuation.valuePaise, variant = MoneyTextVariant.Row)
            entry.deltaPercentBps?.let { bps ->
                val deltaPercent = bps / 100.0
                StatDeltaChip(
                    text = "%.1f%%".format(Locale.US, kotlin.math.abs(deltaPercent)),
                    isPositive = deltaPercent >= 0,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            NxButton(
                text = "Fix",
                onClick = onCorrect,
                variant = NxButtonVariant.Ghost,
                size = NxButtonSize.Small,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
