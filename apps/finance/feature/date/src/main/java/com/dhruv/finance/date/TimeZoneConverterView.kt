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
import com.dhruv.core.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.util.*

@Composable
fun TimeZoneConverterView(viewModel: DateViewModel) {
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

    var sourceZone by remember { mutableStateOf(timeZones[3]) } // default IST (Asia/Calcutta)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Convert World Time Coordinates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Source Parameters", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)

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

                Text("Select Source Timezone", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                // Dropdown mock for simplicity and fast compile (row buttons)
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
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
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { sourceZone = zone }
                                    .padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = zone.id.substringAfter("/"),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
        }

        // Result displays
        Text("Calculated World Clocks", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

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
                        containerColor =
                            if (isMain) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                                    .copy(
                                        alpha = 0.4f,
                                    )
                            },
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(zone.id, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        val offsetStr =
                            try {
                                zone.rules.getOffset(Instant.now()).toString()
                            } catch (e: Exception) {
                                "UTC"
                            }
                        Text("Offset: $offsetStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                    Text(
                        text = displayTime,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
