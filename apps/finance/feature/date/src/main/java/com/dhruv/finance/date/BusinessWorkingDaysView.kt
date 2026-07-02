package com.dhruv.finance.date

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BusinessWorkingDaysView(viewModel: DateViewModel) {
    val context = LocalContext.current
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Evaluate Business Work Week Targets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Start Date Input
            Column(modifier = Modifier.weight(1f)) {
                Text("Start Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Card(
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Text(
                        text = sdf.format(date1.time),
                        modifier = Modifier.padding(14.dp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // End Date Input
            Column(modifier = Modifier.weight(1f)) {
                Text("End Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Card(
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Text(
                        text = sdf.format(date2.time),
                        modifier = Modifier.padding(14.dp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Net Workdays (Excluding Saturdays/Sundays)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Text(
                    "$netWorkingDays Working Days",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Weekend Rest Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text("$weekendDays Days", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val total = netWorkingDays + weekendDays
                        Text(
                            "Total Project Timeline",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text("$total Days", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
