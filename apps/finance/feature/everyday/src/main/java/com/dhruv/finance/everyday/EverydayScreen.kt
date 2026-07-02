package com.dhruv.finance.everyday

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.finance.data.util.CurrencyFormatter
import java.math.BigDecimal

@Composable
fun EverydayScreen(viewModel: EverydayViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Interest", "Discount", "Tip Split", "Inflation")

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }
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
    var compoundFrequency by remember { mutableIntStateOf(1) }

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
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Interest Compounding Estimations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = principalInput,
                    onValueChange = { principalInput = it },
                    label = { Text("Principal Sum (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text("Annual Rate of Interest (%)") },
                    suffix = { Text("% p.a.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = yearsInput,
                    onValueChange = { yearsInput = it },
                    label = { Text("Tenure Time (Years)") },
                    suffix = { Text("Yrs") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                Text(
                    "Compounding Interval Frequency",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(4.dp),
                ) {
                    val frequencies = listOf("Annual", "Quarterly", "Monthly")
                    val freqVals = listOf(1, 4, 12)
                    frequencies.forEachIndexed { idx, label ->
                        val isSel = freqVals[idx] == compoundFrequency
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { compoundFrequency = freqVals[idx] }
                                    .padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Yield Performance side-by-side",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Simple Yield Interest", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(formatCurrency(simpleInterest), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Total: " + formatCurrency(simpleTotal), fontSize = 11.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Compounded Interest", fontSize = 11.sp, color = Color(0xFF4CAF50))
                        Text(formatCurrency(compoundInterest), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF4CAF50))
                        Text("Total: " + formatCurrency(compoundTotal), fontSize = 11.sp)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                val excessGains = compoundInterest.subtract(simpleInterest).coerceAtLeast(BigDecimal.ZERO)
                Text(
                    text = "Compounding advantage payout difference yields: " + formatCurrency(excessGains),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
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
    var isDiscountMode by remember { mutableStateOf(true) }

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
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Product Pricing Adjuster", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDiscountMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { isDiscountMode = true }
                        .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Discount mode",
                    color = if (isDiscountMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isDiscountMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { isDiscountMode = false }
                        .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Markup mode",
                    color = if (!isDiscountMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text(if (isDiscountMode) "List Price Base (₹)" else "Cost Pricing Base (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = percentInput,
                    onValueChange = { percentInput = it },
                    label = { Text(if (isDiscountMode) "Discount %" else "Profit Premium Markup %") },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (isDiscountMode) "Total retail savings offset:" else "Added wholesale markup value:", fontSize = 12.sp)
                    Text(
                        formatCurrency(offset),
                        fontWeight = FontWeight.Bold,
                        color = if (isDiscountMode) Color(0xFF4CAF50) else Color(0xFFFF5252),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Target Final Price Item tag:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        formatCurrency(finalVal),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
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
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Dining Apportionment Clocks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = billInput,
                    onValueChange = { billInput = it },
                    label = { Text("Base dining subtotal (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = tipInput,
                    onValueChange = { tipInput = it },
                    label = { Text("Gratuity Tip level (%)") },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = peopleInput,
                    onValueChange = { peopleInput = it },
                    label = { Text("Active bill split headcount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total accumulated gratuity:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(formatCurrency(totalTip), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Gross Bill Total cost:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(formatCurrency(overallTotal), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Tip per seat share:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(formatCurrency(splitTip), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF4CAF50))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Cost sum share per seat:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(
                            formatCurrency(splitBill),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary,
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
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Inflationary Purchase Power Trackers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Base valuation asset (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = inflationRateInput,
                    onValueChange = { inflationRateInput = it },
                    label = { Text("Avg annual Inflation multiplier (%)") },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = yearsInput,
                    onValueChange = { yearsInput = it },
                    label = { Text("Expected Time Span timeline (Years)") },
                    suffix = { Text("Yrs") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text("Future real value (Purchasing Power):", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        formatCurrency(futurePurchasePower),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text("Value of " + formatCurrency(amount) + " today in ${years.toInt()} years", fontSize = 11.sp)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))

                Column {
                    Text(
                        "Amount needed to match purchase power in ${years.toInt()} years:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(formatCurrency(amountNeeded), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFFF5252))
                }
            }
        }
    }
}

private fun formatCurrency(value: Double): String = CurrencyFormatter.format(value)

private fun formatCurrency(value: BigDecimal): String = CurrencyFormatter.format(value)
