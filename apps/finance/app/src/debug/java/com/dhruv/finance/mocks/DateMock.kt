package com.dhruv.finance.mocks

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
fun DateMock() {
    DhruvTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DhruvCrest(modifier = Modifier.size(24.dp), tint = DhruvGold)
                            Text("Date", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            },
            bottomBar = { MockBottomNav(selected = "Date") }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // From date picker
                DatePickerStub(label = "From", value = "1 Jan 2024")
                DatePickerStub(label = "To", value = "21 Jun 2026")

                Spacer(modifier = Modifier.height(8.dp))

                // Primary result
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0001))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Duration", fontSize = 12.sp, color = Color(0xFF9E9E9E))
                        Text(
                            "902 days",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = DhruvGold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Secondary result rows
                HorizontalDivider()
                ResultRow(label = "Weeks", value = "128 weeks, 6 days")
                HorizontalDivider()
                ResultRow(label = "Months", value = "29 months, 20 days")
                HorizontalDivider()
                ResultRow(label = "Years", value = "2 years, 5 months, 20 days")
            }
        }
    }
}

@Composable
private fun DatePickerStub(label: String, value: String) {
    OutlinedButton(
        onClick = {},
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("  $label: $value", modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF9E9E9E))
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true)
@Composable
private fun DateMockPreviewLight() = DateMock()

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DateMockPreviewDark() = DateMock()
