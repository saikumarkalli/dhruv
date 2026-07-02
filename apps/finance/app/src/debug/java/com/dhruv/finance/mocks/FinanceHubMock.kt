package com.dhruv.finance.mocks

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun FinanceHubMock() {
    DhruvTheme {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Loans", "Investments", "Tax", "Everyday")

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DhruvCrest(modifier = Modifier.size(24.dp), tint = DhruvGold)
                            Text("Finance", fontWeight = FontWeight.SemiBold)
                        }
                    },
                )
            },
            bottomBar = { MockBottomNav(selected = "Finance") },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
                    tabs.forEachIndexed { idx, label ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(label) },
                        )
                    }
                }

                when (selectedTab) {
                    0 -> LoansTabContent()
                    else -> PlaceholderTabContent(tabs[selectedTab])
                }
            }
        }
    }
}

@Composable
private fun LoansTabContent() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("EMI Calculator", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)

        OutlinedTextField(
            value = "5,00,000",
            onValueChange = {},
            label = { Text("Principal (₹)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = "8.5",
            onValueChange = {},
            label = { Text("Annual Interest Rate (%)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = "60",
            onValueChange = {},
            label = { Text("Tenure (months)") },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = DhruvGold),
        ) {
            Text("Calculate EMI", color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        // Result card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0001)),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Monthly EMI", fontSize = 12.sp, color = Color(0xFF9E9E9E))
                Text("₹10,233", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = DhruvGold, textAlign = TextAlign.Center)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Interest", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                        Text("₹1,13,980", fontWeight = FontWeight.Medium, color = Color(0xFFC0C0C0))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Payment", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                        Text("₹6,13,980", fontWeight = FontWeight.Medium, color = Color(0xFFC0C0C0))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderTabContent(name: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(name, fontSize = 24.sp, color = Color(0xFF9E9E9E))
        Text("(collapsed placeholder)", fontSize = 12.sp, color = Color(0xFF666666))
    }
}

@Preview(showBackground = true)
@Composable
private fun FinanceHubMockPreviewLight() = FinanceHubMock()

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FinanceHubMockPreviewDark() = FinanceHubMock()
