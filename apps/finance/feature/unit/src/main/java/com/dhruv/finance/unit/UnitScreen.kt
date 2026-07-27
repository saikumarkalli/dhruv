package com.dhruv.finance.unit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.components.ModeChipRow
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * Unit converter screen extracted from the monolithic ConverterScreen. Hosts Length and Mass
 * conversion using [UnitViewModel]. Restyled onto the DhruvNext component library (ADR-0024,
 * design spec §6.6: category chip row + From/To rows with unit pickers and a live result) — a
 * reskin only, every ViewModel call and conversion path is unchanged.
 */
@Composable
fun UnitScreen(
    viewModel: UnitViewModel,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val categories = listOf("Length", "Mass")
    val colors = LocalDhruvNextColors.current

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.bg),
    ) {
        ModeChipRow(
            options = categories,
            selectedIndex = selectedTab,
            onSelected = { selectedTab = it },
            modifier =
                Modifier.padding(
                    horizontal = DhruvNextSpacing.screenGutter,
                    vertical = DhruvNextSpacing.interCardGap,
                ),
        )
        when (selectedTab) {
            0 -> LengthConverter(viewModel)
            1 -> MassConverter(viewModel)
        }
    }
}

@Composable
private fun LengthConverter(viewModel: UnitViewModel) {
    val input by viewModel.lengthInput.collectAsState()
    val fromUnit by viewModel.lengthFromUnit.collectAsState()
    val toUnit by viewModel.lengthToUnit.collectAsState()
    val result by viewModel.lengthResult.collectAsState()

    UnitConverterBody(
        input = input,
        onInputChange = viewModel::setLengthInput,
        fromLabel = fromUnit.label,
        toLabel = toUnit.label,
        result = result,
        resultSymbol = toUnit.symbol,
        units = LengthUnit.entries.map { it.label },
        onFromSelected = { idx -> viewModel.setLengthFromUnit(LengthUnit.entries[idx]) },
        onToSelected = { idx -> viewModel.setLengthToUnit(LengthUnit.entries[idx]) },
        onSwap = {
            val f = fromUnit
            val t = toUnit
            viewModel.setLengthFromUnit(t)
            viewModel.setLengthToUnit(f)
        },
    )
}

@Composable
private fun MassConverter(viewModel: UnitViewModel) {
    val input by viewModel.massInput.collectAsState()
    val fromUnit by viewModel.massFromUnit.collectAsState()
    val toUnit by viewModel.massToUnit.collectAsState()
    val result by viewModel.massResult.collectAsState()

    UnitConverterBody(
        input = input,
        onInputChange = viewModel::setMassInput,
        fromLabel = fromUnit.label,
        toLabel = toUnit.label,
        result = result,
        resultSymbol = toUnit.symbol,
        units = MassUnit.entries.map { it.label },
        onFromSelected = { idx -> viewModel.setMassFromUnit(MassUnit.entries[idx]) },
        onToSelected = { idx -> viewModel.setMassToUnit(MassUnit.entries[idx]) },
        onSwap = {
            val f = fromUnit
            val t = toUnit
            viewModel.setMassFromUnit(t)
            viewModel.setMassToUnit(f)
        },
    )
}

@Composable
private fun UnitConverterBody(
    input: String,
    onInputChange: (String) -> Unit,
    fromLabel: String,
    toLabel: String,
    result: String,
    resultSymbol: String,
    units: List<String>,
    onFromSelected: (Int) -> Unit,
    onToSelected: (Int) -> Unit,
    onSwap: () -> Unit,
) {
    var showFromMenu by remember { mutableStateOf(false) }
    var showToMenu by remember { mutableStateOf(false) }
    val colors = LocalDhruvNextColors.current

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DhruvNextSpacing.screenGutter)
                .padding(bottom = DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        // From row
        NxCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "From",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.tx2,
                )
                UnitDropdownField(
                    label = fromLabel,
                    expanded = showFromMenu,
                    onExpand = { showFromMenu = true },
                    onDismiss = { showFromMenu = false },
                    units = units,
                    onSelected = {
                        onFromSelected(it)
                        showFromMenu = false
                    },
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    label = { Text("Enter value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(DhruvNextRadii.innerTile),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.tx,
                            unfocusedTextColor = colors.tx,
                            cursorColor = colors.acc,
                            focusedBorderColor = colors.acc,
                            unfocusedBorderColor = colors.line,
                            focusedLabelColor = colors.acc,
                            unfocusedLabelColor = colors.tx2,
                            focusedContainerColor = colors.surf2,
                            unfocusedContainerColor = colors.surf2,
                        ),
                )
            }
        }

        // Swap key — dedicated action between the From/To rows (design spec §6.6).
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FilledIconButton(
                onClick = onSwap,
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = colors.accSoft,
                        contentColor = colors.acc,
                    ),
            ) {
                Icon(Icons.Default.SwapVert, contentDescription = "Swap from and to units")
            }
        }

        // To row + live result
        NxCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "To",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.tx2,
                )
                UnitDropdownField(
                    label = toLabel,
                    expanded = showToMenu,
                    onExpand = { showToMenu = true },
                    onDismiss = { showToMenu = false },
                    units = units,
                    onSelected = {
                        onToSelected(it)
                        showToMenu = false
                    },
                )
                Text(
                    text = "Converted value",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.tx3,
                )
                Text(
                    text = if (result.isNotEmpty()) "$result $resultSymbol" else "—",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.acc,
                )
            }
        }
    }
}

@Composable
private fun UnitDropdownField(
    label: String,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    units: List<String>,
    onSelected: (Int) -> Unit,
) {
    val colors = LocalDhruvNextColors.current
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(DhruvNextRadii.innerTile))
                    .background(colors.surf2)
                    .clickable(onClick = onExpand)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colors.tx,
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = colors.tx2,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            units.forEachIndexed { idx, name ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelected(idx) })
            }
        }
    }
}
