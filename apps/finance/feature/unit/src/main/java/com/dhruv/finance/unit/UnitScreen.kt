package com.dhruv.finance.unit

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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Unit converter screen extracted from the monolithic ConverterScreen.
 * Hosts Length and Mass conversion using [UnitViewModel].
 */
@Composable
fun UnitScreen(viewModel: UnitViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Length", "Mass")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
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
            val f = fromUnit; val t = toUnit
            viewModel.setLengthFromUnit(t); viewModel.setLengthToUnit(f)
        }
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
            val f = fromUnit; val t = toUnit
            viewModel.setMassFromUnit(t); viewModel.setMassToUnit(f)
        }
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
    onSwap: () -> Unit
) {
    var showFromMenu by remember { mutableStateOf(false) }
    var showToMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("From", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                UnitDropdown(label = fromLabel, expanded = showFromMenu, onExpand = { showFromMenu = true }, onDismiss = { showFromMenu = false }, units = units, onSelected = { onFromSelected(it); showFromMenu = false })
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    label = { Text("Enter Value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SmallFloatingActionButton(
                onClick = onSwap,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.SwapVert, contentDescription = "Swap Units")
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("To", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                UnitDropdown(label = toLabel, expanded = showToMenu, onExpand = { showToMenu = true }, onDismiss = { showToMenu = false }, units = units, onSelected = { onToSelected(it); showToMenu = false })
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Converted Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text(
                            text = "$result $resultSymbol",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitDropdown(
    label: String,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    units: List<String>,
    onSelected: (Int) -> Unit
) {
    Box {
        Button(
            onClick = onExpand,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(label)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            units.forEachIndexed { idx, name ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelected(idx) })
            }
        }
    }
}
