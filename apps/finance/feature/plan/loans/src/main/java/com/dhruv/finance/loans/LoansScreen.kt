package com.dhruv.finance.loans

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.components.NxTextField
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.components.SegmentedRow
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.util.CurrencyFormatter
import java.math.BigDecimal

@Composable
fun LoansScreen(viewModel: LoansViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        SegmentedRow(
            options = LoanTabs,
            selectedIndex = selectedTab,
            onSelected = { selectedTab = it },
            modifier =
                Modifier.fillMaxWidth().padding(
                    horizontal = DhruvNextSpacing.screenGutter,
                    vertical = DhruvNextSpacing.interCardGap,
                ),
        )
        when (selectedTab) {
            0 -> GorgeousLoanEmiCalculator(viewModel)
            1 -> LoanComparisonCalculator(viewModel)
        }
    }
}

@Composable
fun GorgeousLoanEmiCalculator(viewModel: LoansViewModel) {
    var principalInput by remember { mutableStateOf("1000000") }
    var interestInput by remember { mutableStateOf("8.5") }
    var tenureInput by remember { mutableStateOf("15") }

    val colors = LocalDhruvNextColors.current
    val principal = principalInput.toDoubleOrNull() ?: 0.0
    val annualRate = interestInput.toDoubleOrNull() ?: 0.0
    val tenureYears = tenureInput.toDoubleOrNull() ?: 0.0

    val emiResult =
        remember(principal, annualRate, tenureYears) {
            viewModel.calculateEmi(principal, annualRate, tenureYears)
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        SectionLabel(text = "Loan EMI")

        NxCard {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                NxTextField(
                    value = principalInput,
                    onValueChange = { principalInput = it },
                    label = "Principal (₹)",
                    prefix = "₹",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Slider(
                    value = (principal.toFloat().coerceIn(10000f, 10000000f) - 10000f) / (10000000f - 10000f),
                    onValueChange = {
                        val v = 10000f + it * (10000000f - 10000f)
                        principalInput = v.toInt().toString()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        SliderDefaults.colors(
                            thumbColor = colors.acc,
                            activeTrackColor = colors.acc,
                            inactiveTrackColor = colors.surf2,
                        ),
                )

                NxTextField(
                    value = interestInput,
                    onValueChange = { interestInput = it },
                    label = "Interest Rate (% p.a.)",
                    suffix = "%",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Slider(
                    value = (annualRate.toFloat().coerceIn(1f, 25f) - 1f) / 24f,
                    onValueChange = {
                        val r = 1f + it * 24f
                        interestInput = "%.2f".format(java.util.Locale.US, r)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        SliderDefaults.colors(
                            thumbColor = colors.acc,
                            activeTrackColor = colors.acc,
                            inactiveTrackColor = colors.surf2,
                        ),
                )

                NxTextField(
                    value = tenureInput,
                    onValueChange = { tenureInput = it },
                    label = "Tenure (Years)",
                    suffix = "Yrs",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Slider(
                    value = (tenureYears.toFloat().coerceIn(1f, 30f) - 1f) / 29f,
                    onValueChange = {
                        val y = 1f + it * 29f
                        tenureInput = y.toInt().toString()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        SliderDefaults.colors(
                            thumbColor = colors.acc,
                            activeTrackColor = colors.acc,
                            inactiveTrackColor = colors.surf2,
                        ),
                )
            }
        }

        NxCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Monthly EMI", fontSize = DhruvNextType.meta, color = colors.tx3)
                Text(
                    text = formatCurrency(emiResult.emi),
                    fontSize = DhruvNextType.hero,
                    fontWeight = FontWeight.Bold,
                    color = colors.acc,
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = DhruvNextSpacing.interCardGap),
                    thickness = 0.5.dp,
                    color = colors.line,
                )

                EmiRatioPieRing(principal = principal, interest = emiResult.totalInterest.toDouble())

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = DhruvNextSpacing.interCardGap),
                    thickness = 0.5.dp,
                    color = colors.line,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Principal", fontSize = DhruvNextType.meta, color = colors.tx3)
                        Text(formatCurrency(principal), fontWeight = FontWeight.Bold, fontSize = DhruvNextType.body, color = colors.tx)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Interest", fontSize = DhruvNextType.meta, color = colors.neg)
                        Text(
                            formatCurrency(emiResult.totalInterest),
                            fontWeight = FontWeight.Bold,
                            fontSize = DhruvNextType.body,
                            color = colors.neg,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(DhruvNextSpacing.inputGroupGap))

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total payment", fontSize = DhruvNextType.meta, color = colors.tx3)
                    Text(
                        formatCurrency(emiResult.totalPayment),
                        fontWeight = FontWeight.Bold,
                        fontSize = DhruvNextType.title,
                        color = colors.acc,
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
    val colors = LocalDhruvNextColors.current
    val total = principal + interest
    if (total <= 0) return
    val principalSweep = (principal / total * 360f).toFloat()
    val interestSweep = 360f - principalSweep

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = DhruvNextSpacing.inputGroupGap),
        horizontalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(70.dp).padding(4.dp)) {
            val strokeWidth = 16f
            drawArc(
                color = colors.neg,
                startAngle = -90f,
                sweepAngle = interestSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = colors.acc,
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
                Box(modifier = Modifier.size(10.dp).background(colors.acc, RoundedCornerShape(DhruvNextRadii.innerTile / 4)))
                Spacer(modifier = Modifier.width(DhruvNextSpacing.inputGroupGap))
                Text(
                    text = "Principal: ${(principal / total * 100).toInt()}%",
                    fontSize = DhruvNextType.meta,
                    fontWeight = FontWeight.Bold,
                    color = colors.tx,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(colors.neg, RoundedCornerShape(DhruvNextRadii.innerTile / 4)))
                Spacer(modifier = Modifier.width(DhruvNextSpacing.inputGroupGap))
                Text(
                    text = "Interest: ${(interest / total * 100).toInt()}%",
                    fontSize = DhruvNextType.meta,
                    fontWeight = FontWeight.Bold,
                    color = colors.neg,
                )
            }
        }
    }
}

@Composable
fun LoanComparisonCalculator(viewModel: LoansViewModel) {
    var principalAInput by remember { mutableStateOf("1000000") }
    var rateAInput by remember { mutableStateOf("8.5") }
    var tenureAInput by remember { mutableStateOf("15") }

    var principalBInput by remember { mutableStateOf("1000000") }
    var rateBInput by remember { mutableStateOf("9.5") }
    var tenureBInput by remember { mutableStateOf("20") }

    val colors = LocalDhruvNextColors.current
    val principalA = principalAInput.toDoubleOrNull() ?: 0.0
    val rateA = rateAInput.toDoubleOrNull() ?: 0.0
    val tenureA = tenureAInput.toDoubleOrNull() ?: 0.0

    val principalB = principalBInput.toDoubleOrNull() ?: 0.0
    val rateB = rateBInput.toDoubleOrNull() ?: 0.0
    val tenureB = tenureBInput.toDoubleOrNull() ?: 0.0

    val resultA = remember(principalA, rateA, tenureA) { viewModel.calculateEmi(principalA, rateA, tenureA) }
    val resultB = remember(principalB, rateB, tenureB) { viewModel.calculateEmi(principalB, rateB, tenureB) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        SectionLabel(text = "Comparison")

        NxCard {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                Text("Loan A", fontSize = DhruvNextType.cardTitle, fontWeight = FontWeight.Bold, color = colors.acc)
                NxTextField(
                    value = principalAInput,
                    onValueChange = { principalAInput = it },
                    label = "Principal (₹)",
                    prefix = "₹",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                NxTextField(
                    value = rateAInput,
                    onValueChange = { rateAInput = it },
                    label = "Interest Rate (% p.a.)",
                    suffix = "%",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                NxTextField(
                    value = tenureAInput,
                    onValueChange = { tenureAInput = it },
                    label = "Tenure (Years)",
                    suffix = "Yrs",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        NxCard {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                Text("Loan B", fontSize = DhruvNextType.cardTitle, fontWeight = FontWeight.Bold, color = colors.tx3)
                NxTextField(
                    value = principalBInput,
                    onValueChange = { principalBInput = it },
                    label = "Principal (₹)",
                    prefix = "₹",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                NxTextField(
                    value = rateBInput,
                    onValueChange = { rateBInput = it },
                    label = "Interest Rate (% p.a.)",
                    suffix = "%",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                NxTextField(
                    value = tenureBInput,
                    onValueChange = { tenureBInput = it },
                    label = "Tenure (Years)",
                    suffix = "Yrs",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        NxCard {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                Text("Summary", fontSize = DhruvNextType.cardTitle, fontWeight = FontWeight.Bold, color = colors.tx)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Loan A EMI", fontSize = DhruvNextType.meta, color = colors.tx3)
                        Text(
                            formatCurrency(resultA.emi),
                            fontWeight = FontWeight.Bold,
                            fontSize = DhruvNextType.cardTitle,
                            color = colors.acc,
                        )
                        Text("Interest: ${formatCurrency(resultA.totalInterest)}", fontSize = DhruvNextType.meta, color = colors.tx2)
                        Text("Total: ${formatCurrency(resultA.totalPayment)}", fontSize = DhruvNextType.meta, color = colors.tx2)
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Loan B EMI", fontSize = DhruvNextType.meta, color = colors.tx3)
                        Text(
                            formatCurrency(resultB.emi),
                            fontWeight = FontWeight.Bold,
                            fontSize = DhruvNextType.cardTitle,
                            color = colors.tx3,
                        )
                        Text("Interest: ${formatCurrency(resultB.totalInterest)}", fontSize = DhruvNextType.meta, color = colors.tx2)
                        Text("Total: ${formatCurrency(resultB.totalPayment)}", fontSize = DhruvNextType.meta, color = colors.tx2)
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = colors.line)

                val interestDiff = resultA.totalInterest.subtract(resultB.totalInterest)
                val cheaperLabel =
                    if (interestDiff > BigDecimal.ZERO) {
                        "Loan B saves ${formatCurrency(interestDiff)} in interest"
                    } else if (interestDiff < BigDecimal.ZERO) {
                        "Loan A saves ${formatCurrency(interestDiff.negate())} in interest"
                    } else {
                        "Both loans cost the same in interest"
                    }
                Text(cheaperLabel, fontSize = DhruvNextType.body, fontWeight = FontWeight.SemiBold, color = colors.acc)
            }
        }
    }
}

private fun formatCurrency(value: Double): String = CurrencyFormatter.format(value)

private fun formatCurrency(value: BigDecimal): String = CurrencyFormatter.format(value)
