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
fun DateCountdownView() {
    val context = LocalContext.current
    var targetDate by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 45) }) }
    var currentMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    val finalDiffMs = remember(targetDate, currentMillis) {
        val target = targetDate.timeInMillis
        val diff = target - currentMillis
        if (diff < 0) 0L else diff
    }

    val days = TimeUnit.MILLISECONDS.toDays(finalDiffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(finalDiffMs) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(finalDiffMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(finalDiffMs) % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Target Calendar Event Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Pick Target Goal Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> targetDate = Calendar.getInstance().apply { set(y, m, d) } },
                                targetDate.get(Calendar.YEAR), targetDate.get(Calendar.MONTH), targetDate.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US).format(targetDate.time),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Time Remaining To Event Indicator",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CountdownBlock(value = "%02d".format(days), label = "Days")
                    Text(":", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    CountdownBlock(value = "%02d".format(hours), label = "Hrs")
                    Text(":", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    CountdownBlock(value = "%02d".format(minutes), label = "Mins")
                    Text(":", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    CountdownBlock(value = "%02d".format(seconds), label = "Secs")
                }
            }
        }
    }
}

@Composable
fun CountdownBlock(value: String, label: String) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .width(50.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
    }
}
