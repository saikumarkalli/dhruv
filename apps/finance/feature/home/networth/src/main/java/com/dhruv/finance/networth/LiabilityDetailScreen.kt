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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.format.Paise
import com.dhruv.core.ui.components.AmortisationDonut
import com.dhruv.core.ui.components.EmptyStateCard
import com.dhruv.core.ui.components.MoneyText
import com.dhruv.core.ui.components.MoneyTextVariant
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxButtonVariant
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.components.NxTextField
import com.dhruv.core.ui.components.NxTopBar
import com.dhruv.core.ui.components.RetryErrorCard
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.components.SkeletonBlock
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import java.util.Locale

/**
 * C7 — liability detail (spec.md Story 4 Scenarios 2-3). [onOpenLoanCalculator] is T032's cross-tab
 * hand-off — this screen never navigates there itself (it has no `NavigationDispatcher`
 * dependency); the caller (`NetWorthNavHost`) owns dispatching `NavTarget.OpenPlanTool(PlanTool.LOAN)`
 * (constitution Article III: cross-feature navigation by id, never a class reference).
 */
@Composable
fun LiabilityDetailScreen(
    viewModel: LiabilityDetailViewModel,
    onBack: () -> Unit,
    onOpenLoanCalculator: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        NxTopBar(title = uiState.holding?.name ?: "Liability", onBack = onBack)

        when {
            uiState.isLoading && uiState.holding == null ->
                Column(modifier = Modifier.padding(DhruvNextSpacing.screenGutter)) {
                    SkeletonBlock(height = 160.dp)
                }
            uiState.errorMessage != null && uiState.holding == null ->
                RetryErrorCard(
                    message = uiState.errorMessage ?: "Couldn't load this liability.",
                    onRetry = { },
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            else -> {
                val holding = uiState.holding
                if (holding == null) {
                    EmptyStateCard(
                        message = "This liability couldn't be found.",
                        modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                    )
                } else {
                    LiabilityDetailContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        onOpenLoanCalculator = onOpenLoanCalculator,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun LiabilityDetailContent(
    viewModel: LiabilityDetailViewModel,
    uiState: LiabilityDetailViewModel.UiState,
    onOpenLoanCalculator: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val meta = uiState.meta
    val split = viewModel.amortisationSplit(uiState)

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        NxCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Outstanding", color = colors.tx3, fontSize = DhruvNextType.meta)
                MoneyText(paise = uiState.outstandingPaise ?: 0L, variant = MoneyTextVariant.Hero)

                if (split != null) {
                    Spacer(Modifier.height(DhruvNextSpacing.interCardGap))
                    AmortisationDonut(
                        principalPaidPaise = split.principalPaidPaise.coerceAtLeast(0L),
                        interestPaidPaise = split.interestPaidPaise.coerceAtLeast(0L),
                        remainingPaise = split.remainingPaise.coerceAtLeast(0L),
                        centerLabel = Paise.formatCompact(split.remainingPaise) + " left",
                    )
                }
            }
        }

        if (meta != null) {
            NxCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionLabel(text = liabilityTypeLabel(meta.liabilityType.name))
                    Spacer(Modifier.height(DhruvNextSpacing.interCardGap))
                    DetailRow(label = "Interest rate", value = "%.2f%% p.a.".format(Locale.US, meta.rateBps / 100.0))
                    meta.emiPaise?.let { DetailRow(label = "Monthly payment", value = Paise.formatCompact(it)) }
                    meta.debitDay?.let { DetailRow(label = "Debit day", value = "$it of every month") }
                    meta.tenureMonths?.let { DetailRow(label = "Tenure", value = "$it months (${meta.paidMonths} paid)") }
                    meta.collateral?.let { DetailRow(label = "Collateral", value = it) }
                }
            }

            NxCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionLabel(text = "Pay extra now")
                    Spacer(Modifier.height(DhruvNextSpacing.interCardGap))
                    NxTextField(
                        value = uiState.extraPaymentText,
                        onValueChange = viewModel::onExtraPaymentChange,
                        label = "Extra payment",
                        prefix = "₹",
                        placeholder = "0",
                        errorMessage = uiState.prepayError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Spacer(Modifier.height(DhruvNextSpacing.inputGroupGap))
                    NxButton(text = "See savings", onClick = viewModel::computePrepay, block = true)

                    uiState.prepayProjection?.let { projection ->
                        Spacer(Modifier.height(DhruvNextSpacing.interCardGap))
                        Text(
                            text = "Estimated — assumes your rate and payment stay the same.",
                            color = colors.tx3,
                            fontSize = DhruvNextType.meta,
                        )
                        Spacer(Modifier.height(4.dp))
                        DetailRow(label = "Interest saved", value = Paise.formatCompact(projection.interestSavedPaise))
                        DetailRow(label = "Paid off", value = "${projection.monthsSaved} months earlier")
                    }

                    Spacer(Modifier.height(DhruvNextSpacing.interCardGap))
                    NxButton(
                        text = "Open in loan calculator",
                        onClick = onOpenLoanCalculator,
                        variant = NxButtonVariant.Outline,
                        block = true,
                    )
                }
            }
        } else {
            EmptyStateCard(message = "No loan details recorded for this liability yet.")
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    val colors = LocalDhruvNextColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = colors.tx3, fontSize = DhruvNextType.body)
        Text(text = value, color = colors.tx, fontSize = DhruvNextType.body)
    }
}
