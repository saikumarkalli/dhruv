package com.dhruv.finance.tax

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
fun TaxScreen(viewModel: TaxViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        SegmentedRow(
            options = TaxTabs,
            selectedIndex = selectedTab,
            onSelected = { selectedTab = it },
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = DhruvNextSpacing.screenGutter,
                vertical = DhruvNextSpacing.interCardGap,
            ),
        )
        when (selectedTab) {
            0 -> GstTaxCalculator(viewModel)
            1 -> SalaryCtcCalculator(viewModel)
        }
    }
}

@Composable
fun GstTaxCalculator(viewModel: TaxViewModel) {
    var amountInput by remember { mutableStateOf("1500") }
    var gstInput by remember { mutableStateOf("18") }
    var gstModeIndex by remember { mutableIntStateOf(0) }
    val isAddGst = gstModeIndex == 0

    val colors = LocalDhruvNextColors.current
    val amount = amountInput.toDoubleOrNull() ?: 0.0
    val gstPercent = gstInput.toDoubleOrNull() ?: 0.0

    val gstResult =
        remember(amount, gstPercent, isAddGst) {
            viewModel.calculateGst(amount, gstPercent, isAddGst)
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        SectionLabel(text = "GST / Tax")

        NxCard {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                NxTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = "Amount (₹)",
                    prefix = "₹",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                NxTextField(
                    value = gstInput,
                    onValueChange = { gstInput = it },
                    label = "Tax Rate (%)",
                    suffix = "%",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                SegmentedRow(
                    options = GstModes,
                    selectedIndex = gstModeIndex,
                    onSelected = { gstModeIndex = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        NxCard {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pre-tax base", fontSize = DhruvNextType.meta, color = colors.tx2)
                    Text(formatCurrency(gstResult.preTaxBase), fontWeight = FontWeight.Bold, fontSize = DhruvNextType.body, color = colors.tx)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tax amount", fontSize = DhruvNextType.meta, color = colors.tx2)
                    Text(formatCurrency(gstResult.taxAmount), fontWeight = FontWeight.Bold, fontSize = DhruvNextType.body, color = colors.neg)
                }
                HorizontalDivider(thickness = 0.5.dp, color = colors.line)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", fontWeight = FontWeight.Bold, fontSize = DhruvNextType.cardTitle, color = colors.tx)
                    Text(formatCurrency(gstResult.totalAmount), fontWeight = FontWeight.Bold, fontSize = DhruvNextType.title, color = colors.acc)
                }
            }
        }
    }
}

@Composable
fun SalaryCtcCalculator(viewModel: TaxViewModel) {
    var ctcInput by remember { mutableStateOf("1200000") }

    val colors = LocalDhruvNextColors.current
    val ctcPrice = ctcInput.toDoubleOrNull() ?: 0.0

    val salaryResult =
        remember(ctcPrice) {
            viewModel.calculateSalaryBreakup(ctcPrice)
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        SectionLabel(text = "Salary breakup")

        NxCard {
            NxTextField(
                value = ctcInput,
                onValueChange = { ctcInput = it },
                label = "Annual CTC (₹)",
                prefix = "₹",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        NxCard {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                Text("Monthly breakdown", fontSize = DhruvNextType.meta, fontWeight = FontWeight.Medium, color = colors.tx3)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gross monthly", fontSize = DhruvNextType.meta, color = colors.tx2)
                    Text(formatCurrency(salaryResult.grossMonthly), fontWeight = FontWeight.Bold, fontSize = DhruvNextType.body, color = colors.tx)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("PF contribution (12%)", fontSize = DhruvNextType.meta, color = colors.tx2)
                    Text(formatCurrency(salaryResult.pfContribution), fontWeight = FontWeight.Bold, fontSize = DhruvNextType.body, color = colors.neg)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Estimated tax", fontSize = DhruvNextType.meta, color = colors.tx2)
                    Text(formatCurrency(salaryResult.estimatedTax), fontWeight = FontWeight.Bold, fontSize = DhruvNextType.body, color = colors.neg)
                }

                HorizontalDivider(thickness = 0.5.dp, color = colors.line)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Net take-home", fontWeight = FontWeight.Bold, fontSize = DhruvNextType.cardTitle, color = colors.tx)
                    Text(formatCurrency(salaryResult.takeHome), fontWeight = FontWeight.Bold, fontSize = DhruvNextType.title, color = colors.acc)
                }
            }
        }
    }
}

private fun formatCurrency(value: BigDecimal): String = CurrencyFormatter.format(value)
