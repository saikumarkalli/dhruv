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
fun DateScreen(
    viewModel: DateViewModel,
    settingsRepository: SettingsRepository = koinInject(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeSubCalculator by viewModel.activeSubCalculator.collectAsStateWithLifecycle()
    
    // Sub-calculators titles and icons
    val subCalculators = listOf(
        DateCalcItem("Date Difference", Icons.Default.DateRange, "Find duration between two calendar dates."),
        DateCalcItem("Add / Subtract Days", Icons.Default.Event, "Move calendar dates forwards or backwards in time."),
        DateCalcItem("Age Calculator", Icons.Default.Cake, "Breakdown of years, months, days, and next birthday countdown."),
        DateCalcItem("Countdown Tracker", Icons.Default.Timelapse, "Live countdown of days and hours to absolute goals."),
        DateCalcItem("Time Zone Converter", Icons.Default.Language, "Quick conversion between world coordinate UTC positions."),
        DateCalcItem("Business Working Days", Icons.Default.Work, "Count weekdays excluding standard Saturdays & Sundays."),
        DateCalcItem("Unix Epoch Converter", Icons.Default.Terminal, "Parse integer timestamp seconds to UTC format and vice versa.")
    )

    val visibleSubCalculators by remember(subCalculators) {
        combine(
            subCalculators.map { item ->
                settingsRepository.isToolEnabled(item.name).map { item to it }
            }
        ) { array ->
            array.filter { it.second }.map { it.first }
        }
    }.collectAsState(initial = subCalculators)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (activeSubCalculator == null) {
            // Main Library Grid View (Matches Screenshot Redesign perfectly!)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Date & Time Calculations",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    text = "Select a tool to begin calculations",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // 3-Column beautiful responsive grid
                val rows = visibleSubCalculators.chunked(3)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            row.forEach { item ->
                                val index = subCalculators.indexOf(item)
                                 GridDateItemCard(
                                     item = item,
                                     onClick = { viewModel.setActiveSubCalculator(index) },
                                     modifier = Modifier.weight(1f)
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         IconButton(onClick = { viewModel.setActiveSubCalculator(null) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to list",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            val activeItem = subCalculators[activeSubCalculator ?: 0]
                            Text(
                                text = activeItem.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = activeItem.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp)
            .testTag("grid_item_${item.name.lowercase().replace(" ", "_").replace("/", "and")}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
    }
}

data class DateCalcItem(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val description: String)

@Composable
fun ActiveSubCalcRender(index: Int, viewModel: DateViewModel) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

@Composable
fun DateDifferenceView(viewModel: DateViewModel) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    var date1 by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -10) }) }
    var date2 by remember { mutableStateOf(Calendar.getInstance()) }

    val diffResult = remember(date1, date2) {
        viewModel.calculateDifference(date1, date2)
    }

    val totalDays = diffResult.totalDays
    val yearsParts = diffResult.years
    val monthsParts = diffResult.months
    val remainingDaysParts = diffResult.days

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Evaluate the distance between two dates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start Date Input
            Column(modifier = Modifier.weight(1f)) {
                Text("Start Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> date1 = Calendar.getInstance().apply { set(y, m, d) } },
                                date1.get(Calendar.YEAR), date1.get(Calendar.MONTH), date1.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = sdf.format(date1.time),
                        modifier = Modifier.padding(14.dp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // End Date Input
            Column(modifier = Modifier.weight(1f)) {
                Text("End Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> date2 = Calendar.getInstance().apply { set(y, m, d) } },
                                date2.get(Calendar.YEAR), date2.get(Calendar.MONTH), date2.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = sdf.format(date2.time),
                        modifier = Modifier.padding(14.dp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Output Result card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Primary Duration Metric",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    "$totalDays Days",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                Text(
                    "Equivalent Chronological Breakdown",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DateStatChip(valStr = "$yearsParts", denomStr = "Years")
                    DateStatChip(valStr = "$monthsParts", denomStr = "Months")
                    DateStatChip(valStr = "$remainingDaysParts", denomStr = "Days")
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Weeks equivalent: ${(totalDays / 7)} Weeks, ${(totalDays % 7)} Days",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun DateStatChip(valStr: String, denomStr: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(valStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(denomStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun AddSubtractDaysView(viewModel: DateViewModel) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US)
    
    var calendarDate by remember { mutableStateOf(Calendar.getInstance()) }
    var deltaDaysInput by remember { mutableStateOf("30") }
    var isSubtract by remember { mutableStateOf(false) }

    val computedResult = remember(calendarDate, deltaDaysInput, isSubtract) {
        val offset = deltaDaysInput.toIntOrNull() ?: 0
        viewModel.offsetDate(calendarDate, offset, isSubtract)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Modify Date by custom offsets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Select Base Date
                Text("Select Key Base Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> calendarDate = Calendar.getInstance().apply { set(y, m, d) } },
                                calendarDate.get(Calendar.YEAR), calendarDate.get(Calendar.MONTH), calendarDate.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendarDate.time),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Add or Subtract toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isSubtract) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { isSubtract = false }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Add Days (+)", color = if (!isSubtract) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSubtract) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { isSubtract = true }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Subtract Days (-)", color = if (isSubtract) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Calculation Results Display
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
                    "Resulting Calendar coordinates",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = sdf.format(computedResult),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun AgeCalculatorView(viewModel: DateViewModel) {
    val context = LocalContext.current
    val sdfDisplay = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    
    var birthDate by remember { mutableStateOf(Calendar.getInstance().apply { set(2000, 0, 1) }) }
    var referenceDate by remember { mutableStateOf(Calendar.getInstance()) }

    val ageResult = remember(birthDate, referenceDate) {
        viewModel.calculateAge(birthDate, referenceDate)
    }

    val ageYears = ageResult.years
    val ageMonths = ageResult.months
    val ageDays = ageResult.days
    val nextMonths = ageResult.nextMonths
    val nextDays = ageResult.nextDays
    val dayOfWeekOfNextBirthday = ageResult.dayOfWeekOfNextBirthday

    val totalDays = ageResult.totalDays
    val totalMonths = ageResult.totalMonths
    val totalWeeks = ageResult.totalWeeks
    val totalHours = ageResult.totalHours
    val totalMinutes = ageResult.totalMinutes

    val miuiOrange = Color(0xFFFF5C00) // Deep Premium MIUI-style Orange
    val textDark = Color(0xFF212121)   // Dark Grey/Black for values
    val textGrey = Color(0xFF757575)   // Muted Grey for labels
    val dividerGrey = Color(0xFFE5E5E5) // Light line separation grey

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date Selectors exactly like the screenshot
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> birthDate = Calendar.getInstance().apply { set(y, m, d) } },
                            birthDate.get(Calendar.YEAR), birthDate.get(Calendar.MONTH), birthDate.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date of birth",
                    style = MaterialTheme.typography.bodyLarge,
                    color = textDark
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sdfDisplay.format(birthDate.time),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = miuiOrange
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select birth date",
                        tint = textGrey,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = dividerGrey.copy(alpha = 0.5f), thickness = 0.5.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> referenceDate = Calendar.getInstance().apply { set(y, m, d) } },
                            referenceDate.get(Calendar.YEAR), referenceDate.get(Calendar.MONTH), referenceDate.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.bodyLarge,
                    color = textDark
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sdfDisplay.format(referenceDate.time),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = textGrey
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select reference date",
                        tint = textGrey,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Output Result card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, dividerGrey),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left block - Age
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Age",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Normal, 
                                fontSize = 32.sp
                            ),
                            color = textGrey
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "$ageYears",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 72.sp
                                ),
                                color = miuiOrange
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "years",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textGrey,
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "$ageMonths months | $ageDays days",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textGrey
                        )
                    }
                    
                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(130.dp)
                            .background(dividerGrey)
                    )
                    
                    // Right block - Next Birthday
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Next birthday",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = miuiOrange
                        )
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(miuiOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cake,
                                contentDescription = "Cake icon",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = dayOfWeekOfNextBirthday,
                            style = MaterialTheme.typography.titleMedium,
                            color = textGrey
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "$nextMonths months | $nextDays days",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textGrey
                        )
                    }
                }
                
                HorizontalDivider(color = dividerGrey)
                
                // Summary heading and Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = miuiOrange
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Row 1: Years, Months, Weeks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryGridItem(
                            label = "Years", 
                            value = "$ageYears", 
                            modifier = Modifier.weight(1f),
                            textGrey = textGrey,
                            textDark = textDark
                        )
                        SummaryGridItem(
                            label = "Months", 
                            value = "$totalMonths", 
                            modifier = Modifier.weight(1f),
                            textGrey = textGrey,
                            textDark = textDark
                        )
                        SummaryGridItem(
                            label = "Weeks", 
                            value = "$totalWeeks", 
                            modifier = Modifier.weight(1f),
                            textGrey = textGrey,
                            textDark = textDark
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Row 2: Days, Hours, Minutes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryGridItem(
                            label = "Days", 
                            value = "$totalDays", 
                            modifier = Modifier.weight(1f),
                            textGrey = textGrey,
                            textDark = textDark
                        )
                        SummaryGridItem(
                            label = "Hours", 
                            value = "$totalHours", 
                            modifier = Modifier.weight(1f),
                            textGrey = textGrey,
                            textDark = textDark
                        )
                        SummaryGridItem(
                            label = "Minutes", 
                            value = "$totalMinutes", 
                            modifier = Modifier.weight(1f),
                            textGrey = textGrey,
                            textDark = textDark
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "powered by Calculator",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = textGrey.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Bottom Actions Button Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "Add to Calendar",
                    color = textGrey,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
            }
            
            Button(
                onClick = { },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = miuiOrange),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "Share",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

@Composable
fun SummaryGridItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    textGrey: Color,
    textDark: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textGrey
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = textDark
        )
    }
}

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

@Composable
fun TimeZoneConverterView(viewModel: DateViewModel) {
    var sourceHour by remember { mutableStateOf("12") }
    var sourceMinute by remember { mutableStateOf("00") }
    
    val timeZones = listOf(
        ZoneId.of("UTC"),
        ZoneId.of("America/New_York"),
        ZoneId.of("Europe/London"),
        ZoneId.of("Asia/Calcutta"),
        ZoneId.of("Asia/Tokyo"),
        ZoneId.of("Australia/Sydney")
    )
    
    var sourceZone by remember { mutableStateOf(timeZones[3]) } // default IST (Asia/Calcutta)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Convert World Time Coordinates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Source Parameters", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sourceHour,
                        onValueChange = { sourceHour = it },
                        label = { Text("Hour (24hr)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sourceMinute,
                        onValueChange = { sourceMinute = it },
                        label = { Text("Minute") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text("Select Source Timezone", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                // Dropdown mock for simplicity and fast compile (row buttons)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    val quickZones = listOf(ZoneId.of("Asia/Calcutta"), ZoneId.of("UTC"), ZoneId.of("America/New_York"))
                    quickZones.forEach { zone ->
                        val isSelected = sourceZone.id == zone.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { sourceZone = zone }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = zone.id.substringAfter("/"),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
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

            val displayTime = remember(sourceHour, sourceMinute, sourceZone, zone) {
                val srcHourInt = sourceHour.toIntOrNull() ?: 12
                val srcMinInt = sourceMinute.toIntOrNull() ?: 0
                viewModel.convertTimeZone(srcHourInt, srcMinInt, sourceZone, zone)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMain) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(zone.id, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        val offsetStr = try { zone.rules.getOffset(Instant.now()).toString() } catch (e: Exception) { "UTC" }
                        Text("Offset: $offsetStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                    Text(
                        text = displayTime,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun BusinessWorkingDaysView(viewModel: DateViewModel) {
    val context = LocalContext.current
    var date1 by remember { mutableStateOf(Calendar.getInstance()) }
    var date2 by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }) }

    val businessDaysResult = remember(date1, date2) {
        viewModel.calculateBusinessDays(date1, date2)
    }
    val netWorkingDays = businessDaysResult.workingDays
    val weekendDays = businessDaysResult.weekends

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Evaluate Business Work Week Targets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start Date Input
            Column(modifier = Modifier.weight(1f)) {
                Text("Start Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> date1 = Calendar.getInstance().apply { set(y, m, d) } },
                                date1.get(Calendar.YEAR), date1.get(Calendar.MONTH), date1.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = sdf.format(date1.time),
                        modifier = Modifier.padding(14.dp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // End Date Input
            Column(modifier = Modifier.weight(1f)) {
                Text("End Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> date2 = Calendar.getInstance().apply { set(y, m, d) } },
                                date2.get(Calendar.YEAR), date2.get(Calendar.MONTH), date2.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = sdf.format(date2.time),
                        modifier = Modifier.padding(14.dp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
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
                    "Net Workdays (Excluding Saturdays/Sundays)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    "$netWorkingDays Working Days",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Weekend Rest Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text("$weekendDays Days", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val total = netWorkingDays + weekendDays
                        Text("Total Project Timeline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text("$total Days", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

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
