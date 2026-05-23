package com.example.ui.finance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.pow
import com.example.ui.theme.*

@Composable
fun FinanceScreen(
    modifier: Modifier = Modifier
) {
    var activeFinanceCalc by remember { mutableIntStateOf(0) }
    
    val calculators = listOf(
        FinanceCalcItem("Loan EMI", Icons.Default.Assessment, "Evaluate monthly home/car EMI and interest ratios."),
        FinanceCalcItem("Simple & Compound", Icons.Default.Percent, "Compare generic interest yields across compounding periods."),
        FinanceCalcItem("SIP Growth", Icons.Default.TrendingUp, "Track future wealth growth of monthly mutual fund plans."),
        FinanceCalcItem("ROI / CAGR", Icons.Default.MonetizationOn, "Determine exact return ratios and annual growth speeds."),
        FinanceCalcItem("GST / Tax", Icons.Default.Receipt, "Calculate net taxes, gross totals, and net billing items."),
        FinanceCalcItem("Discount & Markup", Icons.Default.ShoppingBag, "Compute discount savings margins or wholesale pricing Markups."),
        FinanceCalcItem("Tip & Bill Split", Icons.Default.RoomService, "Apportion gratuities and divide billing tab cleanly among friends."),
        FinanceCalcItem("Salary Breakup", Icons.Default.Payments, "Review annual CTC packages down to estimated monthly take-home pays."),
        FinanceCalcItem("Inflation Adjusted", Icons.Default.TrendingDown, "See past or future purchasing power adjustments of savings."),
        FinanceCalcItem("FD / RD Maturity", Icons.Default.Savings, "Track Fixed/Recurring Deposits maturity pay outs and accruals.")
    )

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 720

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Applet Header - STACKED COLUMN TO PREVENT ANY TEXT OVERLAPPING
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Finance Planning",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = ResponsiveApp.typography.titleLarge),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Real-Time Calculations • May 2026",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveApp.typography.labelSmall),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (isWideScreen) {
            // Adaptive Grid/Detail for large devices (Tablet/Desktop/Landscape)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Panel: Menu Cards
                Card(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Financial Library",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                        )
                        calculators.forEachIndexed { index, item ->
                            val isSelected = activeFinanceCalc == index
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { activeFinanceCalc = index }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Right Panel: Calculations Display Space
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        ActiveFinanceCalcRender(activeFinanceCalc)
                    }
                }
            }
        } else {
            // Mobile scrolling view
            var showSelectionDialog by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Floating Action Selection Trigger Bar
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSelectionDialog = true }
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = calculators[activeFinanceCalc].icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = calculators[activeFinanceCalc].name,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Tap to switch calculator",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Switch")
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    ActiveFinanceCalcRender(activeFinanceCalc)
                }
            }

            // Bottom drawer simulator
            if (showSelectionDialog) {
                AlertDialog(
                    onDismissRequest = { showSelectionDialog = false },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showSelectionDialog = false }) {
                            Text("Cancel")
                        }
                    },
                    title = {
                        Text("Select Financial Tool", fontWeight = FontWeight.Black)
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            calculators.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (activeFinanceCalc == index) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable {
                                            activeFinanceCalc = index
                                            showSelectionDialog = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(item.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

data class FinanceCalcItem(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val description: String)

@Composable
fun ActiveFinanceCalcRender(index: Int) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (index) {
                0 -> GorgeousLoanEmiCalculator()
                1 -> SimpleCompoundInterestCalculator()
                2 -> SipCalculatorRedesign()
                3 -> RoiCagrCalculator()
                4 -> GstTaxCalculator()
                5 -> DiscountMarkupCalculator()
                6 -> TipBillSplitCalculator()
                7 -> SalaryCtcCalculator()
                8 -> InflationAdjustedCalculator()
                9 -> FdRdBatCalculator()
            }
        }
    }
}

// -------------------------------------------------------------
// REDESIGNED GORGEOUS LOAN EMI SCREEN (Issue 2)
// -------------------------------------------------------------
@Composable
fun GorgeousLoanEmiCalculator() {
    var principalInput by remember { mutableStateOf("1000000") }
    var interestInput by remember { mutableStateOf("8.5") }
    var tenureInput by remember { mutableStateOf("15") } // years

    val principal = principalInput.toDoubleOrNull() ?: 0.0
    val annualRate = interestInput.toDoubleOrNull() ?: 0.0
    val tenureYears = tenureInput.toDoubleOrNull() ?: 0.0

    var emi = 0.0
    var totalInterest = 0.0
    var totalPayment = 0.0

    try {
        if (principal > 0 && annualRate > 0 && tenureYears > 0) {
            val r = (annualRate / 12.0) / 100.0
            val n = tenureYears * 12.0
            emi = (principal * r * (1.0 + r).pow(n)) / ((1.0 + r).pow(n) - 1.0)
            totalPayment = emi * n
            totalInterest = totalPayment - principal
            if (totalInterest < 0) totalInterest = 0.0
        }
    } catch (e: Exception) {
        emi = 0.0
        totalInterest = 0.0
        totalPayment = 0.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Interactive Loan Amortization Plans", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Inputs Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Principal Loan Sum (₹)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                OutlinedTextField(
                    value = principalInput,
                    onValueChange = { principalInput = it },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("loan_amount_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Slider(
                    value = (principal.toFloat().coerceIn(10000f, 10000000f) - 10000f) / (10000000f - 10000f),
                    onValueChange = {
                        val v = 10000f + it * (10000000f - 10000f)
                        principalInput = v.toInt().toString()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Interest Factor Rate (% p.a.)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                OutlinedTextField(
                    value = interestInput,
                    onValueChange = { interestInput = it },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("loan_rate_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Slider(
                    value = (annualRate.toFloat().coerceIn(1f, 25f) - 1f) / 24f,
                    onValueChange = {
                        val r = 1f + it * 24f
                        interestInput = "%.2f".format(Locale.US, r)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Pay Back Tenure (Years)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                OutlinedTextField(
                    value = tenureInput,
                    onValueChange = { tenureInput = it },
                    suffix = { Text("Yrs") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("loan_tenure_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Slider(
                    value = (tenureYears.toFloat().coerceIn(1f, 30f) - 1f) / 29f,
                    onValueChange = {
                        val y = 1f + it * 29f
                        tenureInput = y.toInt().toString()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Output Result card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Calculated Monthly Installment",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = formatCurrency(emi),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                // Beautiful hollow ring Canvas representation
                EmiRatioPieRing(principal = principal, interest = totalInterest)

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Borrowed Principal", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(formatCurrency(principal), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Accrued Interest Loading", fontSize = 11.sp, color = Color(0xFFFF5252))
                        Text(formatCurrency(totalInterest), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFFF5252))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Outlay (Principal + Interest)", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(formatCurrency(totalPayment), fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun EmiRatioPieRing(principal: Double, interest: Double) {
    val total = principal + interest
    if (total <= 0) return
    val principalSweep = (principal / total * 360f).toFloat()
    val interestSweep = 360f - principalSweep

    val principalColor = MaterialTheme.colorScheme.primary
    val interestColor = Color(0xFFFF5252)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hollow ring canvas (0% dependency on backgrounds, perfectly light/dark transparent)
        Canvas(
            modifier = Modifier
                .size(70.dp)
                .padding(4.dp)
        ) {
            val strokeWidth = 16f
            drawArc(
                color = interestColor,
                startAngle = -90f,
                sweepAngle = interestSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
            drawArc(
                color = principalColor,
                startAngle = -90f + interestSweep,
                sweepAngle = principalSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(principalColor, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Loan Principal: ${(principal / total * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(interestColor, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Interest Burden: ${(interest / total * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5252)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// SIMPLE & COMPOUND INTEREST CALCULATOR
// -------------------------------------------------------------
@Composable
fun SimpleCompoundInterestCalculator() {
    var principalInput by remember { mutableStateOf("100000") }
    var rateInput by remember { mutableStateOf("7.5") }
    var yearsInput by remember { mutableStateOf("5") }
    var compoundFrequency by remember { mutableIntStateOf(1) } // 1 = yearly, 2 = semi, 4 = quarterly, 12 = monthly

    val principal = principalInput.toDoubleOrNull() ?: 0.0
    val rate = rateInput.toDoubleOrNull() ?: 0.0
    val years = yearsInput.toDoubleOrNull() ?: 0.0

    // Calculations
    val simpleInterest = principal * (rate / 100.0) * years
    val simpleTotal = principal + simpleInterest

    val compoundTotal = principal * (1.0 + (rate / 100.0) / compoundFrequency).pow(compoundFrequency * years)
    val compoundInterest = compoundTotal - principal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Interest Compounding Estimations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = principalInput,
                    onValueChange = { principalInput = it },
                    label = { Text("Principal Sum (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text("Annual Rate of Interest (%)") },
                    suffix = { Text("% p.a.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = yearsInput,
                    onValueChange = { yearsInput = it },
                    label = { Text("Tenure Time (Years)") },
                    suffix = { Text("Yrs") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Compounding Interval Frequency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp)
                ) {
                    val frequencies = listOf("Annual", "Quarterly", "Monthly")
                    val freqVals = listOf(1, 4, 12)
                    frequencies.forEachIndexed { idx, label ->
                        val isSel = freqVals[idx] == compoundFrequency
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { compoundFrequency = freqVals[idx] }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Yield Performance side-by-side", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                
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

                val excessGains = compoundInterest - simpleInterest
                Text(
                    text = "Compounding advantage payout difference yields: " + formatCurrency(excessGains),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// -------------------------------------------------------------
// SIP CALCULATOR REDESIGN
// -------------------------------------------------------------
@Composable
fun SipCalculatorRedesign() {
    var amountInput by remember { mutableStateOf("5000") }
    var returnInput by remember { mutableStateOf("12") }
    var yearsInput by remember { mutableStateOf("10") }

    val amount = amountInput.toDoubleOrNull() ?: 0.0
    val expectedReturn = returnInput.toDoubleOrNull() ?: 0.0
    val years = yearsInput.toDoubleOrNull() ?: 0.0

    var totalInvested = 0.0
    var futureValue = 0.0
    var estimatedReturns = 0.0

    try {
        if (amount > 0 && expectedReturn >= 0 && years > 0) {
            val r = expectedReturn / 100.0
            val i = r / 12.0
            val n = years * 12.0
            if (i > 0) {
                futureValue = amount * (((1.0 + i).pow(n) - 1.0) / i) * (1.0 + i)
            } else {
                futureValue = amount * n
            }
            totalInvested = amount * n
            estimatedReturns = futureValue - totalInvested
            if (estimatedReturns < 0) estimatedReturns = 0.0
        }
    } catch (e: Exception) {
        totalInvested = 0.0
        futureValue = 0.0
        estimatedReturns = 0.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Systematic Wealth Builder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Monthly Contribution (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = returnInput,
                    onValueChange = { returnInput = it },
                    label = { Text("Expected Return Speed (% p.a.)") },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = yearsInput,
                    onValueChange = { yearsInput = it },
                    label = { Text("Duration Timeline (Years)") },
                    suffix = { Text("Yrs") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                    Text(formatCurrency(futureValue), fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ROI / CAGR CALCULATOR
// -------------------------------------------------------------
@Composable
fun RoiCagrCalculator() {
    var initialInput by remember { mutableStateOf("50000") }
    var finalInput by remember { mutableStateOf("95000") }
    var durationInput by remember { mutableStateOf("4") }

    val initial = initialInput.toDoubleOrNull() ?: 0.0
    val finalVal = finalInput.toDoubleOrNull() ?: 0.0
    val duration = durationInput.toDoubleOrNull() ?: 0.0

    val absoluteReturn = if (initial > 0) ((finalVal - initial) / initial) * 100.0 else 0.0
    val cagr = if (initial > 0 && finalVal > 0 && duration > 0) {
        ((finalVal / initial).pow(1.0 / duration) - 1.0) * 100.0
    } else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Yield Performance Indices (ROI / CAGR)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = initialInput,
                    onValueChange = { initialInput = it },
                    label = { Text("Initial Investment Principal (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = finalInput,
                    onValueChange = { finalInput = it },
                    label = { Text("Final Maturity Asset Value (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = durationInput,
                    onValueChange = { durationInput = it },
                    label = { Text("Total Duration Span (Years)") },
                    suffix = { Text("Yrs") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Absolute Return Ratio", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Text("${"%.2f".format(absoluteReturn)}%", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
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
// GST / TAX CALCULATOR
// -------------------------------------------------------------
@Composable
fun GstTaxCalculator() {
    var amountInput by remember { mutableStateOf("1500") }
    var gstInput by remember { mutableStateOf("18") }
    var isAddGst by remember { mutableStateOf(true) }

    val amount = amountInput.toDoubleOrNull() ?: 0.0
    val gstPercent = gstInput.toDoubleOrNull() ?: 0.0

    val taxAmount: Double
    val totalAmount: Double
    val originalBase: Double

    if (isAddGst) {
        taxAmount = (amount * gstPercent) / 100.0
        totalAmount = amount + taxAmount
        originalBase = amount
    } else {
        originalBase = amount / (1.0 + gstPercent / 100.0)
        taxAmount = amount - originalBase
        totalAmount = amount
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("GST / Tax Assessment Engine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Financial Base Sum (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = gstInput,
                    onValueChange = { gstInput = it },
                    label = { Text("Tax Levy Speed (%)") },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isAddGst) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { isAddGst = true }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Add GST / tax", color = if (isAddGst) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isAddGst) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { isAddGst = false }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Remove GST / tax", color = if (!isAddGst) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pre-tax net sum base:", fontSize = 12.sp)
                    Text(formatCurrency(originalBase), fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Calculated tax levy:", fontSize = 12.sp)
                    Text(formatCurrency(taxAmount), fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Final Gross Total:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(formatCurrency(totalAmount), fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DISCOUNT & MARKUP CALCULATOR
// -------------------------------------------------------------
@Composable
fun DiscountMarkupCalculator() {
    var amountInput by remember { mutableStateOf("1000") }
    var percentInput by remember { mutableStateOf("20") }
    var isDiscountMode by remember { mutableStateOf(true) }

    val base = amountInput.toDoubleOrNull() ?: 0.0
    val pct = percentInput.toDoubleOrNull() ?: 0.0

    val offset = (base * pct) / 100.0
    val finalVal = if (isDiscountMode) base - offset else base + offset

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Product Pricing Adjuster", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDiscountMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { isDiscountMode = true }
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Discount mode", color = if (isDiscountMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!isDiscountMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { isDiscountMode = false }
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Markup mode", color = if (!isDiscountMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text(if (isDiscountMode) "List Price Base (₹)" else "Cost Pricing Base (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = percentInput,
                    onValueChange = { percentInput = it },
                    label = { Text(if (isDiscountMode) "Discount %" else "Profit Premium Markup %") },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (isDiscountMode) "Total retail savings offset:" else "Added wholesale markup value:", fontSize = 12.sp)
                    Text(formatCurrency(offset), fontWeight = FontWeight.Bold, color = if (isDiscountMode) Color(0xFF4CAF50) else Color(0xFFFF5252))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Target Final Price Item tag:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(formatCurrency(finalVal), fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TIP & BILL SPLIT CALCULATOR
// -------------------------------------------------------------
@Composable
fun TipBillSplitCalculator() {
    var billInput by remember { mutableStateOf("1500") }
    var tipInput by remember { mutableStateOf("12") }
    var peopleInput by remember { mutableStateOf("4") }

    val bill = billInput.toDoubleOrNull() ?: 0.0
    val tipPercent = tipInput.toDoubleOrNull() ?: 10.0
    val totalPeople = peopleInput.toIntOrNull() ?: 1

    val totalTip = (bill * tipPercent) / 100.0
    val overallTotal = bill + totalTip
    val splitTip = if (totalPeople > 0) totalTip / totalPeople else totalTip
    val splitBill = if (totalPeople > 0) overallTotal / totalPeople else overallTotal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Dining Apportionment Clocks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = billInput,
                    onValueChange = { billInput = it },
                    label = { Text("Base dining subtotal (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = tipInput,
                    onValueChange = { tipInput = it },
                    label = { Text(" gratuity Tip level (%)") },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = peopleInput,
                    onValueChange = { peopleInput = it },
                    label = { Text("Active bill dynamic split headcount") },
                    prefix = { Text("🧑‍🤝‍🧑 ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                        Text(formatCurrency(splitBill), fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SALARY / CTC BREAKUP CALCULATOR
// -------------------------------------------------------------
@Composable
fun SalaryCtcCalculator() {
    var ctcInput by remember { mutableStateOf("1200000") } // 12 LPA default

    val ctcPrice = ctcInput.toDoubleOrNull() ?: 0.0

    val grossMonthly = ctcPrice / 12.0
    val statePF = (grossMonthly * 0.12).coerceIn(0.0, 15000.0) // 12% basic approx
    val estimatedTaxes = if (ctcPrice > 1000000) (grossMonthly * 0.15) else if (ctcPrice > 500000) (grossMonthly * 0.05) else 0.0
    val standardTakeHome = grossMonthly - statePF - estimatedTaxes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Salary Bracket Breakdowns & PF Indicators", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = ctcInput,
                    onValueChange = { ctcInput = it },
                    label = { Text("Annual Gross CTC (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Accurate Monthly Pro-Rata breakdowns", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pro-Rata gross monthly rate:", fontSize = 12.sp)
                    Text(formatCurrency(grossMonthly), fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Employee PF contributions (12% approx):", fontSize = 12.sp)
                    Text(formatCurrency(statePF), fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Estimated income bracket taxes deduction:", fontSize = 12.sp)
                    Text(formatCurrency(estimatedTaxes), fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Net Monthly Take-Home (estimated):", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text(formatCurrency(standardTakeHome), fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// INFLATION ADJUSTED VALUE CALCULATOR
// -------------------------------------------------------------
@Composable
fun InflationAdjustedCalculator() {
    var amountInput by remember { mutableStateOf("10000") }
    var inflationRateInput by remember { mutableStateOf("6") }
    var yearsInput by remember { mutableStateOf("15") }

    val amount = amountInput.toDoubleOrNull() ?: 0.0
    val rate = inflationRateInput.toDoubleOrNull() ?: 0.0
    val years = yearsInput.toDoubleOrNull() ?: 0.0

    val futureValueMultiplier = (1.0 + (rate / 100.0)).pow(years)
    val futurePurchasePowerPrice = if (futureValueMultiplier > 0) amount / futureValueMultiplier else amount
    val pastPowerEquivalent = amount * futureValueMultiplier

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("inflationary Purchase Power Trackers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Base valuation asset (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = inflationRateInput,
                    onValueChange = { inflationRateInput = it },
                    label = { Text("Avg annual Inflation multiplier (%)") },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = yearsInput,
                    onValueChange = { yearsInput = it },
                    label = { Text("Expected Time Span timeline (Years)") },
                    suffix = { Text("Yrs") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text("Future real value (Purchasing Power):", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(formatCurrency(futurePurchasePowerPower(amount, rate, years)), fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Value of " + formatCurrency(amount) + " today in ${years.toInt()} years", fontSize = 11.sp)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))

                Column {
                    Text("Amount needed to match purchase power in ${years.toInt()} years:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(formatCurrency(pastPowerEquivalent), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFFF5252))
                }
            }
        }
    }
}

private fun futurePurchasePowerPower(p: Double, r: Double, y: Double): Double {
    return p / (1.0 + r/100.0).pow(y)
}

// -------------------------------------------------------------
// FD / RD MATURITY CALCULATOR
// -------------------------------------------------------------
@Composable
fun FdRdBatCalculator() {
    var isFixedDeposit by remember { mutableStateOf(true) }
    var investAmountInput by remember { mutableStateOf("100000") }
    var interestRateInput by remember { mutableStateOf("7.1") }
    var tenureInput by remember { mutableStateOf("5") }

    val amount = investAmountInput.toDoubleOrNull() ?: 0.0
    val rate = interestRateInput.toDoubleOrNull() ?: 0.0
    val years = tenureInput.toDoubleOrNull() ?: 0.0

    var maturityAccumulated = 0.0
    var principalInvested = 0.0

    if (isFixedDeposit) {
        // Compound quarterly standard FD
        principalInvested = amount
        maturityAccumulated = amount * (1.0 + (rate / 100.0) / 4.0).pow(4.0 * years)
    } else {
        // RD compound monthly
        principalInvested = amount * (years * 12.0)
        val n = years * 12.0
        val tempR = (rate / 100.0) / 12.0
        // standard monthly recurring deposit compound formulas
        if (tempR > 0) {
            var total = 0.0
            for (month in 1..n.toInt()) {
                total = (total + amount) * (1.0 + tempR)
            }
            maturityAccumulated = total
        } else {
            maturityAccumulated = principalInvested
        }
    }

    val totalInterestAccrued = maturityAccumulated - principalInvested

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("FD / RD Sovereign Maturity Engines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isFixedDeposit) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { isFixedDeposit = true }
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Fixed Deposit (FD)", color = if (isFixedDeposit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!isFixedDeposit) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { isFixedDeposit = false }
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Recurring (RD)", color = if (!isFixedDeposit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = investAmountInput,
                    onValueChange = { investAmountInput = it },
                    label = { Text(if (isFixedDeposit) "Lumpsum Deposit Principal (₹)" else "Monthly Deposit Contribution (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = interestRateInput,
                    onValueChange = { interestRateInput = it },
                    label = { Text("Aggressive Interest Rate (% p.a.)") },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = tenureInput,
                    onValueChange = { tenureInput = it },
                    label = { Text("Duration Period Timeline (Years)") },
                    suffix = { Text("Yrs") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Accumulated Invested Base:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(formatCurrency(principalInvested), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Accrued Interest Gains:", fontSize = 11.sp, color = Color(0xFF4CAF50))
                        Text(formatCurrency(totalInterestAccrued), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF4CAF50))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Maturity Pay Out Yield sum:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(formatCurrency(maturityAccumulated), fontWeight = FontWeight.Black, fontSize = 21.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private fun formatCurrency(valDouble: Double): String {
    val df = DecimalFormat("₹ #,##,###.##", DecimalFormatSymbols(Locale.US))
    return df.format(valDouble)
}
