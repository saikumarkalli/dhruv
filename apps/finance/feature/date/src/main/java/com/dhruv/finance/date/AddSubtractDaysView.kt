package com.dhruv.finance.date

import android.app.DatePickerDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddSubtractDaysView(viewModel: DateViewModel) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US)

    var calendarDate by remember { mutableStateOf(Calendar.getInstance()) }
    var deltaDaysInput by remember { mutableStateOf("30") }
    var isSubtract by remember { mutableStateOf(false) }

    val computedResult =
        remember(calendarDate, deltaDaysInput, isSubtract) {
            val offset = deltaDaysInput.toIntOrNull() ?: 0
            viewModel.offsetDate(calendarDate, offset, isSubtract)
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Modify Date by custom offsets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Select Base Date
                Text("Select Key Base Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d -> calendarDate = Calendar.getInstance().apply { set(y, m, d) } },
                                    calendarDate.get(Calendar.YEAR),
                                    calendarDate.get(Calendar.MONTH),
                                    calendarDate.get(Calendar.DAY_OF_MONTH),
                                ).show()
                            },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendarDate.time),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Add or Subtract toggle
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(4.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isSubtract) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { isSubtract = false }
                                .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Add Days (+)",
                            color = if (!isSubtract) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSubtract) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { isSubtract = true }
                                .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Subtract Days (-)",
                            color = if (isSubtract) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                }

                // Offset Value field
                OutlinedTextField(
                    value = deltaDaysInput,
                    onValueChange = { deltaDaysInput = it },
                    label = { Text("Offset Days magnitude") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }

        // Calculation Results Display
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
                    "Resulting Calendar coordinates",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Text(
                    text = sdf.format(computedResult),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
