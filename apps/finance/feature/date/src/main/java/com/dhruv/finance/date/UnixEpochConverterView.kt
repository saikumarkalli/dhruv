package com.dhruv.finance.date

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import com.dhruv.core.ui.theme.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import com.dhruv.settings.SettingsRepository
import org.koin.compose.koinInject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Composable
fun UnixEpochConverterView(viewModel: DateViewModel) {
    var unixInput by remember { mutableStateOf("1779532800") } // May 23, 2026 default
    var customYear by remember { mutableStateOf("2026") }
    var customMonth by remember { mutableStateOf("05") }
    var customDay by remember { mutableStateOf("23") }
    var customHour by remember { mutableStateOf("18") }
    var customMin by remember { mutableStateOf("00") }

    var isTimestampToDateMode by remember { mutableStateOf(true) }

    val formattedDateResult = remember(unixInput, isTimestampToDateMode) {
        val seconds = unixInput.toLongOrNull() ?: 0L
        viewModel.unixTimestampToDateString(seconds)
    }

    val computedUnixResult = remember(customYear, customMonth, customDay, customHour, customMin, isTimestampToDateMode) {
        val yr = customYear.toIntOrNull() ?: 2026
        val mt = customMonth.toIntOrNull() ?: 5
        val dy = customDay.toIntOrNull() ?: 23
        val hr = customHour.toIntOrNull() ?: 18
        val mn = customMin.toIntOrNull() ?: 0
        val seconds = viewModel.dateComponentsToUnixTimestamp(yr, mt, dy, hr, mn)
        "Generated Unix epoch timestamp:\n$seconds"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Two-way Unix Epoch Translation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isTimestampToDateMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { isTimestampToDateMode = true }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Timestamp to Date", color = if (isTimestampToDateMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (!isTimestampToDateMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { isTimestampToDateMode = false }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Date to Timestamp", color = if (!isTimestampToDateMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        if (isTimestampToDateMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter Epoch Timestamp (Seconds)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    OutlinedTextField(
                        value = unixInput,
                        onValueChange = { unixInput = it },
                        label = { Text("Unix Time Integer") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Human Readable date equivalent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formattedDateResult,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 22.sp
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Specify Date Parameters (UTC)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = customYear, onValueChange = { customYear = it }, label = { Text("Year") }, modifier = Modifier.weight(1.5f))
                        OutlinedTextField(value = customMonth, onValueChange = { customMonth = it }, label = { Text("Month") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = customDay, onValueChange = { customDay = it }, label = { Text("Day") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = customHour, onValueChange = { customHour = it }, label = { Text("Hour (UTC)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = customMin, onValueChange = { customMin = it }, label = { Text("Minute") }, modifier = Modifier.weight(1f))
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Resulting Unix Timestamp", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = computedUnixResult,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
