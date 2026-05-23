package com.example.ui.date

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
import com.example.ui.theme.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@Composable
fun DateScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeSubCalculator by remember { mutableIntStateOf(0) }
    
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

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 720

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Applet Header
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Date & Time Calculations",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = ResponsiveApp.typography.titleLarge),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Professional high-precision time coordinates",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveApp.typography.labelSmall),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (isWideScreen) {
            // Adaptive Grid/Detail for large devices (Tablet, landscape, folding screens)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Panel: Menu Cards
                Card(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Calculator Library",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                        )
                        subCalculators.forEachIndexed { index, item ->
                            val isSelected = activeSubCalculator == index
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { activeSubCalculator = index }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Panel: Calculations Display Space
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        ActiveSubCalcRender(activeSubCalculator)
                    }
                }
            }
        } else {
            // Mobile scrolling view
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                ScrollableTabRow(
                    selectedTabIndex = activeSubCalculator,
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = { tabPositions ->
                        if (activeSubCalculator < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubCalculator]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    subCalculators.forEachIndexed { index, item ->
                        val isSelected = activeSubCalculator == index
                        Tab(
                            selected = isSelected,
                            onClick = { activeSubCalculator = index },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = item.name,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    ActiveSubCalcRender(activeSubCalculator)
                }
            }
        }
    }
}

data class DateCalcItem(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val description: String)

@Composable
fun ActiveSubCalcRender(index: Int) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (index) {
                0 -> DateDifferenceView()
                1 -> AddSubtractDaysView()
                2 -> AgeCalculatorView()
                3 -> DateCountdownView()
                4 -> TimeZoneConverterView()
                5 -> BusinessWorkingDaysView()
                6 -> UnixEpochConverterView()
            }
        }
    }
}

@Composable
fun DateDifferenceView() {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    var date1 by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -10) }) }
    var date2 by remember { mutableStateOf(Calendar.getInstance()) }

    var totalDays by remember { mutableLongStateOf(0) }
    var yearsParts by remember { mutableIntStateOf(0) }
    var monthsParts by remember { mutableIntStateOf(0) }
    var remainingDaysParts by remember { mutableIntStateOf(0) }

    fun calculateDiff() {
        val d1 = date1.timeInMillis
        val d2 = date2.timeInMillis
        val diffMs = Math.abs(d2 - d1)
        totalDays = TimeUnit.MILLISECONDS.toDays(diffMs)

        val calStart = if (date1.before(date2)) date1.clone() as Calendar else date2.clone() as Calendar
        val calEnd = if (date1.before(date2)) date2.clone() as Calendar else date1.clone() as Calendar

        var yrs = calEnd.get(Calendar.YEAR) - calStart.get(Calendar.YEAR)
        var mths = calEnd.get(Calendar.MONTH) - calStart.get(Calendar.MONTH)
        var dys = calEnd.get(Calendar.DAY_OF_MONTH) - calStart.get(Calendar.DAY_OF_MONTH)

        if (dys < 0) {
            mths -= 1
            val tempCal = calStart.clone() as Calendar
            tempCal.add(Calendar.MONTH, yrs * 12 + mths)
            val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            dys += daysInMonth
        }
        if (mths < 0) {
            yrs -= 1
            mths += 12
        }

        yearsParts = yrs
        monthsParts = mths
        remainingDaysParts = dys
    }

    LaunchedEffect(date1, date2) {
        calculateDiff()
    }

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
fun AddSubtractDaysView() {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US)
    
    var calendarDate by remember { mutableStateOf(Calendar.getInstance()) }
    var deltaDaysInput by remember { mutableStateOf("30") }
    var isSubtract by remember { mutableStateOf(false) }

    val computedResult = remember(calendarDate, deltaDaysInput, isSubtract) {
        val offset = deltaDaysInput.toIntOrNull() ?: 0
        val clone = calendarDate.clone() as Calendar
        if (isSubtract) {
            clone.add(Calendar.DAY_OF_YEAR, -offset)
        } else {
            clone.add(Calendar.DAY_OF_YEAR, offset)
        }
        clone.time
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
fun AgeCalculatorView() {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    var birthDate by remember { mutableStateOf(Calendar.getInstance().apply { set(2000, 0, 1) }) }
    var referenceDate by remember { mutableStateOf(Calendar.getInstance()) }

    var ageYears by remember { mutableIntStateOf(0) }
    var ageMonths by remember { mutableIntStateOf(0) }
    var ageDays by remember { mutableIntStateOf(0) }
    var nextBirthdayCountdown by remember { mutableLongStateOf(0) }

    fun calculateAge() {
        if (birthDate.after(referenceDate)) {
            ageYears = 0
            ageMonths = 0
            ageDays = 0
            nextBirthdayCountdown = 0
            return
        }

        var yrs = referenceDate.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
        var mths = referenceDate.get(Calendar.MONTH) - birthDate.get(Calendar.MONTH)
        var dys = referenceDate.get(Calendar.DAY_OF_MONTH) - birthDate.get(Calendar.DAY_OF_MONTH)

        if (dys < 0) {
            mths -= 1
            val tempCal = birthDate.clone() as Calendar
            tempCal.add(Calendar.MONTH, yrs * 12 + mths)
            val daysInBirthMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            dys += daysInBirthMonth
        }
        if (mths < 0) {
            yrs -= 1
            mths += 12
        }

        ageYears = yrs
        ageMonths = mths
        ageDays = dys

        // Next birthday calculation
        val nextBd = birthDate.clone() as Calendar
        nextBd.set(Calendar.YEAR, referenceDate.get(Calendar.YEAR))
        if (nextBd.before(referenceDate) || nextBd.equals(referenceDate)) {
            nextBd.add(Calendar.YEAR, 1)
        }
        val diffMs = nextBd.timeInMillis - referenceDate.timeInMillis
        nextBirthdayCountdown = TimeUnit.MILLISECONDS.toDays(diffMs)
    }

    LaunchedEffect(birthDate, referenceDate) {
        calculateAge()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Detailed breakdown of age parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Birth date selector
            Column(modifier = Modifier.weight(1f)) {
                Text("Date of Birth", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> birthDate = Calendar.getInstance().apply { set(y, m, d) } },
                                birthDate.get(Calendar.YEAR), birthDate.get(Calendar.MONTH), birthDate.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = sdf.format(birthDate.time),
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Reference date selector
            Column(modifier = Modifier.weight(1f)) {
                Text("Today / Reference Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> referenceDate = Calendar.getInstance().apply { set(y, m, d) } },
                                referenceDate.get(Calendar.YEAR), referenceDate.get(Calendar.MONTH), referenceDate.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = sdf.format(referenceDate.time),
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Output Result card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Calculated Age Breakdown",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DateStatChip(valStr = "$ageYears", denomStr = "Years")
                    DateStatChip(valStr = "$ageMonths", denomStr = "Months")
                    DateStatChip(valStr = "$ageDays", denomStr = "Days")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Next Birthday in", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text("$nextBirthdayCountdown Days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val totalLivedDays = TimeUnit.MILLISECONDS.toDays(Math.abs(referenceDate.timeInMillis - birthDate.timeInMillis))
                        Text("Total Lived", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text("$totalLivedDays Days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
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
fun TimeZoneConverterView() {
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

        val srcHourInt = sourceHour.toIntOrNull() ?: 12
        val srcMinInt = sourceMinute.toIntOrNull() ?: 0

        timeZones.forEach { zone ->
            val formatter = DateTimeFormatter.ofPattern("hh:mm a (EEEE)", Locale.US)
            val isMain = zone.id == sourceZone.id

            val displayTime = try {
                val now = Calendar.getInstance()
                val zonedDateTime = ZonedDateTime.of(
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH) + 1,
                    now.get(Calendar.DAY_OF_MONTH),
                    srcHourInt.coerceIn(0, 23),
                    srcMinInt.coerceIn(0, 59),
                    0, 0, sourceZone
                )
                val convertedTime = zonedDateTime.withZoneSameInstant(zone)
                convertedTime.format(formatter)
            } catch (e: Exception) {
                "---"
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
fun BusinessWorkingDaysView() {
    val context = LocalContext.current
    var date1 by remember { mutableStateOf(Calendar.getInstance()) }
    var date2 by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }) }

    var netWorkingDays by remember { mutableIntStateOf(0) }
    var weekendDays by remember { mutableIntStateOf(0) }

    fun calculateBusinessDays() {
        val y1 = date1.get(Calendar.YEAR)
        val m1 = date1.get(Calendar.MONTH) + 1
        val d1 = date1.get(Calendar.DAY_OF_MONTH)

        val y2 = date2.get(Calendar.YEAR)
        val m2 = date2.get(Calendar.MONTH) + 1
        val d2 = date2.get(Calendar.DAY_OF_MONTH)

        try {
            val localStart = java.time.LocalDate.of(y1, m1, d1)
            val localEnd = java.time.LocalDate.of(y2, m2, d2)

            val dStart = if (localStart.isBefore(localEnd)) localStart else localEnd
            val dEnd = if (localStart.isBefore(localEnd)) localEnd else localStart

            val totalDays = java.time.temporal.ChronoUnit.DAYS.between(dStart, dEnd) + 1
            val weeks = totalDays / 7
            var weekends = weeks * 2
            val remainingDays = totalDays % 7

            if (remainingDays > 0) {
                for (i in 0 until remainingDays) {
                    val checkDay = dStart.plusDays(weeks * 7 + i)
                    val dayOfWeek = checkDay.dayOfWeek
                    if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
                        weekends++
                    }
                }
            }

            netWorkingDays = (totalDays - weekends).toInt()
            weekendDays = weekends.toInt()
        } catch (e: Exception) {
            netWorkingDays = 0
            weekendDays = 0
        }
    }

    LaunchedEffect(date1, date2) {
        calculateBusinessDays()
    }

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
fun UnixEpochConverterView() {
    var unixInput by remember { mutableStateOf("1779532800") } // May 23, 2026 default
    var customYear by remember { mutableStateOf("2026") }
    var customMonth by remember { mutableStateOf("05") }
    var customDay by remember { mutableStateOf("23") }
    var customHour by remember { mutableStateOf("18") }
    var customMin by remember { mutableStateOf("00") }

    var isTimestampToDateMode by remember { mutableStateOf(true) }

    val formattedDateResult = remember(unixInput, isTimestampToDateMode) {
        try {
            val seconds = unixInput.toLongOrNull() ?: 0L
            val date = Date(seconds * 1000)
            val sdfLocal = SimpleDateFormat("yyyy-MM-dd HH:mm:ss (z)", Locale.US)
            val sdfUtc = SimpleDateFormat("yyyy-MM-dd HH:mm:ss UTC", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            
            "Local: ${sdfLocal.format(date)}\nUTC: ${sdfUtc.format(date)}"
        } catch (e: Exception) {
            "Invalid Timestamp Number"
        }
    }

    val computedUnixResult = remember(customYear, customMonth, customDay, customHour, customMin, isTimestampToDateMode) {
        try {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(Calendar.YEAR, customYear.toIntOrNull() ?: 2026)
                set(Calendar.MONTH, (customMonth.toIntOrNull() ?: 5) - 1)
                set(Calendar.DAY_OF_MONTH, customDay.toIntOrNull() ?: 23)
                set(Calendar.HOUR_OF_DAY, customHour.toIntOrNull() ?: 18)
                set(Calendar.MINUTE, customMin.toIntOrNull() ?: 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val seconds = cal.timeInMillis / 1000
            "Generated Unix epoch timestamp:\n$seconds"
        } catch (e: Exception) {
            "Error building timestamp"
        }
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
