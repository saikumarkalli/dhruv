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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
        NxTopBar(title = uiState.holding?.name ?: stringResource(R.string.c7_default_title), onBack = onBack)

        when {
            uiState.isLoading && uiState.holding == null ->
                Column(modifier = Modifier.padding(DhruvNextSpacing.screenGutter)) {
                    SkeletonBlock(height = 160.dp)
                }
            uiState.errorMessage != null && uiState.holding == null ->
                RetryErrorCard(
                    message = uiState.errorMessage ?: stringResource(R.string.c7_error_message),
                    onRetry = { },
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            else -> {
                val holding = uiState.holding
                if (holding == null) {
                    EmptyStateCard(
                        message = stringResource(R.string.c7_not_found_message),
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
                Text(text = stringResource(R.string.c7_outstanding_label), color = colors.tx3, fontSize = DhruvNextType.meta)
                MoneyText(paise = uiState.outstandingPaise ?: 0L, variant = MoneyTextVariant.Hero)

                if (split != null) {
                    Spacer(Modifier.height(DhruvNextSpacing.interCardGap))
                    val remainingSuffix = stringResource(R.string.c7_remaining_suffix)
                    // Phase 10, T069: chart contentDescription — previously absent.
                    val amortisationDescription =
                        stringResource(R.string.c7_amortisation_chart_description, Paise.formatCompact(split.remainingPaise))
                    AmortisationDonut(
                        principalPaidPaise = split.principalPaidPaise.coerceAtLeast(0L),
                        interestPaidPaise = split.interestPaidPaise.coerceAtLeast(0L),
                        remainingPaise = split.remainingPaise.coerceAtLeast(0L),
                        centerLabel = Paise.formatCompact(split.remainingPaise) + remainingSuffix,
                        modifier = Modifier.size(160.dp).semantics { contentDescription = amortisationDescription },
                    )
                }
            }
        }

        if (meta != null) {
            NxCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionLabel(text = liabilityTypeLabel(meta.liabilityType.name))
                    Spacer(Modifier.height(DhruvNextSpacing.interCardGap))
                    DetailRow(
                        label = stringResource(R.string.c7_label_rate),
                        value = stringResource(R.string.c7_rate_format, "%.2f".format(Locale.US, meta.rateBps / 100.0)),
                    )
                    meta.emiPaise?.let {
                        DetailRow(label = stringResource(R.string.c7_label_emi), value = Paise.formatCompact(it))
                    }
                    meta.debitDay?.let {
                        DetailRow(
                            label = stringResource(R.string.c7_label_debit_day),
                            value = stringResource(R.string.c7_debit_day_format, it),
                        )
                    }
                    meta.tenureMonths?.let {
                        DetailRow(
                            label = stringResource(R.string.c7_label_tenure),
                            value = stringResource(R.string.c7_tenure_format, it, meta.paidMonths),
                        )
                    }
                    meta.collateral?.let { DetailRow(label = stringResource(R.string.c7_label_collateral), value = it) }
                }
            }

            NxCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionLabel(text = stringResource(R.string.c7_pay_extra_label))
                    Spacer(Modifier.height(DhruvNextSpacing.interCardGap))
                    NxTextField(
                        value = uiState.extraPaymentText,
                        onValueChange = viewModel::onExtraPaymentChange,
                        label = stringResource(R.string.c7_extra_payment_label),
                        prefix = "₹",
                        placeholder = "0",
                        errorMessage = uiState.prepayError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Spacer(Modifier.height(DhruvNextSpacing.inputGroupGap))
                    NxButton(text = stringResource(R.string.c7_see_savings_button), onClick = viewModel::computePrepay, block = true)

                    uiState.prepayProjection?.let { projection ->
                        Spacer(Modifier.height(DhruvNextSpacing.interCardGap))
                        Text(
                            text = stringResource(R.string.c7_estimate_disclaimer),
                            color = colors.tx3,
                            fontSize = DhruvNextType.meta,
                        )
                        Spacer(Modifier.height(4.dp))
                        DetailRow(
                            label = stringResource(R.string.c7_interest_saved_label),
                            value = Paise.formatCompact(projection.interestSavedPaise),
                        )
                        DetailRow(
                            label = stringResource(R.string.c7_paid_off_label),
                            value = stringResource(R.string.c7_months_earlier_format, projection.monthsSaved),
                        )
                    }

                    Spacer(Modifier.height(DhruvNextSpacing.interCardGap))
                    NxButton(
                        text = stringResource(R.string.c7_open_calculator_button),
                        onClick = onOpenLoanCalculator,
                        variant = NxButtonVariant.Outline,
                        block = true,
                    )
                }
            }
        } else {
            EmptyStateCard(message = stringResource(R.string.c7_no_loan_details_message))
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
