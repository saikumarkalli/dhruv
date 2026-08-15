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
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddSubtractDaysView(viewModel: DateViewModel) {
    val context = LocalContext.current
    val colors = LocalDhruvNextColors.current
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
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        Text("Modify Date by custom offsets", fontSize = DhruvNextType.cardTitle, fontWeight = FontWeight.Bold, color = colors.tx)

        NxCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap)) {
                Text("Select Key Base Date", fontSize = DhruvNextType.meta, color = colors.tx3)
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
                    colors = CardDefaults.cardColors(containerColor = colors.surf),
                ) {
                    Row(
                        modifier = Modifier.padding(DhruvNextSpacing.inputGroupGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = colors.acc)
                        Spacer(modifier = Modifier.width(DhruvNextSpacing.inputGroupGap))
                        Text(
                            text = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendarDate.time),
                            fontWeight = FontWeight.Bold,
                            color = colors.tx,
                        )
                    }
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(DhruvNextRadii.innerTile))
                            .background(colors.surf)
                            .padding(4.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isSubtract) colors.acc else Color.Transparent)
                                .clickable { isSubtract = false }
                                .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Add Days (+)",
                            color = if (!isSubtract) colors.onAcc else colors.tx,
                            fontWeight = FontWeight.Bold,
                            fontSize = DhruvNextType.meta,
                        )
                    }
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSubtract) colors.acc else Color.Transparent)
                                .clickable { isSubtract = true }
                                .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Subtract Days (-)",
                            color = if (isSubtract) colors.onAcc else colors.tx,
                            fontWeight = FontWeight.Bold,
                            fontSize = DhruvNextType.meta,
                        )
                    }
                }

                OutlinedTextField(
                    value = deltaDaysInput,
                    onValueChange = { deltaDaysInput = it },
                    label = { Text("Offset Days magnitude") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(DhruvNextRadii.innerTile),
                )
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
                    "Resulting Calendar coordinates",
                    fontSize = DhruvNextType.meta,
                    color = colors.tx2,
                )
                Text(
                    text = sdf.format(computedResult),
                    fontSize = DhruvNextType.cardTitle,
                    fontWeight = FontWeight.Black,
                    color = colors.acc,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
