package com.dhruv.finance.loans

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.finance.data.util.CurrencyFormatter
import java.math.BigDecimal

@Composable
fun LoansScreen(viewModel: LoansViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Loan EMI", "Loan Comparison")

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
            0 -> GorgeousLoanEmiCalculator(viewModel)
            1 -> LoanComparisonCalculator(viewModel)
        }
    }
}

// -------------------------------------------------------------
// REDESIGNED GORGEOUS LOAN EMI SCREEN
// (moved verbatim from FinanceScreen.kt, param type updated)
// -------------------------------------------------------------
@Composable
fun GorgeousLoanEmiCalculator(viewModel: LoansViewModel) {
    var principalInput by remember { mutableStateOf("1000000") }
    var interestInput by remember { mutableStateOf("8.5") }
    var tenureInput by remember { mutableStateOf("15") } // years

    val principal = principalInput.toDoubleOrNull() ?: 0.0
    val annualRate = interestInput.toDoubleOrNull() ?: 0.0
    val tenureYears = tenureInput.toDoubleOrNull() ?: 0.0

    val emiResult =
        remember(principal, annualRate, tenureYears) {
            viewModel.calculateEmi(principal, annualRate, tenureYears)
        }

    val emi = emiResult.emi
    val totalInterest = emiResult.totalInterest
    val totalPayment = emiResult.totalPayment

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Interactive Loan Amortization Plans", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Inputs Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Principal Loan Sum (₹)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                OutlinedTextField(
                    value = principalInput,
                    onValueChange = { principalInput = it },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                Slider(
                    value = (principal.toFloat().coerceIn(10000f, 10000000f) - 10000f) / (10000000f - 10000f),
                    onValueChange = {
                        val v = 10000f + it * (10000000f - 10000f)
                        principalInput = v.toInt().toString()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    "Interest Factor Rate (% p.a.)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                OutlinedTextField(
                    value = interestInput,
                    onValueChange = { interestInput = it },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                Slider(
                    value = (annualRate.toFloat().coerceIn(1f, 25f) - 1f) / 24f,
                    onValueChange = {
                        val r = 1f + it * 24f
                        interestInput = "%.2f".format(java.util.Locale.US, r)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Pay Back Tenure (Years)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                OutlinedTextField(
                    value = tenureInput,
                    onValueChange = { tenureInput = it },
                    suffix = { Text("Yrs") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                Slider(
                    value = (tenureYears.toFloat().coerceIn(1f, 30f) - 1f) / 29f,
                    onValueChange = {
                        val y = 1f + it * 29f
                        tenureInput = y.toInt().toString()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Output Result card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Calculated Monthly Installment",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Text(
                    text = formatCurrency(emi),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                )

                // Beautiful hollow ring Canvas representation
                EmiRatioPieRing(principal = principal, interest = totalInterest.toDouble())

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
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
                    Text(
                        formatCurrency(totalPayment),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
fun EmiRatioPieRing(
    principal: Double,
    interest: Double,
) {
    val total = principal + interest
    if (total <= 0) return
    val principalSweep = (principal / total * 360f).toFloat()
    val interestSweep = 360f - principalSweep

    val principalColor = MaterialTheme.colorScheme.primary
    val interestColor = Color(0xFFFF5252)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Hollow ring canvas
        Canvas(
            modifier =
                Modifier
                    .size(70.dp)
                    .padding(4.dp),
        ) {
            val strokeWidth = 16f
            drawArc(
                color = interestColor,
                startAngle = -90f,
                sweepAngle = interestSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = principalColor,
                startAngle = -90f + interestSweep,
                sweepAngle = principalSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(principalColor, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Loan Principal: ${(principal / total * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(interestColor, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Interest Burden: ${(interest / total * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5252),
                )
            }
        }
    }
}

// -------------------------------------------------------------
// LOAN COMPARISON SCREEN (new — reuses calculateEmi engine)
// -------------------------------------------------------------
@Composable
fun LoanComparisonCalculator(viewModel: LoansViewModel) {
    // Loan A inputs
    var principalAInput by remember { mutableStateOf("1000000") }
    var rateAInput by remember { mutableStateOf("8.5") }
    var tenureAInput by remember { mutableStateOf("15") }

    // Loan B inputs
    var principalBInput by remember { mutableStateOf("1000000") }
    var rateBInput by remember { mutableStateOf("9.5") }
    var tenureBInput by remember { mutableStateOf("20") }

    val principalA = principalAInput.toDoubleOrNull() ?: 0.0
    val rateA = rateAInput.toDoubleOrNull() ?: 0.0
    val tenureA = tenureAInput.toDoubleOrNull() ?: 0.0

    val principalB = principalBInput.toDoubleOrNull() ?: 0.0
    val rateB = rateBInput.toDoubleOrNull() ?: 0.0
    val tenureB = tenureBInput.toDoubleOrNull() ?: 0.0

    val resultA =
        remember(principalA, rateA, tenureA) {
            viewModel.calculateEmi(principalA, rateA, tenureA)
        }
    val resultB =
        remember(principalB, rateB, tenureB) {
            viewModel.calculateEmi(principalB, rateB, tenureB)
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Side-by-Side Loan Comparison", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Loan A inputs
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Loan A",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedTextField(
                    value = principalAInput,
                    onValueChange = { principalAInput = it },
                    label = { Text("Principal (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = rateAInput,
                    onValueChange = { rateAInput = it },
                    label = { Text("Interest Rate (% p.a.)") },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = tenureAInput,
                    onValueChange = { tenureAInput = it },
                    label = { Text("Tenure (Years)") },
                    suffix = { Text("Yrs") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }

        // Loan B inputs
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Loan B",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
                OutlinedTextField(
                    value = principalBInput,
                    onValueChange = { principalBInput = it },
                    label = { Text("Principal (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = rateBInput,
                    onValueChange = { rateBInput = it },
                    label = { Text("Interest Rate (% p.a.)") },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = tenureBInput,
                    onValueChange = { tenureBInput = it },
                    label = { Text("Tenure (Years)") },
                    suffix = { Text("Yrs") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }

        // Comparison result
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Comparison Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Loan A EMI", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(
                            formatCurrency(resultA.emi),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text("Interest: ${formatCurrency(resultA.totalInterest)}", fontSize = 10.sp)
                        Text("Total: ${formatCurrency(resultA.totalPayment)}", fontSize = 10.sp)
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Loan B EMI", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(
                            formatCurrency(resultB.emi),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text("Interest: ${formatCurrency(resultB.totalInterest)}", fontSize = 10.sp)
                        Text("Total: ${formatCurrency(resultB.totalPayment)}", fontSize = 10.sp)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                val interestDiff = resultA.totalInterest.subtract(resultB.totalInterest)
                val cheaperLabel =
                    if (interestDiff >
                        BigDecimal.ZERO
                    ) {
                        "Loan B saves ${formatCurrency(interestDiff)} in interest"
                    } else if (interestDiff <
                        BigDecimal.ZERO
                    ) {
                        "Loan A saves ${formatCurrency(interestDiff.negate())} in interest"
                    } else {
                        "Both loans cost the same in interest"
                    }
                Text(cheaperLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun formatCurrency(value: Double): String = CurrencyFormatter.format(value)

private fun formatCurrency(value: BigDecimal): String = CurrencyFormatter.format(value)
