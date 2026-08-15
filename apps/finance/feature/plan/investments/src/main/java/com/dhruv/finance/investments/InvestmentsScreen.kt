package com.dhruv.finance.investments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.components.NxTextField
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.components.SegmentedRow
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.util.CurrencyFormatter
import java.math.BigDecimal

@Composable
fun InvestmentsScreen(viewModel: InvestmentsViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        SegmentedRow(
            options = InvestmentTabs,
            selectedIndex = selectedTab,
            onSelected = { selectedTab = it },
            modifier =
                Modifier.fillMaxWidth().padding(
                    horizontal = DhruvNextSpacing.screenGutter,
                    vertical = DhruvNextSpacing.interCardGap,
                ),
        )
        when (selectedTab) {
            0 -> SipCalculatorRedesign(viewModel)
            1 -> RoiCagrCalculator(viewModel)
            2 -> FdRdBatCalculator(viewModel)
        }
    }
}

// -------------------------------------------------------------
// SIP CALCULATOR REDESIGN
// (moved verbatim from FinanceScreen.kt, param type updated)
// -------------------------------------------------------------
@Composable
fun SipCalculatorRedesign(viewModel: InvestmentsViewModel) {
    var amountInput by remember { mutableStateOf("5000") }
    var returnInput by remember { mutableStateOf("12") }
    var yearsInput by remember { mutableStateOf("10") }

    val colors = LocalDhruvNextColors.current
    val amount = amountInput.toDoubleOrNull() ?: 0.0
    val expectedReturn = returnInput.toDoubleOrNull() ?: 0.0
    val years = yearsInput.toDoubleOrNull() ?: 0.0

    val sipResult =
        remember(amount, expectedReturn, years) {
            viewModel.calculateSip(amount, expectedReturn, years)
        }

    val totalInvested = sipResult.totalInvested
    val futureValue = sipResult.futureValue
    val estimatedReturns = sipResult.estimatedReturns

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        SectionLabel(text = "Systematic Wealth Builder")

        NxCard {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                NxTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = "Monthly Contribution (₹)",
                    prefix = "₹",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                NxTextField(
                    value = returnInput,
                    onValueChange = { returnInput = it },
                    label = "Expected Return Speed (% p.a.)",
                    suffix = "%",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                NxTextField(
                    value = yearsInput,
                    onValueChange = { yearsInput = it },
                    label = "Duration Timeline (Years)",
                    suffix = "Yrs",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        NxCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Principal Outlay", fontSize = DhruvNextType.meta, color = colors.tx3)
                        Text(formatCurrency(totalInvested), fontWeight = FontWeight.Bold, fontSize = DhruvNextType.body, color = colors.tx)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Estimated Yields", fontSize = DhruvNextType.meta, color = colors.pos)
                        Text(
                            formatCurrency(estimatedReturns),
                            fontWeight = FontWeight.Bold,
                            fontSize = DhruvNextType.body,
                            color = colors.pos,
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = colors.line)

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Total Ultimate Future Welth", fontSize = DhruvNextType.meta, color = colors.tx3)
                    Text(
                        formatCurrency(futureValue),
                        fontWeight = FontWeight.Black,
                        fontSize = DhruvNextType.title,
                        color = colors.acc,
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ROI / CAGR CALCULATOR
// (moved verbatim from FinanceScreen.kt, param type updated)
// -------------------------------------------------------------
@Composable
fun RoiCagrCalculator(viewModel: InvestmentsViewModel) {
    var initialInput by remember { mutableStateOf("50000") }
    var finalInput by remember { mutableStateOf("95000") }
    var durationInput by remember { mutableStateOf("4") }

    val colors = LocalDhruvNextColors.current
    val initial = initialInput.toDoubleOrNull() ?: 0.0
    val finalVal = finalInput.toDoubleOrNull() ?: 0.0
    val duration = durationInput.toDoubleOrNull() ?: 0.0

    val roiCagrResult =
        remember(initial, finalVal, duration) {
            viewModel.calculateRoiCagr(initial, finalVal, duration)
        }
    val absoluteReturn = roiCagrResult.absoluteReturn
    val cagr = roiCagrResult.cagr

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        SectionLabel(text = "Yield Performance Indices (ROI / CAGR)")

        NxCard {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                NxTextField(
                    value = initialInput,
                    onValueChange = { initialInput = it },
                    label = "Initial Investment Principal (₹)",
                    prefix = "₹",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                NxTextField(
                    value = finalInput,
                    onValueChange = { finalInput = it },
                    label = "Final Maturity Asset Value (₹)",
                    prefix = "₹",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                NxTextField(
                    value = durationInput,
                    onValueChange = { durationInput = it },
                    label = "Total Duration Span (Years)",
                    suffix = "Yrs",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        NxCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Absolute Return Ratio", fontSize = DhruvNextType.meta, color = colors.tx3)
                    Text(
                        "${"%.2f".format(absoluteReturn)}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = DhruvNextType.title,
                        color = colors.acc,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Compounded Annualized CAGR", fontSize = DhruvNextType.meta, color = colors.pos)
                    Text("${"%.2f".format(cagr)}%", fontWeight = FontWeight.Bold, fontSize = DhruvNextType.title, color = colors.pos)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// FD / RD MATURITY CALCULATOR
// (moved verbatim from FinanceScreen.kt, param type updated)
// -------------------------------------------------------------
@Composable
fun FdRdBatCalculator(viewModel: InvestmentsViewModel) {
    var isFixedDeposit by remember { mutableStateOf(true) }
    var investAmountInput by remember { mutableStateOf("100000") }
    var interestRateInput by remember { mutableStateOf("7.1") }
    var tenureInput by remember { mutableStateOf("5") }

    val colors = LocalDhruvNextColors.current
    val amount = investAmountInput.toDoubleOrNull() ?: 0.0
    val rate = interestRateInput.toDoubleOrNull() ?: 0.0
    val years = tenureInput.toDoubleOrNull() ?: 0.0

    val fdRdResult =
        remember(amount, rate, years, isFixedDeposit) {
            viewModel.calculateFdRd(amount, rate, years, isFixedDeposit)
        }

    val principalInvested = fdRdResult.principalInvested
    val totalInterestAccrued = fdRdResult.interestGains
    val maturityAccumulated = fdRdResult.maturityValue

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        SectionLabel(text = "FD / RD Sovereign Maturity Engines")

        SegmentedRow(
            options = FdRdOptions,
            selectedIndex = if (isFixedDeposit) 0 else 1,
            onSelected = { isFixedDeposit = it == 0 },
            modifier = Modifier.fillMaxWidth(),
        )

        NxCard {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                NxTextField(
                    value = investAmountInput,
                    onValueChange = { investAmountInput = it },
                    label = if (isFixedDeposit) "Lumpsum Deposit Principal (₹)" else "Monthly Deposit Contribution (₹)",
                    prefix = "₹",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                NxTextField(
                    value = interestRateInput,
                    onValueChange = { interestRateInput = it },
                    label = "Aggressive Interest Rate (% p.a.)",
                    suffix = "%",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                NxTextField(
                    value = tenureInput,
                    onValueChange = { tenureInput = it },
                    label = "Duration Period Timeline (Years)",
                    suffix = "Yrs",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        NxCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Accumulated Invested Base:", fontSize = DhruvNextType.meta, color = colors.tx3)
                        Text(
                            formatCurrency(principalInvested),
                            fontWeight = FontWeight.Bold,
                            fontSize = DhruvNextType.body,
                            color = colors.tx,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Accrued Interest Gains:", fontSize = DhruvNextType.meta, color = colors.pos)
                        Text(
                            formatCurrency(totalInterestAccrued),
                            fontWeight = FontWeight.Bold,
                            fontSize = DhruvNextType.body,
                            color = colors.pos,
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = colors.line)

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Maturity Pay Out Yield sum:", fontSize = DhruvNextType.meta, color = colors.tx3)
                    Text(
                        formatCurrency(maturityAccumulated),
                        fontWeight = FontWeight.Black,
                        fontSize = DhruvNextType.title,
                        color = colors.acc,
                    )
                }
            }
        }
    }
}

private fun formatCurrency(value: Double): String = CurrencyFormatter.format(value)

private fun formatCurrency(value: BigDecimal): String = CurrencyFormatter.format(value)
