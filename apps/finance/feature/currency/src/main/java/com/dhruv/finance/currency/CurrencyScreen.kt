package com.dhruv.finance.currency

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState

/**
 * Currency converter screen extracted from the monolithic ConverterScreen.
 * Depends solely on [CurrencyViewModel] which is wired via Koin.
 *
 * Note: the top-level back/sync toolbar is expected to be provided by the
 * Converter hub in the app shell (or a wrapper composable) that navigates
 * between currency and unit sub-screens. This composable owns only the
 * conversion UI body.
 */
@Composable
fun CurrencyScreen(
    viewModel: CurrencyViewModel,
    modifier: Modifier = Modifier
) {
    CurrencyConverterContent(viewModel = viewModel, modifier = modifier)
}

@Composable
fun CurrencyConverterContent(
    viewModel: CurrencyViewModel,
    modifier: Modifier = Modifier
) {
    val currencyInput by viewModel.currencyInput.collectAsState()
    val currencyFrom by viewModel.currencyFrom.collectAsState()
    val currencyTo by viewModel.currencyTo.collectAsState()
    val currencyResult by viewModel.currencyResult.collectAsState()
    val currencyStatus by viewModel.currencyStatus.collectAsState()
    val lastUpdatedTime by viewModel.lastUpdatedTime.collectAsState()

    var showFromMenu by remember { mutableStateOf(false) }
    var showToMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (val status = currencyStatus) {
            is CurrencyViewModel.CurrencyStatus.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Refreshing currency rates...",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 36.dp)
                    )
                }
            }
            is CurrencyViewModel.CurrencyStatus.Success -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (status.isOffline) MaterialTheme.colorScheme.errorContainer
                                         else MaterialTheme.colorScheme.primaryContainer
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
                            tint = if (status.isOffline) MaterialTheme.colorScheme.onErrorContainer
                                   else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                text = if (status.isOffline) "Offline Backup Mode" else "Rates Live & Connected",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (status.isOffline) MaterialTheme.colorScheme.onErrorContainer
                                        else MaterialTheme.colorScheme.onPrimaryContainer
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
                                text = if (status.isOffline) "Using cached local exchange rates. $timeStr"
                                       else "Local storage cache synced. $timeStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (status.isOffline) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                        else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            is CurrencyViewModel.CurrencyStatus.Error -> {
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
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
                        Text(
                            "Calculated Exchange",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
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
