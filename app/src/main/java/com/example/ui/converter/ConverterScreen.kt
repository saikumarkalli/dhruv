package com.example.ui.converter

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.example.ui.theme.*

@Composable
fun ConverterScreen(
    viewModel: ConverterViewModel,
    modifier: Modifier = Modifier
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) } // 0 is Currency, 1..16 are physical categories

    val categories = listOf(
        ConverterCat("Currency Exchange", Icons.Default.AttachMoney, "Live currency exchange with offline backup memory."),
        ConverterCat("Length", Icons.Default.Straighten, "Meters, Kilometers, Millis, Miles, Yards, Feet, Inches."),
        ConverterCat("Weight & Mass", Icons.Default.Scale, "Kilograms, Grams, Pounds, Ounces, Tons."),
        ConverterCat("Temperature", Icons.Default.Thermostat, "Celsius, Fahrenheit, Kelvin offsets."),
        ConverterCat("Speed", Icons.Default.Speed, "Meters/sec, Kilometers/hr, Miles/hr, Mariners Knots."),
        ConverterCat("Time Epochs", Icons.Default.Schedule, "Seconds, Minutes, Hours, Days, Weeks, Months, Years."),
        ConverterCat("Area Horizons", Icons.Default.SquareFoot, "Square Meters/Kilometers/Miles, Acres, Hectares."),
        ConverterCat("Liquid Volume", Icons.Default.InvertColors, "Liters, Milliliters, Gallons, Quarts, Cups, Cubic Feet."),
        ConverterCat("Energy Joules", Icons.Default.Bolt, "Joules, Calories, Kilocalories, Kilowatt-Hours, BTUs."),
        ConverterCat("Power Watts", Icons.Default.OfflineBolt, "Watts, Kilowatts, Megawatts, Mechanical Horsepower."),
        ConverterCat("Pressure Bars", Icons.Default.Compress, "Pascals, Kilopascals, Bars, PSIs, Atmospheres."),
        ConverterCat("Data Storage", Icons.Default.SdCard, "Bytes, Kilobytes, Megabytes, Gigabytes, Terabytes, Bits."),
        ConverterCat("Fuel Efficiency", Icons.Default.LocalGasStation, "Kilometers/Liter, Miles/Gallon, Liters per 100km."),
        ConverterCat("Angle Radians", Icons.Default.CompassCalibration, "Compass Arc Degrees, Radians, Gradians."),
        ConverterCat("Force Newtons", Icons.Default.FitnessCenter, "Newtons, Kilonewtons, Dynes, Pound-force values."),
        ConverterCat("Torque Nm", Icons.Default.Build, "Newton-Meters, Pound-Feet, Kilogram-Meters torque loads."),
        ConverterCat("Cooking Units", Icons.Default.Restaurant, "Serving Cups, Tablespoons, Teaspoons, Fluid Ounces.")
    )

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 720

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Conversions Header
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Unit Converts",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = ResponsiveApp.typography.titleLarge),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Sovereign physical measurements & currency",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveApp.typography.labelSmall),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (selectedCategoryIndex == 0) {
                        IconButton(onClick = { viewModel.syncCurrencyRates() }, modifier = Modifier.testTag("currency_sync_btn")) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync rates", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        if (isWideScreen) {
            // Tablet Left Category List Panel / Right Work area
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                            "Convert Classes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                        )
                        categories.forEachIndexed { index, cat ->
                            val isSel = selectedCategoryIndex == index
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { selectedCategoryIndex = index }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = cat.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        ActiveConverterRender(selectedCategoryIndex, viewModel)
                    }
                }
            }
        } else {
            // Mobile scrolling view
            var showCategorySelector by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategorySelector = true }
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = categories[selectedCategoryIndex].icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = categories[selectedCategoryIndex].name,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Tap to select convert criteria",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Open")
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    ActiveConverterRender(selectedCategoryIndex, viewModel)
                }
            }

            if (showCategorySelector) {
                AlertDialog(
                    onDismissRequest = { showCategorySelector = false },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showCategorySelector = false }) {
                            Text("Dismiss")
                        }
                    },
                    title = {
                        Text("Select Conversion Subject", fontWeight = FontWeight.Black)
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEachIndexed { index, cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selectedCategoryIndex == index) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable {
                                            selectedCategoryIndex = index
                                            showCategorySelector = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(cat.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

data class ConverterCat(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val description: String)

@Composable
fun ActiveConverterRender(index: Int, viewModel: ConverterViewModel) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (index) {
                0 -> CurrencyConverterTab(viewModel)
                1 -> PhysicalUnitConverterRender("Length", getLengthUnits())
                2 -> PhysicalUnitConverterRender("Weight & Mass", getWeightUnits())
                3 -> TemperatureConverterRender()
                4 -> PhysicalUnitConverterRender("Speed", getSpeedUnits())
                5 -> PhysicalUnitConverterRender("Time Duration", getTimeUnits())
                6 -> PhysicalUnitConverterRender("Area Fields", getAreaUnits())
                7 -> PhysicalUnitConverterRender("Liquid Volume", getVolumeUnits())
                8 -> PhysicalUnitConverterRender("Energy Joules", getEnergyUnits())
                9 -> PhysicalUnitConverterRender("Power Output", getPowerUnits())
                10 -> PhysicalUnitConverterRender("Pressure Level", getPressureUnits())
                11 -> PhysicalUnitConverterRender("Data Storage", getDataStorageUnits())
                12 -> FuelEfficiencyConverterRender()
                13 -> PhysicalUnitConverterRender("Angles", getAngleUnits())
                14 -> PhysicalUnitConverterRender("Force Newtons", getForceUnits())
                15 -> PhysicalUnitConverterRender("Torque Nm", getTorqueUnits())
                16 -> PhysicalUnitConverterRender("Cooking Units", getCookingUnits())
            }
        }
    }
}

// -------------------------------------------------------------
// UNIFIED ENGINE FOR STATIC RATIO CONVERSIONS
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
        Text("$title conversions", style = MaterialTheme.typography.titleMedium.copy(fontSize = ResponsiveApp.typography.titleMedium), fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pre-conversion Source", style = MaterialTheme.typography.labelSmall)

                // Dropdown trigger From
                Box {
                    Button(
                        onClick = { expandedFromMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${selectedFromUnit.label} (${selectedFromUnit.symbol})", fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Open")
                        }
                    }
                    DropdownMenu(expanded = expandedFromMenu, onDismissRequest = { expandedFromMenu = false }) {
                        unitsList.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text("${unit.label} (${unit.symbol})") },
                                onClick = {
                                    selectedFromUnit = unit
                                    expandedFromMenu = false
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Target conversion Destination", style = MaterialTheme.typography.labelSmall)

                // Dropdown trigger To
                Box {
                    Button(
                        onClick = { expandedToMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${selectedToUnit.label} (${selectedToUnit.symbol})", fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Open")
                        }
                    }
                    DropdownMenu(expanded = expandedToMenu, onDismissRequest = { expandedToMenu = false }) {
                        unitsList.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text("${unit.label} (${unit.symbol})") },
                                onClick = {
                                    selectedToUnit = unit
                                    expandedToMenu = false
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
                        Text("Conversion output results", style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveApp.typography.labelSmall), color = MaterialTheme.colorScheme.secondary)
                        Text(
                            text = outputResult,
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = ResponsiveApp.typography.headlineMedium),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
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
        Text("Temperature conversions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Text("ResultDegrees", style = MaterialTheme.typography.labelSmall)
                        Text(convertedVal, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// FUEL EFFICIENCY FORMULA CONVERTER WITH INVERSE FUNCTIONS
// -------------------------------------------------------------
@Composable
fun FuelEfficiencyConverterRender() {
    var rawInput by remember { mutableStateOf("15") }
    val fuelTypes = listOf("Kilometers per Liter (km/l)", "Miles per Gallon US (mpg)", "Liters per 100km (L/100km)")
    var fromType by remember { mutableStateOf(fuelTypes[0]) }
    var toType by remember { mutableStateOf(fuelTypes[1]) }

    var expandedFrom by remember { mutableStateOf(false) }
    var expandedTo by remember { mutableStateOf(false) }

    val doubleVal = rawInput.toDoubleOrNull() ?: 0.0
    val convertedVal = remember(doubleVal, fromType, toType) {
        if (doubleVal <= 0.0) "0"
        else {
            // first convert to base: km/l
            val baseKml = when (fromType) {
                "Miles per Gallon US (mpg)" -> doubleVal / 2.3521458
                "Liters per 100km (L/100km)" -> 100.0 / doubleVal
                else -> doubleVal
            }
            // convert to target
            val outVal = when (toType) {
                "Miles per Gallon US (mpg)" -> baseKml * 2.3521458
                "Liters per 100km (L/100km)" -> 100.0 / baseKml
                else -> baseKml
            }
            "%.4f".format(outVal).trimEnd('0').trimEnd('.')
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Fuel Efficiency conversions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    Button(onClick = { expandedFrom = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(fromType)
                    }
                    DropdownMenu(expanded = expandedFrom, onDismissRequest = { expandedFrom = false }) {
                        fuelTypes.forEach { type ->
                            DropdownMenuItem(text = { Text(type) }, onClick = { fromType = type; expandedFrom = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { rawInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Enter Efficiency Value") },
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    Button(onClick = { expandedTo = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(toType)
                    }
                    DropdownMenu(expanded = expandedTo, onDismissRequest = { expandedTo = false }) {
                        fuelTypes.forEach { type ->
                            DropdownMenuItem(text = { Text(type) }, onClick = { toType = type; expandedTo = false })
                        }
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Conversion output results", style = MaterialTheme.typography.labelSmall)
                        Text(convertedVal, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
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
    UnifiedUnit("Meters per second", 1.0, "m/s"),
    UnifiedUnit("Kilometers per hour", 0.27777778, "km/h"),
    UnifiedUnit("Miles per hour", 0.44704, "mph"),
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
    UnifiedUnit("Square Meters", 1.0, "m²"),
    UnifiedUnit("Square Kilometers", 1000000.0, "km²"),
    UnifiedUnit("Square Miles", 2589988.11, "mi²"),
    UnifiedUnit("Square Yards", 0.83612736, "yd²"),
    UnifiedUnit("Square Feet", 0.09290304, "ft²"),
    UnifiedUnit("Square Inches", 0.00064516, "in²"),
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
    UnifiedUnit("Cubic Meters", 1000.0, "m³"),
    UnifiedUnit("Cubic Feet", 28.3168465, "ft³")
)

private fun getEnergyUnits() = listOf(
    UnifiedUnit("Joules", 1.0, "J"),
    UnifiedUnit("Kilojoules", 1000.0, "kJ"),
    UnifiedUnit("Calories", 4.184, "cal"),
    UnifiedUnit("Kilocalories", 4184.0, "kcal"),
    UnifiedUnit("Watt-hours", 3600.0, "Wh"),
    UnifiedUnit("Kilowatt-hours", 3600000.0, "kWh"),
    UnifiedUnit("BTU", 1055.06, "BTU")
)

private fun getPowerUnits() = listOf(
    UnifiedUnit("Watts", 1.0, "W"),
    UnifiedUnit("Kilowatts", 1000.0, "kW"),
    UnifiedUnit("Megawatts", 1000000.0, "MW"),
    UnifiedUnit("Horsepower mec", 745.699872, "hp")
)

private fun getPressureUnits() = listOf(
    UnifiedUnit("Pascals", 1.0, "Pa"),
    UnifiedUnit("Kilopascals", 1000.0, "kPa"),
    UnifiedUnit("Bar", 100000.0, "bar"),
    UnifiedUnit("PSI", 6894.757, "psi"),
    UnifiedUnit("Atmosphere standard", 101325.0, "atm")
)

private fun getDataStorageUnits() = listOf(
    UnifiedUnit("Bytes", 1.0, "B"),
    UnifiedUnit("Kilobytes (KB)", 1024.0, "KB"),
    UnifiedUnit("Megabytes (MB)", 1048576.0, "MB"),
    UnifiedUnit("Gigabytes (GB)", 1073741824.0, "GB"),
    UnifiedUnit("Terabytes (TB)", 1099511627776.0, "TB"),
    UnifiedUnit("Bits", 0.125, "bit"),
    UnifiedUnit("Kilobits (kb)", 128.0, "kb"),
    UnifiedUnit("Megabits (mb)", 131072.0, "mb"),
    UnifiedUnit("Gigabits (gb)", 134217728.0, "gb")
)

private fun getAngleUnits() = listOf(
    UnifiedUnit("Degrees", 1.0, "deg"),
    UnifiedUnit("Radians", 57.2957795, "rad"),
    UnifiedUnit("Gradians", 0.9, "grad")
)

private fun getForceUnits() = listOf(
    UnifiedUnit("Newtons", 1.0, "N"),
    UnifiedUnit("Kilonewtons", 1000.0, "kN"),
    UnifiedUnit("Dynes", 0.00001, "dyn"),
    UnifiedUnit("Pound-force", 4.4482216, "lbf")
)

private fun getTorqueUnits() = listOf(
    UnifiedUnit("Newton-Meters", 1.0, "Nm"),
    UnifiedUnit("Pound-Feet", 1.3558179, "lb-ft"),
    UnifiedUnit("Kilogram-Meters", 9.80665, "kg-m")
)

private fun getCookingUnits() = listOf(
    UnifiedUnit("Milliliters", 1.0, "mL"),
    UnifiedUnit("Cups", 240.0, "cup"),
    UnifiedUnit("Tablespoons", 15.0, "tbsp"),
    UnifiedUnit("Teaspoons", 5.0, "tsp"),
    UnifiedUnit("Fluid Ounces US", 29.57353, "fl oz")
)

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
                                    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy h:mm a", java.util.Locale.getDefault())
                                    sdf.format(java.util.Date(it))
                                } ?: "Never synced"
                            }
                            Text(
                                text = if (status.isOffline) "Using cached local exchange rates. Loaded: $timeStr" else "Local storage cache synced: $timeStr",
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text("Converted Amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = currencyResult.ifEmpty { "0" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("currency_output_field")
                    )
                }
            }
        }
    }
}
