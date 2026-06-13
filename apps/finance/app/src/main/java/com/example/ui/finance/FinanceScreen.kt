package com.example.ui.finance

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import com.example.data.SettingsRepository
import org.koin.compose.koinInject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@Composable
fun FinanceScreen(
    viewModel: FinanceViewModel,
    settingsRepository: SettingsRepository = koinInject(),
    modifier: Modifier = Modifier
) {
    val activeFinanceCalc by viewModel.activeFinanceCalc.collectAsStateWithLifecycle()
    
    val calculators = listOf(
        FinanceCalcItem("Loan EMI", Icons.Default.Assessment, "Evaluate monthly home/car EMI and interest ratios."),
        FinanceCalcItem("Simple & Compound", Icons.Default.Percent, "Compare generic interest yields across compounding periods."),
        FinanceCalcItem("SIP Growth", Icons.AutoMirrored.Filled.TrendingUp, "Track future wealth growth of monthly mutual fund plans."),
        FinanceCalcItem("ROI / CAGR", Icons.Default.MonetizationOn, "Determine exact return ratios and annual growth speeds."),
        FinanceCalcItem("GST / Tax", Icons.Default.Receipt, "Calculate net taxes, gross totals, and net billing items."),
        FinanceCalcItem("Discount & Markup", Icons.Default.ShoppingBag, "Compute discount savings margins or wholesale pricing Markups."),
        FinanceCalcItem("Tip & Bill Split", Icons.Default.RoomService, "Apportion gratuities and divide billing tab cleanly among friends."),
        FinanceCalcItem("Salary Breakup", Icons.Default.Payments, "Review annual CTC packages down to estimated monthly take-home pays."),
        FinanceCalcItem("Inflation Adjusted", Icons.AutoMirrored.Filled.TrendingDown, "See past or future purchasing power adjustments of savings."),
        FinanceCalcItem("FD / RD Maturity", Icons.Default.Savings, "Track Fixed/Recurring Deposits maturity pay outs and accruals.")
    )

    val visibleCalculators by remember(calculators) {
        combine(
            calculators.map { item ->
                settingsRepository.isToolEnabled(item.name).map { item to it }
            }
        ) { array ->
            array.filter { it.second }.map { it.first }
        }
    }.collectAsState(initial = calculators)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (activeFinanceCalc == null) {
            // Main Library Grid View (Matches Screenshot Redesign perfectly!)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Financial Planning",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    text = "Select a tool to begin calculations",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // 3-Column beautiful responsive grid
                val rows = visibleCalculators.chunked(3)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            row.forEach { item ->
                                val index = calculators.indexOf(item)
                                GridFinanceItemCard(
                                    item = item,
                                    onClick = { viewModel.setActiveFinanceCalc(index) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill remaining space if row is not full
                            if (row.size < 3) {
                                repeat(3 - row.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Active Tool Container View (with Elegant Top App Bar)
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.setActiveFinanceCalc(null) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to list",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            val activeItem = calculators[activeFinanceCalc ?: 0]
                            Text(
                                text = activeItem.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = activeItem.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    ActiveFinanceCalcRender(activeFinanceCalc ?: 0, viewModel)
                }
            }
        }
    }
}

@Composable
fun GridFinanceItemCard(
    item: FinanceCalcItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp)
            .testTag("grid_item_${item.name.lowercase().replace(" ", "_").replace("&", "and").replace("/", "and")}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
    }
}

data class FinanceCalcItem(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val description: String)

@Composable
fun ActiveFinanceCalcRender(index: Int, viewModel: FinanceViewModel) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (index) {
                0 -> GorgeousLoanEmiCalculator(viewModel)
                1 -> SimpleCompoundInterestCalculator(viewModel)
                2 -> SipCalculatorRedesign(viewModel)
                3 -> RoiCagrCalculator(viewModel)
                4 -> GstTaxCalculator(viewModel)
                5 -> DiscountMarkupCalculator(viewModel)
                6 -> TipBillSplitCalculator(viewModel)
                7 -> SalaryCtcCalculator(viewModel)
                8 -> InflationAdjustedCalculator(viewModel)
                9 -> FdRdBatCalculator(viewModel)
            }
        }
    }
}

// -------------------------------------------------------------
// REDESIGNED GORGEOUS LOAN EMI SCREEN (Issue 2)
// -------------------------------------------------------------
@Composable
fun GorgeousLoanEmiCalculator(viewModel: FinanceViewModel) {
    var principalInput by remember { mutableStateOf("1000000") }
    var interestInput by remember { mutableStateOf("8.5") }
    var tenureInput by remember { mutableStateOf("15") } // years

    val principal = principalInput.toDoubleOrNull() ?: 0.0
    val annualRate = interestInput.toDoubleOrNull() ?: 0.0
    val tenureYears = tenureInput.toDoubleOrNull() ?: 0.0

    val emiResult = remember(principal, annualRate, tenureYears) {
        viewModel.calculateEmi(principal, annualRate, tenureYears)
    }

    val emi = emiResult.emi
    val totalInterest = emiResult.totalInterest
    val totalPayment = emiResult.totalPayment

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
                EmiRatioPieRing(principal = principal, interest = totalInterest.toDouble())

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
fun SimpleCompoundInterestCalculator(viewModel: FinanceViewModel) {
    var principalInput by remember { mutableStateOf("100000") }
    var rateInput by remember { mutableStateOf("7.5") }
    var yearsInput by remember { mutableStateOf("5") }
    var compoundFrequency by remember { mutableIntStateOf(1) } // 1 = yearly, 2 = semi, 4 = quarterly, 12 = monthly

    val principal = principalInput.toDoubleOrNull() ?: 0.0
    val rate = rateInput.toDoubleOrNull() ?: 0.0
    val years = yearsInput.toDoubleOrNull() ?: 0.0

    val scResult = remember(principal, rate, years, compoundFrequency) {
        viewModel.calculateSimpleCompound(principal, rate, years, compoundFrequency)
    }

    val simpleInterest = scResult.simpleInterest
    val simpleTotal = scResult.simpleTotal
    val compoundInterest = scResult.compoundInterest
    val compoundTotal = scResult.compoundTotal

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

                val excessGains = compoundInterest.subtract(simpleInterest).coerceAtLeast(BigDecimal.ZERO)
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
fun SipCalculatorRedesign(viewModel: FinanceViewModel) {
    var amountInput by remember { mutableStateOf("5000") }
    var returnInput by remember { mutableStateOf("12") }
    var yearsInput by remember { mutableStateOf("10") }

    val amount = amountInput.toDoubleOrNull() ?: 0.0
    val expectedReturn = returnInput.toDoubleOrNull() ?: 0.0
    val years = yearsInput.toDoubleOrNull() ?: 0.0

    val sipResult = remember(amount, expectedReturn, years) {
        viewModel.calculateSip(amount, expectedReturn, years)
    }

    val totalInvested = sipResult.totalInvested
    val futureValue = sipResult.futureValue
    val estimatedReturns = sipResult.estimatedReturns

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
fun RoiCagrCalculator(viewModel: FinanceViewModel) {
    var initialInput by remember { mutableStateOf("50000") }
    var finalInput by remember { mutableStateOf("95000") }
    var durationInput by remember { mutableStateOf("4") }

    val initial = initialInput.toDoubleOrNull() ?: 0.0
    val finalVal = finalInput.toDoubleOrNull() ?: 0.0
    val duration = durationInput.toDoubleOrNull() ?: 0.0

    val roiCagrResult = remember(initial, finalVal, duration) {
        viewModel.calculateRoiCagr(initial, finalVal, duration)
    }
    val absoluteReturn = roiCagrResult.absoluteReturn
    val cagr = roiCagrResult.cagr

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
fun GstTaxCalculator(viewModel: FinanceViewModel) {
    var amountInput by remember { mutableStateOf("1500") }
    var gstInput by remember { mutableStateOf("18") }
    var isAddGst by remember { mutableStateOf(true) }

    val amount = amountInput.toDoubleOrNull() ?: 0.0
    val gstPercent = gstInput.toDoubleOrNull() ?: 0.0

    val gstResult = remember(amount, gstPercent, isAddGst) {
        viewModel.calculateGst(amount, gstPercent, isAddGst)
    }

    val taxAmount = gstResult.taxAmount
    val totalAmount = gstResult.totalAmount
    val originalBase = gstResult.preTaxBase

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
fun DiscountMarkupCalculator(viewModel: FinanceViewModel) {
    var amountInput by remember { mutableStateOf("1000") }
    var percentInput by remember { mutableStateOf("20") }
    var isDiscountMode by remember { mutableStateOf(true) }

    val base = amountInput.toDoubleOrNull() ?: 0.0
    val pct = percentInput.toDoubleOrNull() ?: 0.0

    val dmResult = remember(base, pct, isDiscountMode) {
        viewModel.calculateDiscountMarkup(base, pct, isDiscountMode)
    }

    val offset = dmResult.offset
    val finalVal = dmResult.finalVal

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
fun TipBillSplitCalculator(viewModel: FinanceViewModel) {
    var billInput by remember { mutableStateOf("1500") }
    var tipInput by remember { mutableStateOf("12") }
    var peopleInput by remember { mutableStateOf("4") }

    val bill = billInput.toDoubleOrNull() ?: 0.0
    val tipPercent = tipInput.toDoubleOrNull() ?: 10.0
    val totalPeople = peopleInput.toIntOrNull() ?: 1

    val tipResult = remember(bill, tipPercent, totalPeople) {
        viewModel.calculateTipSplit(bill, tipPercent, totalPeople)
    }

    val totalTip = tipResult.totalTip
    val overallTotal = tipResult.overallTotal
    val splitTip = tipResult.splitTip
    val splitBill = tipResult.splitBill

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
fun SalaryCtcCalculator(viewModel: FinanceViewModel) {
    var ctcInput by remember { mutableStateOf("1200000") } // 12 LPA default

    val ctcPrice = ctcInput.toDoubleOrNull() ?: 0.0

    val salaryResult = remember(ctcPrice) {
        viewModel.calculateSalaryBreakup(ctcPrice)
    }

    val grossMonthly = salaryResult.grossMonthly
    val statePF = salaryResult.pfContribution
    val estimatedTaxes = salaryResult.estimatedTax
    val standardTakeHome = salaryResult.takeHome

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
fun InflationAdjustedCalculator(viewModel: FinanceViewModel) {
    var amountInput by remember { mutableStateOf("10000") }
    var inflationRateInput by remember { mutableStateOf("6") }
    var yearsInput by remember { mutableStateOf("15") }

    val amount = amountInput.toDoubleOrNull() ?: 0.0
    val rate = inflationRateInput.toDoubleOrNull() ?: 0.0
    val years = yearsInput.toDoubleOrNull() ?: 0.0

    val inflationResult = remember(amount, rate, years) {
        viewModel.calculateInflation(amount, rate, years)
    }

    val futurePurchasePower = inflationResult.futurePurchasePower
    val amountNeeded = inflationResult.amountNeeded

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
                    Text(formatCurrency(futurePurchasePower), fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Value of " + formatCurrency(amount) + " today in ${years.toInt()} years", fontSize = 11.sp)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))

                Column {
                    Text("Amount needed to match purchase power in ${years.toInt()} years:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(formatCurrency(amountNeeded), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFFF5252))
                }
            }
        }
    }
}



// -------------------------------------------------------------
// FD / RD MATURITY CALCULATOR
// -------------------------------------------------------------
@Composable
fun FdRdBatCalculator(viewModel: FinanceViewModel) {
    var isFixedDeposit by remember { mutableStateOf(true) }
    var investAmountInput by remember { mutableStateOf("100000") }
    var interestRateInput by remember { mutableStateOf("7.1") }
    var tenureInput by remember { mutableStateOf("5") }

    val amount = investAmountInput.toDoubleOrNull() ?: 0.0
    val rate = interestRateInput.toDoubleOrNull() ?: 0.0
    val years = tenureInput.toDoubleOrNull() ?: 0.0

    val fdRdResult = remember(amount, rate, years, isFixedDeposit) {
        viewModel.calculateFdRd(amount, rate, years, isFixedDeposit)
    }

    val principalInvested = fdRdResult.principalInvested
    val totalInterestAccrued = fdRdResult.interestGains
    val maturityAccumulated = fdRdResult.maturityValue

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
    return com.example.util.CurrencyFormatter.format(valDouble)
}

private fun formatCurrency(valBD: BigDecimal): String {
    return com.example.util.CurrencyFormatter.format(valBD)
}
