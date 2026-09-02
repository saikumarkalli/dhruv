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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.ui.components.Chip
import com.dhruv.core.ui.components.EmptyStateCard
import com.dhruv.core.ui.components.MoneyText
import com.dhruv.core.ui.components.MoneyTextVariant
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.components.NxTopBar
import com.dhruv.core.ui.components.OfflineStateCard
import com.dhruv.core.ui.components.RetryErrorCard
import com.dhruv.core.ui.components.SignedOutCard
import com.dhruv.core.ui.components.SkeletonBlock
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.tracker.auth.SessionState
import com.dhruv.finance.data.tracker.model.HoldingWithValue
import com.dhruv.finance.data.tracker.model.Sector

/** C2 — sector-grouped assets list with filter chips (spec.md Assumptions: this screen lists
 * ASSET-kind holdings only; liabilities have their own C6 screen, out of this phase's scope).
 * Signed-out/consent-off gating (NW-UI-005, added Phase 9) mirrors [NetWorthOverviewScreen]'s
 * pattern — this screen had none until found by the Phase 8 QA pass. */
@Composable
fun AssetsScreen(
    viewModel: AssetsViewModel,
    onBack: () -> Unit,
    onOpenHolding: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val consentState by viewModel.consentState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        NxTopBar(title = stringResource(R.string.c2_title), onBack = onBack)

        when {
            sessionState !is SessionState.Active ->
                SignedOutCard(
                    message = stringResource(R.string.c2_signed_out_message),
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
            uiState.isLoading && uiState.holdings.isEmpty() ->
                Column(modifier = Modifier.padding(DhruvNextSpacing.screenGutter)) {
                    SkeletonBlock(height = 64.dp)
                }
            uiState.errorMessage != null && uiState.holdings.isEmpty() ->
                OfflineStateCard(
                    message = uiState.errorMessage ?: stringResource(R.string.c2_error_message),
                    onRetry = viewModel::load,
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            else -> {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = DhruvNextSpacing.screenGutter, vertical = DhruvNextSpacing.inputGroupGap),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Chip(
                        label = stringResource(R.string.c2_filter_all),
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

                val filtered =
                    uiState.holdings.filter {
                        uiState.selectedSectorFilter == null || it.holding.sector == uiState.selectedSectorFilter
                    }
                if (filtered.isEmpty()) {
                    EmptyStateCard(
                        message = stringResource(R.string.c2_empty_message),
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
                Text(
                    text = stringResource(R.string.networth_value_placeholder_dash),
                    color = colors.tx3,
                    fontSize = DhruvNextType.cardTitle,
                )
            }
        }
    }
}
