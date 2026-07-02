package com.dhruv.finance.mocks

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.components.DhruvCrest
import com.dhruv.core.ui.theme.DhruvGold
import com.dhruv.core.ui.theme.DhruvTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterHubMock() {
    DhruvTheme {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Currency", "Units")

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DhruvCrest(modifier = Modifier.size(24.dp), tint = DhruvGold)
                            Text("Converter", fontWeight = FontWeight.SemiBold)
                        }
                    },
                )
            },
            bottomBar = { MockBottomNav(selected = "Converter") },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { idx, label ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(label) },
                        )
                    }
                }

                when (selectedTab) {
                    0 -> CurrencyTabContent()
                    1 -> UnitsTabContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyTabContent() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("From", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        // Currency picker stub
        ExposedDropdownMenuBox(expanded = false, onExpandedChange = {}) {
            OutlinedTextField(
                value = "Indian Rupee (INR)",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
        }

        Text("To", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        ExposedDropdownMenuBox(expanded = false, onExpandedChange = {}) {
            OutlinedTextField(
                value = "US Dollar (USD)",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
        }

        OutlinedTextField(
            value = "1000",
            onValueChange = {},
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth(),
        )

        // Large result display
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("≈ 12.05 USD", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = DhruvGold, textAlign = TextAlign.Center)
                Text("1 INR = 0.01205 USD", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun UnitsTabContent() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Category picker row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Length", "Mass", "Temp", "Volume", "Speed").forEach { category ->
                val isSelected = category == "Length"
                Button(
                    onClick = {},
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                if (isSelected) {
                                    DhruvGold
                                } else {
                                    androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                                },
                        ),
                ) {
                    Text(category, fontSize = 11.sp)
                }
            }
        }

        OutlinedTextField(
            value = "100",
            onValueChange = {},
            label = { Text("Meters (m)") },
            modifier = Modifier.fillMaxWidth(),
        )

        Icon(
            imageVector = Icons.Default.SwapVert,
            contentDescription = "Swap",
            modifier = Modifier.align(Alignment.CenterHorizontally).size(32.dp),
            tint = DhruvGold,
        )

        OutlinedTextField(
            value = "328.084",
            onValueChange = {},
            label = { Text("Feet (ft)") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConverterHubMockPreviewLight() = ConverterHubMock()

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ConverterHubMockPreviewDark() = ConverterHubMock()
