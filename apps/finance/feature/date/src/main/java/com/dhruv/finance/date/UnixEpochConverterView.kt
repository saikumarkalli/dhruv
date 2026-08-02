package com.dhruv.finance.date

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
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
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import java.util.*

@Composable
fun UnixEpochConverterView(viewModel: DateViewModel) {
    val colors = LocalDhruvNextColors.current
    var unixInput by remember { mutableStateOf("1779532800") } // May 23, 2026 default
    var customYear by remember { mutableStateOf("2026") }
    var customMonth by remember { mutableStateOf("05") }
    var customDay by remember { mutableStateOf("23") }
    var customHour by remember { mutableStateOf("18") }
    var customMin by remember { mutableStateOf("00") }

    var isTimestampToDateMode by remember { mutableStateOf(true) }

    val formattedDateResult =
        remember(unixInput, isTimestampToDateMode) {
            val seconds = unixInput.toLongOrNull() ?: 0L
            viewModel.unixTimestampToDateString(seconds)
        }

    val computedUnixResult =
        remember(customYear, customMonth, customDay, customHour, customMin, isTimestampToDateMode) {
            val yr = customYear.toIntOrNull() ?: 2026
            val mt = customMonth.toIntOrNull() ?: 5
            val dy = customDay.toIntOrNull() ?: 23
            val hr = customHour.toIntOrNull() ?: 18
            val mn = customMin.toIntOrNull() ?: 0
            val seconds = viewModel.dateComponentsToUnixTimestamp(yr, mt, dy, hr, mn)
            "Generated Unix epoch timestamp:\n$seconds"
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Two-way Unix Epoch Translation", fontSize = DhruvNextType.cardTitle, fontWeight = FontWeight.Bold, color = colors.tx)

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DhruvNextRadii.innerTile))
                    .background(colors.surf2)
                    .padding(4.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isTimestampToDateMode) colors.acc else Color.Transparent)
                        .clickable { isTimestampToDateMode = true }
                        .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Timestamp to Date",
                    color = if (isTimestampToDateMode) colors.onAcc else colors.tx,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (!isTimestampToDateMode) colors.acc else Color.Transparent)
                        .clickable { isTimestampToDateMode = false }
                        .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Date to Timestamp",
                    color = if (!isTimestampToDateMode) colors.onAcc else colors.tx,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
        }

        if (isTimestampToDateMode) {
            NxCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter Epoch Timestamp (Seconds)",
                        fontSize = DhruvNextType.meta,
                        color = colors.tx3,
                    )
                    OutlinedTextField(
                        value = unixInput,
                        onValueChange = { unixInput = it },
                        label = { Text("Unix Time Integer") },
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
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Human Readable date equivalent",
                        fontSize = DhruvNextType.meta,
                        color = colors.tx2,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formattedDateResult,
                        fontSize = DhruvNextType.body,
                        fontWeight = FontWeight.Bold,
                        color = colors.acc,
                        lineHeight = 22.sp,
                    )
                }
            }
        } else {
            NxCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Specify Date Parameters (UTC)",
                        fontSize = DhruvNextType.meta,
                        color = colors.tx3,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = customYear,
                            onValueChange = { customYear = it },
                            label = { Text("Year") },
                            modifier = Modifier.weight(1.5f),
                        )
                        OutlinedTextField(
                            value = customMonth,
                            onValueChange = { customMonth = it },
                            label = { Text("Month") },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = customDay,
                            onValueChange = { customDay = it },
                            label = { Text("Day") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = customHour,
                            onValueChange = { customHour = it },
                            label = { Text("Hour (UTC)") },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = customMin,
                            onValueChange = { customMin = it },
                            label = { Text("Minute") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.accSoft),
                shape = RoundedCornerShape(DhruvNextRadii.card),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Resulting Unix Timestamp",
                        fontSize = DhruvNextType.meta,
                        color = colors.tx2,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = computedUnixResult,
                        fontSize = DhruvNextType.body,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.acc,
                    )
                }
            }
        }
    }
}
