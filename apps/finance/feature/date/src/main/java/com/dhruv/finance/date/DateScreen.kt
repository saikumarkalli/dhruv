package com.dhruv.finance.date

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.ui.theme.*
import com.dhruv.settings.SettingsRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import java.util.*

@Composable
fun DateScreen(
    viewModel: DateViewModel,
    settingsRepository: SettingsRepository = koinInject(),
    modifier: Modifier = Modifier,
) {
    val activeSubCalculator by viewModel.activeSubCalculator.collectAsStateWithLifecycle()

    // Sub-calculators titles and icons
    val subCalculators =
        listOf(
            DateCalcItem("Date Difference", Icons.Default.DateRange, "Find duration between two calendar dates."),
            DateCalcItem("Add / Subtract Days", Icons.Default.Event, "Move calendar dates forwards or backwards in time."),
            DateCalcItem("Age Calculator", Icons.Default.Cake, "Breakdown of years, months, days, and next birthday countdown."),
            DateCalcItem("Countdown Tracker", Icons.Default.Timelapse, "Live countdown of days and hours to absolute goals."),
            DateCalcItem("Time Zone Converter", Icons.Default.Language, "Quick conversion between world coordinate UTC positions."),
            DateCalcItem("Business Working Days", Icons.Default.Work, "Count weekdays excluding standard Saturdays & Sundays."),
            DateCalcItem("Unix Epoch Converter", Icons.Default.Terminal, "Parse integer timestamp seconds to UTC format and vice versa."),
        )

    val visibleSubCalculators by remember(subCalculators) {
        combine(
            subCalculators.map { item ->
                settingsRepository.isToolEnabled(item.name).map { item to it }
            },
        ) { array ->
            array.filter { it.second }.map { it.first }
        }
    }.collectAsState(initial = subCalculators)

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        if (activeSubCalculator == null) {
            // Main Library Grid View (Matches Screenshot Redesign perfectly!)
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Date & Time Calculations",
                    style =
                        MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                        ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 6.dp),
                )

                Text(
                    text = "Select a tool to begin calculations",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 24.dp),
                )

                // 3-Column beautiful responsive grid
                val rows = visibleSubCalculators.chunked(3)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            row.forEach { item ->
                                val index = subCalculators.indexOf(item)
                                GridDateItemCard(
                                    item = item,
                                    onClick = { viewModel.setActiveSubCalculator(index) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            // Fill remaining space if row is not full
                            if (row.size < 3) {
                                repeat(3 - row.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Active Tool Container View (with Elegant Top App Bar)
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { viewModel.setActiveSubCalculator(null) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to list",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            val activeItem = subCalculators[activeSubCalculator ?: 0]
                            Text(
                                text = activeItem.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = activeItem.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            )
                        }
                    }
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                ) {
                    ActiveSubCalcRender(activeSubCalculator ?: 0, viewModel)
                }
            }
        }
    }
}

@Composable
fun GridDateItemCard(
    item: DateCalcItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp, horizontal = 8.dp)
                .testTag("grid_item_${item.name.lowercase().replace(" ", "_").replace("/", "and")}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(60.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = item.name,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )
    }
}

data class DateCalcItem(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String,
)

@Composable
fun ActiveSubCalcRender(
    index: Int,
    viewModel: DateViewModel,
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (index) {
                0 -> DateDifferenceView(viewModel)
                1 -> AddSubtractDaysView(viewModel)
                2 -> AgeCalculatorView(viewModel)
                3 -> DateCountdownView()
                4 -> TimeZoneConverterView(viewModel)
                5 -> BusinessWorkingDaysView(viewModel)
                6 -> UnixEpochConverterView(viewModel)
            }
        }
    }
}
