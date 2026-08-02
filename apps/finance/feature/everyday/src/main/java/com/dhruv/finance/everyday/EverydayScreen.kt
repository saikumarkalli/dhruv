package com.dhruv.finance.everyday

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
import androidx.compose.ui.text.style.TextAlign
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
fun EverydayScreen(viewModel: EverydayViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        SegmentedRow(
            options = EverydayTabs,
            selectedIndex = selectedTab,
            onSelected = { selectedTab = it },
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = DhruvNextSpacing.screenGutter,
                vertical = DhruvNextSpacing.interCardGap,
            ),
        )
        when (selectedTab) {
            0 -> SimpleCompoundInterestCalculator(viewModel)
            1 -> DiscountMarkupCalculator(viewModel)
            2 -> TipBillSplitCalculator(viewModel)
            3 -> InflationAdjustedCalculator(viewModel)
        }
    }
}

// -------------------------------------------------------------
// SIMPLE & COMPOUND INTEREST CALCULATOR (moved verbatim from FinanceScreen.kt)
// -------------------------------------------------------------
@Composable
fun SimpleCompoundInterestCalculator(viewModel: EverydayViewModel) {
    var principalInput by remember { mutableStateOf("100000") }
    var rateInput by remember { mutableStateOf("7.5") }
    var yearsInput by remember { mutableStateOf("5") }
    var freqIndex by remember { mutableIntStateOf(0) }
    val compoundFrequency = CompoundFrequencies[freqIndex].second

    val colors = LocalDhruvNextColors.current
    val principal = principalInput.toDoubleOrNull() ?: 0.0
    val rate = rateInput.toDoubleOrNull() ?: 0.0
    val years = yearsInput.toDoubleOrNull() ?: 0.0

    val scResult =
        remember(principal, rate, years, compoundFrequency) {
            viewModel.calculateSimpleCompound(principal, rate, years, compoundFrequency)
        }

    val simpleInterest = scResult.simpleInterest
    val simpleTotal = scResult.simpleTotal
    val compoundInterest = scResult.compoundInterest
    val compoundTotal = scResult.compoundTotal

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        SectionLabel(text = "Interest Compounding Estimations")

        NxCard {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                NxTextField(
                    value = principalInput,
                    onValueChange = { principalInput = it },
                    label = "Principal Sum (₹)",
                    prefix = "₹",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                NxTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = "Annual Rate of Interest (%)",
                    suffix = "% p.a.",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                NxTextField(
                    value = yearsInput,
                    onValueChange = { yearsInput = it },
                    label = "Tenure Time (Years)",
                    suffix = "Yrs",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    "Compounding Interval Frequency",
                    fontSize = DhruvNextType.meta,
                    fontWeight = FontWeight.Medium,
                    color = colors.tx3,
                )
                SegmentedRow(
                    options = CompoundFrequencies.map { it.first },
                    selectedIndex = freqIndex,
                    onSelected = { freqIndex = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        NxCard {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                Text(
                    "Yield Performance side-by-side",
                    fontSize = DhruvNextType.meta,
                    fontWeight = FontWeight.Medium,
                    color = colors.tx3,
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Simple Yield Interest", fontSize = DhruvNextType.meta, color = colors.tx3)
                        Text(formatCurrency(simpleInterest), fontWeight = FontWeight.Bold, fontSize = DhruvNextType.body, color = colors.tx)
                        Text("Total: " + formatCurrency(simpleTotal), fontSize = DhruvNextType.meta, color = colors.tx2)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Compounded Interest", fontSize = DhruvNextType.meta, color = colors.pos)
                        Text(formatCurrency(compoundInterest), fontWeight = FontWeight.Bold, fontSize = DhruvNextType.body, color = colors.pos)
                        Text("Total: " + formatCurrency(compoundTotal), fontSize = DhruvNextType.meta, color = colors.tx2)
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = colors.line)

                val excessGains = compoundInterest.subtract(simpleInterest).coerceAtLeast(BigDecimal.ZERO)
                Text(
                    text = "Compounding advantage payout difference yields: " + formatCurrency(excessGains),
                    fontSize = DhruvNextType.meta,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.acc,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// -------------------------------------------------------------
// DISCOUNT & MARKUP CALCULATOR (moved verbatim from FinanceScreen.kt)
// -------------------------------------------------------------
@Composable
fun DiscountMarkupCalculator(viewModel: EverydayViewModel) {
    var amountInput by remember { mutableStateOf("1000") }
    var percentInput by remember { mutableStateOf("20") }
    var modeIndex by remember { mutableIntStateOf(0) }
    val isDiscountMode = modeIndex == 0

    val colors = LocalDhruvNextColors.current
    val base = amountInput.toDoubleOrNull() ?: 0.0
    val pct = percentInput.toDoubleOrNull() ?: 0.0

    val dmResult =
        remember(base, pct, isDiscountMode) {
            viewModel.calculateDiscountMarkup(base, pct, isDiscountMode)
        }

    val offset = dmResult.offset
    val finalVal = dmResult.finalVal

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        SectionLabel(text = "Product Pricing Adjuster")

        SegmentedRow(
            options = DiscountModes,
            selectedIndex = modeIndex,
            onSelected = { modeIndex = it },
            modifier = Modifier.fillMaxWidth(),
        )

        NxCard {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                NxTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = if (isDiscountMode) "List Price Base (₹)" else "Cost Pricing Base (₹)",
                    prefix = "₹",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                NxTextField(
                    value = percentInput,
                    onValueChange = { percentInput = it },
                    label = if (isDiscountMode) "Discount %" else "Profit Premium Markup %",
                    suffix = "%",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        NxCard {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (isDiscountMode) "Total retail savings offset:" else "Added wholesale markup value:",
                        fontSize = DhruvNextType.meta,
                        color = colors.tx2,
                    )
                    Text(
                        formatCurrency(offset),
                        fontWeight = FontWeight.Bold,
                        fontSize = DhruvNextType.body,
                        color = if (isDiscountMode) colors.pos else colors.neg,
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = colors.line)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Target Final Price Item tag:", fontWeight = FontWeight.Bold, fontSize = DhruvNextType.body, color = colors.tx)
                    Text(
                        formatCurrency(finalVal),
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
// TIP & BILL SPLIT CALCULATOR (moved verbatim from FinanceScreen.kt)
// -------------------------------------------------------------
@Composable
fun TipBillSplitCalculator(viewModel: EverydayViewModel) {
    var billInput by remember { mutableStateOf("1500") }
    var tipInput by remember { mutableStateOf("12") }
    var peopleInput by remember { mutableStateOf("4") }

    val colors = LocalDhruvNextColors.current
    val bill = billInput.toDoubleOrNull() ?: 0.0
    val tipPercent = tipInput.toDoubleOrNull() ?: 10.0
    val totalPeople = peopleInput.toIntOrNull() ?: 1

    val tipResult =
        remember(bill, tipPercent, totalPeople) {
            viewModel.calculateTipSplit(bill, tipPercent, totalPeople)
        }

    val totalTip = tipResult.totalTip
    val overallTotal = tipResult.overallTotal
    val splitTip = tipResult.splitTip
    val splitBill = tipResult.splitBill

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        SectionLabel(text = "Dining Apportionment Clocks")

        NxCard {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                NxTextField(
                    value = billInput,
                    onValueChange = { billInput = it },
                    label = "Base dining subtotal (₹)",
                    prefix = "₹",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                NxTextField(
                    value = tipInput,
                    onValueChange = { tipInput = it },
                    label = "Gratuity Tip level (%)",
                    suffix = "%",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                NxTextField(
                    value = peopleInput,
                    onValueChange = { peopleInput = it },
                    label = "Active bill split headcount",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        NxCard {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total accumulated gratuity:", fontSize = DhruvNextType.meta, color = colors.tx3)
                        Text(formatCurrency(totalTip), fontWeight = FontWeight.Bold, fontSize = DhruvNextType.body, color = colors.tx)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Gross Bill Total cost:", fontSize = DhruvNextType.meta, color = colors.tx3)
                        Text(formatCurrency(overallTotal), fontWeight = FontWeight.Bold, fontSize = DhruvNextType.body, color = colors.tx)
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = colors.line)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Tip per seat share:", fontSize = DhruvNextType.meta, color = colors.tx3)
                        Text(formatCurrency(splitTip), fontWeight = FontWeight.Bold, fontSize = DhruvNextType.body, color = colors.pos)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Cost sum share per seat:", fontSize = DhruvNextType.meta, color = colors.tx3)
                        Text(
                            formatCurrency(splitBill),
                            fontWeight = FontWeight.Black,
                            fontSize = DhruvNextType.title,
                            color = colors.acc,
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// INFLATION ADJUSTED VALUE CALCULATOR (moved verbatim from FinanceScreen.kt)
// -------------------------------------------------------------
@Composable
fun InflationAdjustedCalculator(viewModel: EverydayViewModel) {
    var amountInput by remember { mutableStateOf("10000") }
    var inflationRateInput by remember { mutableStateOf("6") }
    var yearsInput by remember { mutableStateOf("15") }

    val colors = LocalDhruvNextColors.current
    val amount = amountInput.toDoubleOrNull() ?: 0.0
    val rate = inflationRateInput.toDoubleOrNull() ?: 0.0
    val years = yearsInput.toDoubleOrNull() ?: 0.0

    val inflationResult =
        remember(amount, rate, years) {
            viewModel.calculateInflation(amount, rate, years)
        }

    val futurePurchasePower = inflationResult.futurePurchasePower
    val amountNeeded = inflationResult.amountNeeded

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        SectionLabel(text = "Inflationary Purchase Power Trackers")

        NxCard {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                NxTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = "Base valuation asset (₹)",
                    prefix = "₹",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                NxTextField(
                    value = inflationRateInput,
                    onValueChange = { inflationRateInput = it },
                    label = "Avg annual Inflation multiplier (%)",
                    suffix = "%",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                NxTextField(
                    value = yearsInput,
                    onValueChange = { yearsInput = it },
                    label = "Expected Time Span timeline (Years)",
                    suffix = "Yrs",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        NxCard {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                Column {
                    Text("Future real value (Purchasing Power):", fontSize = DhruvNextType.meta, color = colors.tx3)
                    Text(
                        formatCurrency(futurePurchasePower),
                        fontWeight = FontWeight.Black,
                        fontSize = DhruvNextType.hero,
                        color = colors.acc,
                    )
                    Text("Value of " + formatCurrency(amount) + " today in ${years.toInt()} years", fontSize = DhruvNextType.meta, color = colors.tx2)
                }

                HorizontalDivider(thickness = 0.5.dp, color = colors.line)

                Column {
                    Text(
                        "Amount needed to match purchase power in ${years.toInt()} years:",
                        fontSize = DhruvNextType.meta,
                        color = colors.tx3,
                    )
                    Text(formatCurrency(amountNeeded), fontWeight = FontWeight.Bold, fontSize = DhruvNextType.title, color = colors.neg)
                }
            }
        }
    }
}

private fun formatCurrency(value: Double): String = CurrencyFormatter.format(value)

private fun formatCurrency(value: BigDecimal): String = CurrencyFormatter.format(value)
