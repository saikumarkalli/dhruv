package com.dhruv.finance.networth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.ui.components.Chip
import com.dhruv.core.ui.components.EmptyStateCard
import com.dhruv.core.ui.components.MoneyText
import com.dhruv.core.ui.components.MoneyTextVariant
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.components.NxTopBar
import com.dhruv.core.ui.components.RetryErrorCard
import com.dhruv.core.ui.components.SkeletonBlock
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.tracker.model.HoldingWithValue
import com.dhruv.finance.data.tracker.model.Sector

/** C2 — sector-grouped assets list with filter chips (spec.md Assumptions: this screen lists
 * ASSET-kind holdings only; liabilities have their own C6 screen, out of this phase's scope). */
@Composable
fun AssetsScreen(
    viewModel: AssetsViewModel,
    onBack: () -> Unit,
    onOpenHolding: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        NxTopBar(title = "Assets", onBack = onBack)

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = DhruvNextSpacing.screenGutter, vertical = DhruvNextSpacing.inputGroupGap),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Chip(
                label = "All",
                selected = uiState.selectedSectorFilter == null,
                onClick = { viewModel.setSectorFilter(null) },
            )
            Sector.entries.forEach { sector ->
                Chip(
                    label = sectorLabel(sector.name),
                    selected = uiState.selectedSectorFilter == sector,
                    onClick = { viewModel.setSectorFilter(sector) },
                )
            }
        }

        when {
            uiState.isLoading && uiState.holdings.isEmpty() ->
                Column(modifier = Modifier.padding(DhruvNextSpacing.screenGutter)) {
                    SkeletonBlock(height = 64.dp)
                }
            uiState.errorMessage != null && uiState.holdings.isEmpty() ->
                RetryErrorCard(
                    message = uiState.errorMessage ?: "Couldn't load your assets.",
                    onRetry = viewModel::load,
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            else -> {
                val filtered =
                    uiState.holdings.filter {
                        uiState.selectedSectorFilter == null || it.holding.sector == uiState.selectedSectorFilter
                    }
                if (filtered.isEmpty()) {
                    EmptyStateCard(
                        message = "No assets in this category yet.",
                        modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(DhruvNextSpacing.screenGutter),
                        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
                    ) {
                        items(filtered, key = { it.holding.id }) { item ->
                            AssetRow(item, onClick = { onOpenHolding(item.holding.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetRow(
    item: HoldingWithValue,
    onClick: () -> Unit,
) {
    val colors = LocalDhruvNextColors.current
    NxCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = item.holding.name,
                    color = colors.tx,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = DhruvNextType.cardTitle,
                )
                Text(
                    text = sectorLabel(item.holding.sector.name),
                    color = colors.tx3,
                    fontSize = DhruvNextType.meta,
                )
            }
            val currentValuePaise = item.currentValuePaise
            if (currentValuePaise != null) {
                MoneyText(paise = currentValuePaise, variant = MoneyTextVariant.Row)
            } else {
                // Cannot actually happen for a holding created via createWithFirstValuation
                // (BR-C2 guarantees one) — defensive fallback only, never a fabricated ₹0.
                Text(text = "—", color = colors.tx3, fontSize = DhruvNextType.cardTitle)
            }
        }
    }
}
