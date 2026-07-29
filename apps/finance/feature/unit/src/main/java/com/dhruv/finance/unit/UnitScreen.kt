package com.dhruv.finance.unit

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.components.ModeChipRow
import com.dhruv.core.ui.theme.DhruvNextKeypad
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private enum class UnitTab { LENGTH, MASS, TEMP, AREA }

private fun formatResult(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    val df = DecimalFormat("#,##0.######", DecimalFormatSymbols(Locale.US))
    return df.format(value)
}

@Composable
fun UnitScreen(
    viewModel: UnitViewModel,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(UnitTab.LENGTH) }
    val colors = LocalDhruvNextColors.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val lengthInput by viewModel.lengthInput.collectAsState()
    val lengthFromUnit by viewModel.lengthFromUnit.collectAsState()
    val lengthToUnit by viewModel.lengthToUnit.collectAsState()
    val lengthResult by viewModel.lengthResult.collectAsState()

    val massInput by viewModel.massInput.collectAsState()
    val massFromUnit by viewModel.massFromUnit.collectAsState()
    val massToUnit by viewModel.massToUnit.collectAsState()
    val massResult by viewModel.massResult.collectAsState()

    val tempInput by viewModel.tempInput.collectAsState()
    val tempFromUnit by viewModel.tempFromUnit.collectAsState()
    val tempToUnit by viewModel.tempToUnit.collectAsState()
    val tempResult by viewModel.tempResult.collectAsState()

    val areaInput by viewModel.areaInput.collectAsState()
    val areaFromUnit by viewModel.areaFromUnit.collectAsState()
    val areaToUnit by viewModel.areaToUnit.collectAsState()
    val areaResult by viewModel.areaResult.collectAsState()

    val activeInput = when (selectedTab) {
        UnitTab.LENGTH -> lengthInput
        UnitTab.MASS -> massInput
        UnitTab.TEMP -> tempInput
        UnitTab.AREA -> areaInput
    }
    val activeResult = when (selectedTab) {
        UnitTab.LENGTH -> lengthResult
        UnitTab.MASS -> massResult
        UnitTab.TEMP -> tempResult
        UnitTab.AREA -> areaResult
    }

    fun appendDigit(d: String) {
        val cur = activeInput
        val new = when {
            d == "." && cur.contains(".") -> cur
            (cur == "0" || cur.isEmpty()) && d != "." -> d
            else -> cur + d
        }
        when (selectedTab) {
            UnitTab.LENGTH -> viewModel.setLengthInput(new)
            UnitTab.MASS -> viewModel.setMassInput(new)
            UnitTab.TEMP -> viewModel.setTempInput(new)
            UnitTab.AREA -> viewModel.setAreaInput(new)
        }
    }

    fun clear() {
        when (selectedTab) {
            UnitTab.LENGTH -> viewModel.setLengthInput("0")
            UnitTab.MASS -> viewModel.setMassInput("0")
            UnitTab.TEMP -> viewModel.setTempInput("0")
            UnitTab.AREA -> viewModel.setAreaInput("0")
        }
    }

    fun backspace() {
        val new = activeInput.dropLast(1).ifEmpty { "0" }
        when (selectedTab) {
            UnitTab.LENGTH -> viewModel.setLengthInput(new)
            UnitTab.MASS -> viewModel.setMassInput(new)
            UnitTab.TEMP -> viewModel.setTempInput(new)
            UnitTab.AREA -> viewModel.setAreaInput(new)
        }
    }

    fun swap() {
        when (selectedTab) {
            UnitTab.LENGTH -> {
                val f = lengthFromUnit; val t = lengthToUnit
                viewModel.setLengthFromUnit(t); viewModel.setLengthToUnit(f)
            }
            UnitTab.MASS -> {
                val f = massFromUnit; val t = massToUnit
                viewModel.setMassFromUnit(t); viewModel.setMassToUnit(f)
            }
            UnitTab.TEMP -> {
                val f = tempFromUnit; val t = tempToUnit
                viewModel.setTempFromUnit(t); viewModel.setTempToUnit(f)
            }
            UnitTab.AREA -> {
                val f = areaFromUnit; val t = areaToUnit
                viewModel.setAreaFromUnit(t); viewModel.setAreaToUnit(f)
            }
        }
    }

    val categoryOptions = listOf("Length", "Mass", "Temp", "Area")
    val categoryIcons = listOf<ImageVector?>(
        Icons.Default.Straighten,
        Icons.Default.Scale,
        Icons.Default.Thermostat,
        Icons.Default.CropFree,
    )

    val clipboardManager = LocalClipboardManager.current

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    ModeChipRow(
                        options = categoryOptions,
                        icons = categoryIcons,
                        selectedIndex = selectedTab.ordinal,
                        onSelected = { selectedTab = UnitTab.entries[it] },
                        modifier = Modifier.padding(
                            horizontal = DhruvNextSpacing.screenGutter,
                            vertical = DhruvNextSpacing.interCardGap,
                        ),
                    )
                    UnitDisplayArea(
                        tab = selectedTab,
                        lengthInput = lengthInput, lengthFromUnit = lengthFromUnit,
                        lengthToUnit = lengthToUnit, lengthResult = lengthResult,
                        massInput = massInput, massFromUnit = massFromUnit,
                        massToUnit = massToUnit, massResult = massResult,
                        tempInput = tempInput, tempFromUnit = tempFromUnit,
                        tempToUnit = tempToUnit, tempResult = tempResult,
                        areaInput = areaInput, areaFromUnit = areaFromUnit,
                        areaToUnit = areaToUnit, areaResult = areaResult,
                        onLengthFromUnit = viewModel::setLengthFromUnit,
                        onLengthToUnit = viewModel::setLengthToUnit,
                        onMassFromUnit = viewModel::setMassFromUnit,
                        onMassToUnit = viewModel::setMassToUnit,
                        onTempFromUnit = viewModel::setTempFromUnit,
                        onTempToUnit = viewModel::setTempToUnit,
                        onAreaFromUnit = viewModel::setAreaFromUnit,
                        onAreaToUnit = viewModel::setAreaToUnit,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
                UnitKeypad(
                    onDigit = ::appendDigit,
                    onClear = ::clear,
                    onBackspace = ::backspace,
                    onSwap = ::swap,
                    onCopy = { clipboardManager.setText(AnnotatedString(activeResult)) },
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight()
                        .widthIn(max = 320.dp)
                        .padding(
                            end = DhruvNextSpacing.screenGutter,
                            bottom = DhruvNextSpacing.screenGutter,
                        ),
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                ModeChipRow(
                    options = categoryOptions,
                    icons = categoryIcons,
                    selectedIndex = selectedTab.ordinal,
                    onSelected = { selectedTab = UnitTab.entries[it] },
                    modifier = Modifier.padding(
                        horizontal = DhruvNextSpacing.screenGutter,
                        vertical = DhruvNextSpacing.interCardGap,
                    ),
                )
                UnitDisplayArea(
                    tab = selectedTab,
                    lengthInput = lengthInput, lengthFromUnit = lengthFromUnit,
                    lengthToUnit = lengthToUnit, lengthResult = lengthResult,
                    massInput = massInput, massFromUnit = massFromUnit,
                    massToUnit = massToUnit, massResult = massResult,
                    tempInput = tempInput, tempFromUnit = tempFromUnit,
                    tempToUnit = tempToUnit, tempResult = tempResult,
                    areaInput = areaInput, areaFromUnit = areaFromUnit,
                    areaToUnit = areaToUnit, areaResult = areaResult,
                    onLengthFromUnit = viewModel::setLengthFromUnit,
                    onLengthToUnit = viewModel::setLengthToUnit,
                    onMassFromUnit = viewModel::setMassFromUnit,
                    onMassToUnit = viewModel::setMassToUnit,
                    onTempFromUnit = viewModel::setTempFromUnit,
                    onTempToUnit = viewModel::setTempToUnit,
                    onAreaFromUnit = viewModel::setAreaFromUnit,
                    onAreaToUnit = viewModel::setAreaToUnit,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                UnitKeypad(
                    onDigit = ::appendDigit,
                    onClear = ::clear,
                    onBackspace = ::backspace,
                    onSwap = ::swap,
                    onCopy = { clipboardManager.setText(AnnotatedString(activeResult)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DhruvNextSpacing.screenGutter)
                        .padding(bottom = DhruvNextSpacing.screenGutter),
                )
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun UnitDisplayArea(
    tab: UnitTab,
    lengthInput: String, lengthFromUnit: LengthUnit, lengthToUnit: LengthUnit, lengthResult: String,
    massInput: String, massFromUnit: MassUnit, massToUnit: MassUnit, massResult: String,
    tempInput: String, tempFromUnit: TemperatureUnit, tempToUnit: TemperatureUnit, tempResult: String,
    areaInput: String, areaFromUnit: AreaUnit, areaToUnit: AreaUnit, areaResult: String,
    onLengthFromUnit: (LengthUnit) -> Unit, onLengthToUnit: (LengthUnit) -> Unit,
    onMassFromUnit: (MassUnit) -> Unit, onMassToUnit: (MassUnit) -> Unit,
    onTempFromUnit: (TemperatureUnit) -> Unit, onTempToUnit: (TemperatureUnit) -> Unit,
    onAreaFromUnit: (AreaUnit) -> Unit, onAreaToUnit: (AreaUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .then(if (isTablet) Modifier.widthIn(max = 480.dp) else Modifier.fillMaxWidth())
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DhruvNextSpacing.screenGutter)
                .padding(bottom = DhruvNextSpacing.interCardGap),
            verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
        ) {
            when (tab) {
                UnitTab.LENGTH -> {
                    val inputDouble = lengthInput.toDoubleOrNull() ?: 0.0
                    val others = UnitCategory.Length.convertToOtherUnits(inputDouble, lengthFromUnit)
                        .map { (unit, v) -> unit.label to formatResult(v) }
                    ConverterCard(
                        fromLabel = lengthFromUnit.label,
                        toLabel = lengthToUnit.label,
                        fromAmount = lengthInput,
                        toAmount = if (lengthResult.isNotEmpty()) lengthResult else "—",
                        fromUnits = LengthUnit.entries.map { it.label },
                        toUnits = LengthUnit.entries.map { it.label },
                        fromUnitIndex = lengthFromUnit.ordinal,
                        toUnitIndex = lengthToUnit.ordinal,
                        onFromSelected = { onLengthFromUnit(LengthUnit.entries[it]) },
                        onToSelected = { onLengthToUnit(LengthUnit.entries[it]) },
                    )
                    AlsoList(
                        inputDisplay = "$lengthInput ${lengthFromUnit.symbol}",
                        items = others,
                    )
                }
                UnitTab.MASS -> {
                    val inputDouble = massInput.toDoubleOrNull() ?: 0.0
                    val others = UnitCategory.Mass.convertToOtherUnits(inputDouble, massFromUnit)
                        .map { (unit, v) -> unit.label to formatResult(v) }
                    ConverterCard(
                        fromLabel = massFromUnit.label,
                        toLabel = massToUnit.label,
                        fromAmount = massInput,
                        toAmount = if (massResult.isNotEmpty()) massResult else "—",
                        fromUnits = MassUnit.entries.map { it.label },
                        toUnits = MassUnit.entries.map { it.label },
                        fromUnitIndex = massFromUnit.ordinal,
                        toUnitIndex = massToUnit.ordinal,
                        onFromSelected = { onMassFromUnit(MassUnit.entries[it]) },
                        onToSelected = { onMassToUnit(MassUnit.entries[it]) },
                    )
                    AlsoList(
                        inputDisplay = "$massInput ${massFromUnit.symbol}",
                        items = others,
                    )
                }
                UnitTab.TEMP -> {
                    val inputDouble = tempInput.toDoubleOrNull() ?: 0.0
                    val others = UnitCategory.Temperature.convertToOtherUnits(inputDouble, tempFromUnit)
                        .map { (unit, v) -> unit.label to formatResult(v) }
                    ConverterCard(
                        fromLabel = tempFromUnit.label,
                        toLabel = tempToUnit.label,
                        fromAmount = tempInput,
                        toAmount = if (tempResult.isNotEmpty()) tempResult else "—",
                        fromUnits = TemperatureUnit.entries.map { it.label },
                        toUnits = TemperatureUnit.entries.map { it.label },
                        fromUnitIndex = tempFromUnit.ordinal,
                        toUnitIndex = tempToUnit.ordinal,
                        onFromSelected = { onTempFromUnit(TemperatureUnit.entries[it]) },
                        onToSelected = { onTempToUnit(TemperatureUnit.entries[it]) },
                    )
                    AlsoList(
                        inputDisplay = "$tempInput ${tempFromUnit.symbol}",
                        items = others,
                    )
                }
                UnitTab.AREA -> {
                    val inputDouble = areaInput.toDoubleOrNull() ?: 0.0
                    val others = UnitCategory.Area.convertToOtherUnits(inputDouble, areaFromUnit)
                        .map { (unit, v) -> unit.label to formatResult(v) }
                    ConverterCard(
                        fromLabel = areaFromUnit.label,
                        toLabel = areaToUnit.label,
                        fromAmount = areaInput,
                        toAmount = if (areaResult.isNotEmpty()) areaResult else "—",
                        fromUnits = AreaUnit.entries.map { it.label },
                        toUnits = AreaUnit.entries.map { it.label },
                        fromUnitIndex = areaFromUnit.ordinal,
                        toUnitIndex = areaToUnit.ordinal,
                        onFromSelected = { onAreaFromUnit(AreaUnit.entries[it]) },
                        onToSelected = { onAreaToUnit(AreaUnit.entries[it]) },
                    )
                    AlsoList(
                        inputDisplay = "$areaInput ${areaFromUnit.symbol}",
                        items = others,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConverterCard(
    fromLabel: String,
    toLabel: String,
    fromAmount: String,
    toAmount: String,
    fromUnits: List<String>,
    toUnits: List<String>,
    fromUnitIndex: Int,
    toUnitIndex: Int,
    onFromSelected: (Int) -> Unit,
    onToSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val radius = DhruvNextRadii.card

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(radius), clip = false)
            .clip(RoundedCornerShape(radius))
            .background(colors.surf)
            .border(1.dp, colors.line, RoundedCornerShape(radius))
            .padding(DhruvNextSpacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        UnitRow(
            label = "FROM",
            unitLabel = fromLabel,
            amount = fromAmount,
            unitOptions = fromUnits,
            selectedIndex = fromUnitIndex,
            onSelected = onFromSelected,
            amountColor = colors.tx,
        )
        HorizontalDivider(color = colors.line2, thickness = 1.dp)
        UnitRow(
            label = "TO",
            unitLabel = toLabel,
            amount = toAmount,
            unitOptions = toUnits,
            selectedIndex = toUnitIndex,
            onSelected = onToSelected,
            amountColor = colors.acc,
        )
    }
}

@Composable
private fun UnitRow(
    label: String,
    unitLabel: String,
    amount: String,
    unitOptions: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    amountColor: Color,
) {
    val colors = LocalDhruvNextColors.current
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Left: label + unit picker (intrinsic width; amount Text takes remaining via weight(1f))
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = colors.tx3,
            )
            Box {
                Row(
                    modifier = Modifier.clickable { expanded = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = unitLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.tx,
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Change unit",
                        modifier = Modifier.size(17.dp),
                        tint = colors.tx3,
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    unitOptions.forEachIndexed { idx, name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = { onSelected(idx); expanded = false },
                        )
                    }
                }
            }
        }
        // Right: amount display
        Text(
            text = amount,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.2).sp,
            color = amountColor,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            softWrap = false,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AlsoList(
    inputDisplay: String,
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val radius = DhruvNextRadii.card

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(radius), clip = false)
            .clip(RoundedCornerShape(radius))
            .background(colors.surf)
            .border(1.dp, colors.line, RoundedCornerShape(radius))
            .padding(DhruvNextSpacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "$inputDisplay is also",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp,
            color = colors.tx,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items.forEachIndexed { idx, (label, value) ->
                if (idx > 0) {
                    HorizontalDivider(color = colors.line2, thickness = 1.dp)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = label,
                        fontSize = 13.5.sp,
                        color = colors.tx2,
                    )
                    Text(
                        text = value,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.tx,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitKeypad(
    onDigit: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onSwap: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val keyRadius = RoundedCornerShape(DhruvNextRadii.listGroup)
    val digitSize = DhruvNextKeypad.digit
    val gap = 8.dp
    val keyMinHeight = 54.dp

    // Layout: Row { 3-col left grid | right column (C, ⌫, swap×2) }
    // The right column's swap key spans the height of 2 key rows, achieved by weight(2f).
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        // Left 3 columns — 4 rows of 3 keys each
        Column(
            modifier = Modifier.weight(3f),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            // Row 1: 7 8 9
            Row(
                modifier = Modifier.fillMaxWidth().height(keyMinHeight),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                listOf("7", "8", "9").forEach { d ->
                    UnitKey(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = keyRadius,
                        background = colors.surf,
                        borderColor = colors.line,
                        onClick = { onDigit(d) },
                    ) {
                        Text(d, fontSize = digitSize, fontWeight = FontWeight.W500, color = colors.tx)
                    }
                }
            }
            // Row 2: 4 5 6
            Row(
                modifier = Modifier.fillMaxWidth().height(keyMinHeight),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                listOf("4", "5", "6").forEach { d ->
                    UnitKey(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = keyRadius,
                        background = colors.surf,
                        borderColor = colors.line,
                        onClick = { onDigit(d) },
                    ) {
                        Text(d, fontSize = digitSize, fontWeight = FontWeight.W500, color = colors.tx)
                    }
                }
            }
            // Row 3: 1 2 3
            Row(
                modifier = Modifier.fillMaxWidth().height(keyMinHeight),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                listOf("1", "2", "3").forEach { d ->
                    UnitKey(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = keyRadius,
                        background = colors.surf,
                        borderColor = colors.line,
                        onClick = { onDigit(d) },
                    ) {
                        Text(d, fontSize = digitSize, fontWeight = FontWeight.W500, color = colors.tx)
                    }
                }
            }
            // Row 4: 0 . copy
            Row(
                modifier = Modifier.fillMaxWidth().height(keyMinHeight),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                UnitKey(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = keyRadius,
                    background = colors.surf,
                    borderColor = colors.line,
                    onClick = { onDigit("0") },
                ) {
                    Text("0", fontSize = digitSize, fontWeight = FontWeight.W500, color = colors.tx)
                }
                UnitKey(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = keyRadius,
                    background = colors.surf,
                    borderColor = colors.line,
                    onClick = { onDigit(".") },
                ) {
                    Text(".", fontSize = digitSize, fontWeight = FontWeight.W500, color = colors.tx)
                }
                UnitKey(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = keyRadius,
                    background = colors.surf,
                    borderColor = colors.line,
                    onClick = onCopy,
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy result",
                        modifier = Modifier.size(20.dp),
                        tint = colors.tx2,
                    )
                }
            }
        }

        // Right column: C (row 1), ⌫ (row 2), swap (rows 3+4 via weight 2f)
        Column(
            modifier = Modifier
                .weight(1f)
                .height(keyMinHeight * 4 + gap * 3),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            // C key (clear)
            UnitKey(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = keyRadius,
                background = colors.surf,
                borderColor = colors.line,
                onClick = onClear,
            ) {
                Text(
                    "C",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.acc,
                )
            }
            // Backspace key
            UnitKey(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = keyRadius,
                background = colors.surf,
                borderColor = colors.line,
                onClick = onBackspace,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    modifier = Modifier.size(21.dp),
                    tint = colors.acc,
                )
            }
            // Swap key — weight(2f) makes it span 2 key heights + 1 gap
            UnitKey(
                modifier = Modifier.fillMaxWidth().weight(2f),
                shape = keyRadius,
                background = colors.acc,
                borderColor = colors.acc,
                onClick = onSwap,
            ) {
                Icon(
                    Icons.Default.SwapVert,
                    contentDescription = "Swap units",
                    modifier = Modifier.size(22.dp),
                    tint = colors.onAcc,
                )
            }
        }
    }
}

@Composable
private fun UnitKey(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    background: Color,
    borderColor: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
