package com.dhruv.finance.date

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BusinessWorkingDaysView(viewModel: DateViewModel) {
    val context = LocalContext.current
    val colors = LocalDhruvNextColors.current
    var date1 by remember { mutableStateOf(Calendar.getInstance()) }
    var date2 by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }) }

    val businessDaysResult =
        remember(date1, date2) {
            viewModel.calculateBusinessDays(date1, date2)
        }
    val netWorkingDays = businessDaysResult.workingDays
    val weekendDays = businessDaysResult.weekends

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        Text("Evaluate Business Work Week Targets", fontSize = DhruvNextType.cardTitle, fontWeight = FontWeight.Bold, color = colors.tx)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Start Date", fontSize = DhruvNextType.meta, color = colors.tx3)
                NxCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d -> date1 = Calendar.getInstance().apply { set(y, m, d) } },
                                    date1.get(Calendar.YEAR),
                                    date1.get(Calendar.MONTH),
                                    date1.get(Calendar.DAY_OF_MONTH),
                                ).show()
                            },
                ) {
                    Text(
                        text = sdf.format(date1.time),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = colors.tx,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("End Date", fontSize = DhruvNextType.meta, color = colors.tx3)
                NxCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d -> date2 = Calendar.getInstance().apply { set(y, m, d) } },
                                    date2.get(Calendar.YEAR),
                                    date2.get(Calendar.MONTH),
                                    date2.get(Calendar.DAY_OF_MONTH),
                                ).show()
                            },
                ) {
                    Text(
                        text = sdf.format(date2.time),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = colors.tx,
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.accSoft),
            shape = RoundedCornerShape(DhruvNextRadii.card),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Net Workdays (Excluding Saturdays/Sundays)",
                    fontSize = DhruvNextType.meta,
                    color = colors.tx2,
                )
                Text(
                    "$netWorkingDays Working Days",
                    fontSize = DhruvNextType.title,
                    fontWeight = FontWeight.Black,
                    color = colors.acc,
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = DhruvNextSpacing.inputGroupGap),
                    color = colors.line,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Weekend Rest Days", fontSize = DhruvNextType.meta, color = colors.tx3)
                        Text("$weekendDays Days", fontSize = DhruvNextType.body, fontWeight = FontWeight.Bold, color = colors.tx)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val total = netWorkingDays + weekendDays
                        Text("Total Project Timeline", fontSize = DhruvNextType.meta, color = colors.tx3)
                        Text("$total Days", fontSize = DhruvNextType.body, fontWeight = FontWeight.Bold, color = colors.tx)
                    }
                }
            }
        }
    }
}
