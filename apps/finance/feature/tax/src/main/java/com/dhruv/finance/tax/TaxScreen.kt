package com.dhruv.finance.tax

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
fun TaxScreen(viewModel: TaxViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("GST / Tax", "Salary Breakup")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        when (selectedTab) {
            0 -> GstTaxCalculator(viewModel)
            1 -> SalaryCtcCalculator(viewModel)
        }
    }
}

// -------------------------------------------------------------
// GST / TAX CALCULATOR (moved verbatim from FinanceScreen.kt)
// -------------------------------------------------------------
@Composable
fun GstTaxCalculator(viewModel: TaxViewModel) {
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
// SALARY / CTC BREAKUP CALCULATOR (moved verbatim from FinanceScreen.kt)
// -------------------------------------------------------------
@Composable
fun SalaryCtcCalculator(viewModel: TaxViewModel) {
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

private fun formatCurrency(value: BigDecimal): String = CurrencyFormatter.format(value)
