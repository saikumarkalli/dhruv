package com.dhruv.finance.investments

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.finance.data.util.CurrencyFormatter
import java.math.BigDecimal

@Composable
fun InvestmentsScreen(viewModel: InvestmentsViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("SIP Growth", "ROI / CAGR", "FD / RD")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }
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
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Systematic Wealth Builder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Monthly Contribution (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = returnInput,
                    onValueChange = { returnInput = it },
                    label = { Text("Expected Return Speed (% p.a.)") },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = yearsInput,
                    onValueChange = { yearsInput = it },
                    label = { Text("Duration Timeline (Years)") },
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
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Principal Outlay", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(formatCurrency(totalInvested), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Estimated Yields", fontSize = 11.sp, color = Color(0xFF4CAF50))
                        Text(formatCurrency(estimatedReturns), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF4CAF50))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Total Ultimate Future Welth", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        formatCurrency(futureValue),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary,
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
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Yield Performance Indices (ROI / CAGR)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = initialInput,
                    onValueChange = { initialInput = it },
                    label = { Text("Initial Investment Principal (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = finalInput,
                    onValueChange = { finalInput = it },
                    label = { Text("Final Maturity Asset Value (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = durationInput,
                    onValueChange = { durationInput = it },
                    label = { Text("Total Duration Span (Years)") },
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Absolute Return Ratio", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(
                            "${"%.2f".format(absoluteReturn)}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Compounded Annualized CAGR", fontSize = 11.sp, color = Color(0xFF4CAF50))
                        Text("${"%.2f".format(cagr)}%", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF4CAF50))
                    }
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
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("FD / RD Sovereign Maturity Engines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

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
                        .background(if (isFixedDeposit) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { isFixedDeposit = true }
                        .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Fixed Deposit (FD)",
                    color = if (isFixedDeposit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isFixedDeposit) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { isFixedDeposit = false }
                        .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Recurring (RD)",
                    color = if (!isFixedDeposit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
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
                    value = investAmountInput,
                    onValueChange = { investAmountInput = it },
                    label = { Text(if (isFixedDeposit) "Lumpsum Deposit Principal (₹)" else "Monthly Deposit Contribution (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = interestRateInput,
                    onValueChange = { interestRateInput = it },
                    label = { Text("Aggressive Interest Rate (% p.a.)") },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = tenureInput,
                    onValueChange = { tenureInput = it },
                    label = { Text("Duration Period Timeline (Years)") },
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
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Accumulated Invested Base:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(formatCurrency(principalInvested), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Accrued Interest Gains:", fontSize = 11.sp, color = Color(0xFF4CAF50))
                        Text(
                            formatCurrency(totalInterestAccrued),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF4CAF50),
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Maturity Pay Out Yield sum:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        formatCurrency(maturityAccumulated),
                        fontWeight = FontWeight.Black,
                        fontSize = 21.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun formatCurrency(value: Double): String = CurrencyFormatter.format(value)

private fun formatCurrency(value: BigDecimal): String = CurrencyFormatter.format(value)
