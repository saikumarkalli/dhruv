package com.example.ui.converter

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.TextStyle
import com.dhruv.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.example.data.SettingsRepository
import org.koin.compose.koinInject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    viewModel: ConverterViewModel,
    settingsRepository: SettingsRepository = koinInject(),
    modifier: Modifier = Modifier
) {
    var activeTool by remember { mutableStateOf<String?>(null) }

    val tools = listOf(
        GridTool("Age", Icons.Default.Cake, "Age Calculator"),
        GridTool("Area", Icons.Default.SquareFoot, "Area unit converter"),
        GridTool("BMI", Icons.Default.Scale, "Body Mass Index"),
        GridTool("Currency", Icons.Default.AttachMoney, "Live exchange rates"),
        GridTool("Data", Icons.Default.SdCard, "Data storage units"),
        GridTool("Date", Icons.Default.DateRange, "Date difference & math"),
        GridTool("Discount", Icons.Default.LocalOffer, "Price savings & markup"),
        GridTool("Length", Icons.Default.Straighten, "Distance measurements"),
        GridTool("Mass", Icons.Default.MonitorWeight, "Weight conversion"),
        GridTool("Numeral system", Icons.Default.Memory, "Number base conversion"),
        GridTool("Speed", Icons.Default.Speed, "Velocity measurements"),
        GridTool("Temperature", Icons.Default.Thermostat, "Celsius, Fahrenheit"),
        GridTool("Time", Icons.Default.Schedule, "Time intervals & durations"),
        GridTool("Volume", Icons.Default.InvertColors, "Cubit & liquid volume")
    )

    val visibleTools by remember(tools) {
        combine(
            tools.map { tool ->
                settingsRepository.isToolEnabled(tool.name).map { tool to it }
            }
        ) { array ->
            array.filter { it.second }.map { it.first }
        }
    }.collectAsState(initial = tools)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (activeTool == null) {
            // Main Library Grid View (Matches Screenshot Redesign perfectly!)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Calculators & Converters",
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
                val rows = visibleTools.chunked(3)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            row.forEach { tool ->
                                GridItemCard(
                                    tool = tool,
                                    onClick = { activeTool = tool.name },
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
                        IconButton(onClick = { activeTool = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to list",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = activeTool ?: "",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val selectedDesc = tools.find { it.name == activeTool }?.desc ?: ""
                            Text(
                                text = selectedDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (activeTool == "Currency") {
                            IconButton(onClick = { viewModel.syncCurrencyRates() }, modifier = Modifier.testTag("currency_sync_btn")) {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync rates", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    when (activeTool) {
                        "Age" -> AgeCalculatorView()
                        "Area" -> PhysicalUnitConverterRender("Area", getAreaUnits())
                        "BMI" -> BMICalculatorView()
                        "Currency" -> CurrencyConverterTab(viewModel)
                        "Data" -> PhysicalUnitConverterRender("Data Storage", getDataStorageUnits())
                        "Date" -> DateMathCalculatorView()
                        "Discount" -> DiscountCalculatorView()
                        "Length" -> PhysicalUnitConverterRender("Length", getLengthUnits())
                        "Mass" -> PhysicalUnitConverterRender("Weight & Mass", getWeightUnits())
                        "Numeral system" -> NumeralSystemConverterView()
                        "Speed" -> PhysicalUnitConverterRender("Speed", getSpeedUnits())
                        "Temperature" -> TemperatureConverterRender()
                        "Time" -> PhysicalUnitConverterRender("Time Duration", getTimeUnits())
                        "Volume" -> PhysicalUnitConverterRender("Liquid Volume", getVolumeUnits())
                    }
                }
            }
        }
    }
}

data class GridTool(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val desc: String)

@Composable
fun GridItemCard(
    tool: GridTool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp)
            .testTag("grid_item_${tool.name.lowercase().replace(" ", "_")}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.name,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = tool.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
    }
}

// -------------------------------------------------------------
// AGE CALCULATOR IMPLEMENTATION
// -------------------------------------------------------------
@Composable
fun AgeCalculatorView() {
    val context = LocalContext.current
    val sdfDisplay = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    
    var birthDate by remember { mutableStateOf(Calendar.getInstance().apply { set(2000, 0, 1) }) }
    var referenceDate by remember { mutableStateOf(Calendar.getInstance()) }

    var ageYears by remember { mutableIntStateOf(0) }
    var ageMonths by remember { mutableIntStateOf(0) }
    var ageDays by remember { mutableIntStateOf(0) }
    var nextMonths by remember { mutableIntStateOf(0) }
    var nextDays by remember { mutableIntStateOf(0) }
    var dayOfWeekOfNextBirthday by remember { mutableStateOf("Monday") }

    var totalDays by remember { mutableLongStateOf(0L) }
    var totalMonths by remember { mutableIntStateOf(0) }
    var totalWeeks by remember { mutableLongStateOf(0L) }
    var totalHours by remember { mutableLongStateOf(0L) }
    var totalMinutes by remember { mutableLongStateOf(0L) }

    fun calculateAge() {
        if (birthDate.after(referenceDate)) {
            ageYears = 0
            ageMonths = 0
            ageDays = 0
            nextMonths = 0
            nextDays = 0
            dayOfWeekOfNextBirthday = "Monday"
            totalDays = 0L
            totalMonths = 0
            totalWeeks = 0L
            totalHours = 0L
            totalMinutes = 0L
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

        dayOfWeekOfNextBirthday = SimpleDateFormat("EEEE", Locale.US).format(nextBd.time)

        // Calculate difference in months and days from referenceDate to nextBd
        var nMonths = nextBd.get(Calendar.MONTH) - referenceDate.get(Calendar.MONTH)
        var nDays = nextBd.get(Calendar.DAY_OF_MONTH) - referenceDate.get(Calendar.DAY_OF_MONTH)

        if (nDays < 0) {
            nMonths -= 1
            val tempCal = referenceDate.clone() as Calendar
            val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            nDays += daysInMonth
        }
        if (nMonths < 0) {
            nMonths += 12
        }

        nextMonths = nMonths
        nextDays = nDays

        // Total metrics
        val totalMs = referenceDate.timeInMillis - birthDate.timeInMillis
        val tDays = if (totalMs >= 0) TimeUnit.MILLISECONDS.toDays(totalMs) else 0L
        totalDays = tDays
        totalWeeks = tDays / 7
        totalMonths = yrs * 12 + mths
        totalHours = tDays * 24
        totalMinutes = tDays * 24 * 60
    }

    LaunchedEffect(birthDate, referenceDate) {
        calculateAge()
    }

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

// -------------------------------------------------------------
// BMI CALCULATOR IMPLEMENTATION
// -------------------------------------------------------------
@Composable
fun BMICalculatorView() {
    var weightInput by remember { mutableStateOf("70") }
    var heightInput by remember { mutableStateOf("175") }

    val weight = weightInput.toDoubleOrNull() ?: 0.0
    val height = heightInput.toDoubleOrNull() ?: 0.0

    val bmiResult = remember(weight, height) {
        if (weight <= 0.0 || height <= 0.0) 0.0
        else {
            val htM = height / 100.0
            weight / (htM * htM)
        }
    }

    val classification = remember(bmiResult) {
        when {
            bmiResult <= 0.0 -> "Unknown"
            bmiResult < 18.5 -> "Underweight"
            bmiResult < 25.0 -> "Normal Weight"
            bmiResult < 30.0 -> "Overweight"
            else -> "Obesity"
        }
    }

    val classificationColor = remember(classification) {
        when (classification) {
            "Normal Weight" -> Color(0xFF4CAF50)
            "Underweight" -> Color(0xFFFFB300)
            "Overweight" -> Color(0xFFFB8C00)
            "Obesity" -> Color(0xFFE53935)
            else -> Color.Gray
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Calculate Body Mass Index (BMI)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = weightInput,
            onValueChange = { weightInput = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("Weight (kg)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = heightInput,
            onValueChange = { heightInput = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("Height (cm)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        if (bmiResult > 0.0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your BMI Score",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "%.1f".format(bmiResult),
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(classificationColor)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = classification,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // M3 styled BMI spectrum meter
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("18.5", style = MaterialTheme.typography.labelSmall)
                            Text("25.0", style = MaterialTheme.typography.labelSmall)
                            Text("30.0", style = MaterialTheme.typography.labelSmall)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                        ) {
                            val activeVal = (bmiResult - 10).coerceIn(0.0, 30.0)
                            val progress = activeVal / 30.0
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress.toFloat())
                                    .fillMaxHeight()
                                    .background(classificationColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DISCOUNT CALCULATOR IMPLEMENTATION
// -------------------------------------------------------------
@Composable
fun DiscountCalculatorView() {
    var originalPriceStr by remember { mutableStateOf("100") }
    var discountStr by remember { mutableStateOf("20") }
    var taxStr by remember { mutableStateOf("10") }

    val originalPrice = originalPriceStr.toDoubleOrNull() ?: 0.0
    val discountPercent = discountStr.toDoubleOrNull() ?: 0.0
    val taxPercent = taxStr.toDoubleOrNull() ?: 0.0

    val discountAmount = originalPrice * (discountPercent / 100.0)
    val discountedPrice = originalPrice - discountAmount
    val taxAmount = discountedPrice * (taxPercent / 100.0)
    val finalPrice = discountedPrice + taxAmount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Calculate price savings and absolute prices.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = originalPriceStr,
            onValueChange = { originalPriceStr = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("Original Price ($)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = discountStr,
            onValueChange = { discountStr = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("Discount (%)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = taxStr,
            onValueChange = { taxStr = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("Tax (%) (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Discount savings", style = MaterialTheme.typography.bodyMedium)
                    Text("-$%.2f".format(discountAmount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF4CAF50))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tax amount", style = MaterialTheme.typography.bodyMedium)
                    Text("+$%.2f".format(taxAmount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Final retail Price", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("$%.2f".format(finalPrice), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DATE MATH/DIFFERENCE CALCULATOR IMPLEMENTATION
// -------------------------------------------------------------
@Composable
fun DateMathCalculatorView() {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    var dateFrom by remember { mutableStateOf(Calendar.getInstance()) }
    var dateTo by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 10) }) }

    val differenceDays = remember(dateFrom, dateTo) {
        val diffMs = dateTo.timeInMillis - dateFrom.timeInMillis
        TimeUnit.MILLISECONDS.toDays(diffMs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Select two dates to measure details in days difference",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "From Date",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> dateFrom = Calendar.getInstance().apply { set(y, m, d) } },
                                dateFrom.get(Calendar.YEAR), dateFrom.get(Calendar.MONTH), dateFrom.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = sdf.format(dateFrom.time),
                        modifier = Modifier.padding(14.dp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "To Date",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> dateTo = Calendar.getInstance().apply { set(y, m, d) } },
                                dateTo.get(Calendar.YEAR), dateTo.get(Calendar.MONTH), dateTo.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = sdf.format(dateTo.time),
                        modifier = Modifier.padding(14.dp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Duration Difference",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "$differenceDays Days",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "or ${differenceDays / 7} Weeks, ${differenceDays % 7} Days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// NUMERAL SYSTEM CONVERTER VIEW
// -------------------------------------------------------------
@Composable
fun NumeralSystemConverterView() {
    var decInput by remember { mutableStateOf("10") }
    var baseSelected by remember { mutableStateOf("DEC") } // DEC, BIN, HEX, OCT

    val parsedLong = remember(decInput, baseSelected) {
        try {
            when (baseSelected) {
                "BIN" -> decInput.toLong(2)
                "HEX" -> decInput.toLong(16)
                "OCT" -> decInput.toLong(8)
                else -> decInput.toLong(10)
            }
        } catch (e: Exception) {
            null
        }
    }

    val displayDec = parsedLong?.toString(10) ?: ""
    val displayBin = parsedLong?.toString(2) ?: ""
    val displayHex = parsedLong?.toString(16)?.uppercase() ?: ""
    val displayOct = parsedLong?.toString(8) ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Convert values between numeral foundations",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("DEC", "BIN", "HEX", "OCT").forEach { base ->
                val isSel = baseSelected == base
                Button(
                    onClick = {
                        baseSelected = base
                        decInput = when (base) {
                            "BIN" -> displayBin
                            "HEX" -> displayHex
                            "OCT" -> displayOct
                            else -> displayDec
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = base, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        OutlinedTextField(
            value = decInput,
            onValueChange = { decInput = it },
            label = { Text("Enter Number ($baseSelected)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NumeralResultRow(label = "Decimal (Dec)", value = displayDec)
                NumeralResultRow(label = "Binary (Bin)", value = displayBin)
                NumeralResultRow(label = "Hexadecimal (Hex)", value = displayHex)
                NumeralResultRow(label = "Octal (Oct)", value = displayOct)
            }
        }
    }
}

@Composable
fun NumeralResultRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(
            text = value.ifEmpty { "0" },
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(top = 8.dp))
    }
}

// -------------------------------------------------------------
// UNIFIED ENGINE FOR PHYSICAL RATIO CONVERSIONS
// -------------------------------------------------------------
data class UnifiedUnit(val label: String, val relationToBase: Double, val symbol: String)

@Composable
fun PhysicalUnitConverterRender(title: String, unitsList: List<UnifiedUnit>) {
    var rawInput by remember(title) { mutableStateOf("1") }
    var selectedFromUnit by remember(title) { mutableStateOf(unitsList[0]) }
    var selectedToUnit by remember(title) { mutableStateOf(if (unitsList.size > 1) unitsList[1] else unitsList[0]) }

    var expandedFromMenu by remember { mutableStateOf(false) }
    var expandedToMenu by remember { mutableStateOf(false) }

    val doubleInput = rawInput.toDoubleOrNull() ?: 0.0
    val outputResult = remember(doubleInput, selectedFromUnit, selectedToUnit) {
        if (doubleInput <= 0.0) "0"
        else {
            val baseValue = doubleInput * selectedFromUnit.relationToBase
            val converted = baseValue / selectedToUnit.relationToBase
            "%.6f".format(converted).trimEnd('0').trimEnd('.')
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pre-conversion Source", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)

                Box {
                    var searchFromQuery by remember { mutableStateOf("") }
                    Button(
                        onClick = { expandedFromMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${selectedFromUnit.label} (${selectedFromUnit.symbol})", fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(expanded = expandedFromMenu, onDismissRequest = { expandedFromMenu = false; searchFromQuery = "" }) {
                        OutlinedTextField(
                            value = searchFromQuery,
                            onValueChange = { searchFromQuery = it },
                            placeholder = { Text("Search units...", fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        val filteredList = unitsList.filter {
                            it.label.contains(searchFromQuery, ignoreCase = true) ||
                            it.symbol.contains(searchFromQuery, ignoreCase = true)
                        }
                        filteredList.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text("${unit.label} (${unit.symbol})") },
                                onClick = {
                                    selectedFromUnit = unit
                                    expandedFromMenu = false
                                    searchFromQuery = ""
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { rawInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Enter Input Value") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SmallFloatingActionButton(
                onClick = {
                    val originalFrom = selectedFromUnit
                    selectedFromUnit = selectedToUnit
                    selectedToUnit = originalFrom
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.SwapVert, contentDescription = "Swap")
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Target conversion Destination", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)

                Box {
                    var searchToQuery by remember { mutableStateOf("") }
                    Button(
                        onClick = { expandedToMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${selectedToUnit.label} (${selectedToUnit.symbol})", fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(expanded = expandedToMenu, onDismissRequest = { expandedToMenu = false; searchToQuery = "" }) {
                        OutlinedTextField(
                            value = searchToQuery,
                            onValueChange = { searchToQuery = it },
                            placeholder = { Text("Search units...", fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        val filteredList = unitsList.filter {
                            it.label.contains(searchToQuery, ignoreCase = true) ||
                            it.symbol.contains(searchToQuery, ignoreCase = true)
                        }
                        filteredList.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text("${unit.label} (${unit.symbol})") },
                                onClick = {
                                    selectedToUnit = unit
                                    expandedToMenu = false
                                    searchToQuery = ""
                                }
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Conversion output results", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text(
                            text = outputResult,
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 4.dp).testTag("${title.lowercase().replace(" ","_")}_output_val")
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TEMPERATURE CONVERTER VIEW WITH FORMULA ENGINE
// -------------------------------------------------------------
@Composable
fun TemperatureConverterRender() {
    var rawInput by remember { mutableStateOf("0") }
    val tempTypes = listOf("Celsius", "Fahrenheit", "Kelvin")
    var fromType by remember { mutableStateOf(tempTypes[0]) }
    var toType by remember { mutableStateOf(tempTypes[1]) }

    var expandedFrom by remember { mutableStateOf(false) }
    var expandedTo by remember { mutableStateOf(false) }

    val doubleVal = rawInput.toDoubleOrNull() ?: 0.0
    val convertedVal = remember(doubleVal, fromType, toType) {
        val baseC = when (fromType) {
            "Fahrenheit" -> (doubleVal - 32.0) / 1.8
            "Kelvin" -> doubleVal - 273.15
            else -> doubleVal
        }
        val outVal = when (toType) {
            "Fahrenheit" -> baseC * 1.8 + 32.0
            "Kelvin" -> baseC + 273.15
            else -> baseC
        }
        "%.4f".format(outVal).trimEnd('0').trimEnd('.')
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pre-conversion Source", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Box {
                    Button(onClick = { expandedFrom = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("From: $fromType")
                    }
                    DropdownMenu(expanded = expandedFrom, onDismissRequest = { expandedFrom = false }) {
                        tempTypes.forEach { type ->
                            DropdownMenuItem(text = { Text(type) }, onClick = { fromType = type; expandedFrom = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { rawInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Enter degrees") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SmallFloatingActionButton(onClick = {
                val orig = fromType
                fromType = toType
                toType = orig
            }) {
                Icon(Icons.Default.SwapVert, contentDescription = "Swap")
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Target conversion Destination", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Box {
                    Button(onClick = { expandedTo = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("To: $toType")
                    }
                    DropdownMenu(expanded = expandedTo, onDismissRequest = { expandedTo = false }) {
                        tempTypes.forEach { type ->
                            DropdownMenuItem(text = { Text(type) }, onClick = { toType = type; expandedTo = false })
                        }
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("ResultDegrees", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text(convertedVal, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PHYSICAL RATIO LIST BUILDERS
// -------------------------------------------------------------
private fun getLengthUnits() = listOf(
    UnifiedUnit("Meters", 1.0, "m"),
    UnifiedUnit("Kilometers", 1000.0, "km"),
    UnifiedUnit("Centimeters", 0.01, "cm"),
    UnifiedUnit("Millimeters", 0.001, "mm"),
    UnifiedUnit("Miles", 1609.344, "mi"),
    UnifiedUnit("Yards", 0.9144, "yd"),
    UnifiedUnit("Feet", 0.3048, "ft"),
    UnifiedUnit("Inches", 0.0254, "in")
)

private fun getWeightUnits() = listOf(
    UnifiedUnit("Kilograms", 1.0, "kg"),
    UnifiedUnit("Grams", 0.001, "g"),
    UnifiedUnit("Milligrams", 0.000001, "mg"),
    UnifiedUnit("Pounds", 0.45359237, "lb"),
    UnifiedUnit("Ounces", 0.028349523, "oz"),
    UnifiedUnit("Metric Tons", 1000.0, "t")
)

private fun getSpeedUnits() = listOf(
    UnifiedUnit("m/s", 1.0, "m/s"),
    UnifiedUnit("km/h", 0.27777778, "km/h"),
    UnifiedUnit("mph", 0.44704, "mph"),
    UnifiedUnit("Knots", 0.514444, "kt")
)

private fun getTimeUnits() = listOf(
    UnifiedUnit("Seconds", 1.0, "s"),
    UnifiedUnit("Minutes", 60.0, "min"),
    UnifiedUnit("Hours", 3600.0, "h"),
    UnifiedUnit("Days", 86400.0, "d"),
    UnifiedUnit("Weeks", 604800.0, "wk"),
    UnifiedUnit("Months", 2592000.0, "mo"),
    UnifiedUnit("Years", 31536000.0, "yr")
)

private fun getAreaUnits() = listOf(
    UnifiedUnit("Square Meters", 1.0, "mÂ²"),
    UnifiedUnit("Square Kilometers", 1000000.0, "kmÂ²"),
    UnifiedUnit("Square Miles", 2589988.11, "miÂ²"),
    UnifiedUnit("Square Yards", 0.83612736, "ydÂ²"),
    UnifiedUnit("Square Feet", 0.09290304, "ftÂ²"),
    UnifiedUnit("Square Inches", 0.00064516, "inÂ²"),
    UnifiedUnit("Acres", 4046.8564, "ac"),
    UnifiedUnit("Hectares", 10000.0, "ha")
)

private fun getVolumeUnits() = listOf(
    UnifiedUnit("Liters", 1.0, "L"),
    UnifiedUnit("Milliliters", 0.001, "mL"),
    UnifiedUnit("Gallons US", 3.78541178, "gal"),
    UnifiedUnit("Quarts US", 0.94635294, "qt"),
    UnifiedUnit("Pints US", 0.473176473, "pt"),
    UnifiedUnit("Cups", 0.24, "cup"),
    UnifiedUnit("Cubic Meters", 1000.0, "mÂ³"),
    UnifiedUnit("Cubic Feet", 28.3168465, "ftÂ³")
)

private fun getDataStorageUnits() = listOf(
    UnifiedUnit("Bytes", 1.0, "B"),
    UnifiedUnit("Kilobytes (KB)", 1024.0, "KB"),
    UnifiedUnit("Megabytes (MB)", 1048576.0, "MB"),
    UnifiedUnit("Gigabytes (GB)", 1073741824.0, "GB"),
    UnifiedUnit("Terabytes (TB)", 1099511627776.0, "TB"),
    UnifiedUnit("Bits", 0.125, "bit")
)

// -------------------------------------------------------------
// CURRENCY CONVERTER RENDERING
// -------------------------------------------------------------
@Composable
fun CurrencyConverterTab(viewModel: ConverterViewModel) {
    val currencyInput by viewModel.currencyInput.collectAsState()
    val currencyFrom by viewModel.currencyFrom.collectAsState()
    val currencyTo by viewModel.currencyTo.collectAsState()
    val currencyResult by viewModel.currencyResult.collectAsState()
    val currencyStatus by viewModel.currencyStatus.collectAsState()
    val lastUpdatedTime by viewModel.lastUpdatedTime.collectAsState()

    var showFromMenu by remember { mutableStateOf(false) }
    var showToMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (val status = currencyStatus) {
            is ConverterViewModel.CurrencyStatus.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refreshing currency rates...", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 36.dp))
                }
            }
            is ConverterViewModel.CurrencyStatus.Success -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (status.isOffline) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (status.isOffline) Icons.Default.CloudOff else Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = if (status.isOffline) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                text = if (status.isOffline) "Offline Backup Mode" else "Rates Live & Connected",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (status.isOffline) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                              )
                              val timeStr = remember(lastUpdatedTime) {
                                  lastUpdatedTime?.let {
                                      val diffMs = System.currentTimeMillis() - it
                                      val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diffMs)
                                      val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60
                                      when {
                                          hours > 0 -> "Rates cached: $hours hours $minutes mins ago"
                                          minutes > 0 -> "Rates cached: $minutes mins ago"
                                          else -> "Rates cached: Just now"
                                      }
                                  } ?: "Never synced"
                              }
                              Text(
                                  text = if (status.isOffline) "Using cached local exchange rates. $timeStr" else "Local storage cache synced. $timeStr",
                                  style = MaterialTheme.typography.bodySmall,
                                  color = if (status.isOffline) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                              )
                          }
                      }
                  }
              }
              is ConverterViewModel.CurrencyStatus.Error -> {
                  Card(
                      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                      shape = RoundedCornerShape(12.dp)
                  ) {
                      Text(
                          text = "Error: ${status.message}",
                          color = MaterialTheme.colorScheme.onErrorContainer,
                          modifier = Modifier.padding(12.dp),
                          style = MaterialTheme.typography.bodyMedium
                      )
                  }
              }
          }
  
          Card(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
          ) {
              Column(
                  modifier = Modifier
                      .fillMaxWidth()
                      .padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                  Text("From", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
  
                  Box {
                      Button(
                          onClick = { showFromMenu = true },
                          modifier = Modifier.fillMaxWidth().testTag("currency_from_btn"),
                          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                          shape = RoundedCornerShape(12.dp)
                      ) {
                          Row(
                              horizontalArrangement = Arrangement.SpaceBetween,
                              verticalAlignment = Alignment.CenterVertically,
                              modifier = Modifier.fillMaxWidth()
                          ) {
                              Text(currencyFrom)
                              Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                          }
                      }
                      DropdownMenu(
                          expanded = showFromMenu,
                          onDismissRequest = { showFromMenu = false }
                      ) {
                          viewModel.availableCurrencies.forEach { code ->
                              DropdownMenuItem(
                                  text = { Text(code) },
                                  onClick = {
                                      viewModel.setCurrencyFrom(code)
                                      showFromMenu = false
                                  }
                              )
                          }
                      }
                  }
  
                  OutlinedTextField(
                      value = currencyInput,
                      onValueChange = { viewModel.setCurrencyInput(it) },
                      label = { Text("Enter Amount") },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      modifier = Modifier.fillMaxWidth().testTag("currency_input_field"),
                      singleLine = true,
                      shape = RoundedCornerShape(12.dp)
                  )
              }
          }
  
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
              SmallFloatingActionButton(
                  onClick = {
                      val from = currencyFrom
                      val to = currencyTo
                      viewModel.setCurrencyFrom(to)
                      viewModel.setCurrencyTo(from)
                  },
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
                  contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.testTag("currency_swap_btn")
              ) {
                  Icon(Icons.Default.SwapVert, contentDescription = "Swap Currencies")
              }
          }
  
          Card(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
          ) {
              Column(
                  modifier = Modifier
                      .fillMaxWidth()
                      .padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                  Text("To", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
  
                  Box {
                      Button(
                          onClick = { showToMenu = true },
                          modifier = Modifier.fillMaxWidth().testTag("currency_to_btn"),
                          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                          shape = RoundedCornerShape(12.dp)
                      ) {
                          Row(
                              horizontalArrangement = Arrangement.SpaceBetween,
                              verticalAlignment = Alignment.CenterVertically,
                              modifier = Modifier.fillMaxWidth()
                          ) {
                              Text(currencyTo)
                              Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                          }
                      }
                      DropdownMenu(
                          expanded = showToMenu,
                          onDismissRequest = { showToMenu = false }
                      ) {
                          viewModel.availableCurrencies.forEach { code ->
                              DropdownMenuItem(
                                  text = { Text(code) },
                                  onClick = {
                                      viewModel.setCurrencyTo(code)
                                      showToMenu = false
                                  }
                              )
                          }
                      }
                  }
  
                  Card(
                      modifier = Modifier.fillMaxWidth(),
                      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                  ) {
                      Column(modifier = Modifier.padding(16.dp)) {
                          Text("Calculated Exchange", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                          Text(
                              text = currencyResult,
                              style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                              color = MaterialTheme.colorScheme.onPrimaryContainer,
                              modifier = Modifier.padding(top = 4.dp).testTag("currency_output_val")
                          )
                      }
                  }
              }
          }
      }
  }
