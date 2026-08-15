package com.dhruv.finance.date

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
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import java.time.Instant
import java.time.ZoneId

@Composable
fun TimeZoneConverterView(viewModel: DateViewModel) {
    val colors = LocalDhruvNextColors.current
    var sourceHour by remember { mutableStateOf("12") }
    var sourceMinute by remember { mutableStateOf("00") }

    val timeZones =
        listOf(
            ZoneId.of("UTC"),
            ZoneId.of("America/New_York"),
            ZoneId.of("Europe/London"),
            ZoneId.of("Asia/Calcutta"),
            ZoneId.of("Asia/Tokyo"),
            ZoneId.of("Australia/Sydney"),
        )

    var sourceZone by remember { mutableStateOf(timeZones[3]) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap),
    ) {
        Text("Convert World Time Coordinates", fontSize = DhruvNextType.cardTitle, fontWeight = FontWeight.Bold, color = colors.tx)

        NxCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Source Parameters", fontSize = DhruvNextType.meta, color = colors.tx3)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = sourceHour,
                        onValueChange = { sourceHour = it },
                        label = { Text("Hour (24hr)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = sourceMinute,
                        onValueChange = { sourceMinute = it },
                        label = { Text("Minute") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }

                Text("Select Source Timezone", fontSize = DhruvNextType.meta, color = colors.tx3)
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(colors.surf, RoundedCornerShape(DhruvNextRadii.innerTile))
                            .padding(4.dp),
                ) {
                    val quickZones = listOf(ZoneId.of("Asia/Calcutta"), ZoneId.of("UTC"), ZoneId.of("America/New_York"))
                    quickZones.forEach { zone ->
                        val isSelected = sourceZone.id == zone.id
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) colors.acc else Color.Transparent)
                                    .clickable { sourceZone = zone }
                                    .padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = zone.id.substringAfter("/"),
                                color = if (isSelected) colors.onAcc else colors.tx,
                                fontWeight = FontWeight.Bold,
                                fontSize = DhruvNextType.meta,
                            )
                        }
                    }
                }
            }
        }

        Text("Calculated World Clocks", fontSize = DhruvNextType.meta, fontWeight = FontWeight.Bold, color = colors.tx)

        timeZones.forEach { zone ->
            val isMain = zone.id == sourceZone.id

            val displayTime =
                remember(sourceHour, sourceMinute, sourceZone, zone) {
                    val srcHourInt = sourceHour.toIntOrNull() ?: 12
                    val srcMinInt = sourceMinute.toIntOrNull() ?: 0
                    viewModel.convertTimeZone(srcHourInt, srcMinInt, sourceZone, zone)
                }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = if (isMain) colors.accSoft else colors.surf2,
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(DhruvNextSpacing.inputGroupGap),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(zone.id, fontWeight = FontWeight.Bold, fontSize = DhruvNextType.body, color = colors.tx)
                        val offsetStr =
                            try {
                                zone.rules.getOffset(Instant.now()).toString()
                            } catch (e: Exception) {
                                "UTC"
                            }
                        Text("Offset: $offsetStr", fontSize = DhruvNextType.meta, color = colors.tx3)
                    }
                    Text(
                        text = displayTime,
                        fontWeight = FontWeight.Black,
                        fontSize = DhruvNextType.body,
                        color = colors.acc,
                    )
                }
            }
        }
    }
}
