package com.dhruv.finance.mocks

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

private val DhruvNavyMock = Color(0xFF0D1B2A)
private val KeySurface = Color(0xFF1E1E1E)
private val KeyNumBg = Color(0xFF2C2C2C)

private data class CalcKey(
    val label: String,
    val isOp: Boolean = false,
    val isEquals: Boolean = false,
)

private val keypadRows =
    listOf(
        listOf(CalcKey("AC"), CalcKey("±"), CalcKey("%"), CalcKey("÷", isOp = true)),
        listOf(CalcKey("7"), CalcKey("8"), CalcKey("9"), CalcKey("×", isOp = true)),
        listOf(CalcKey("4"), CalcKey("5"), CalcKey("6"), CalcKey("−", isOp = true)),
        listOf(CalcKey("1"), CalcKey("2"), CalcKey("3"), CalcKey("+", isOp = true)),
        listOf(CalcKey("0"), CalcKey("."), CalcKey("=", isEquals = true)),
    )

@Composable
private fun CalcKeyButton(
    key: CalcKey,
    modifier: Modifier = Modifier,
) {
    val bg =
        when {
            key.isEquals || key.isOp -> DhruvGold
            else -> KeyNumBg
        }
    val fg = if (key.isEquals || key.isOp) Color.White else Color(0xFFF5F5F5)
    Box(
        modifier =
            modifier
                .height(64.dp)
                .background(bg, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(key.label, color = fg, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorMock() {
    DhruvTheme {
        Scaffold(
            containerColor = Color(0xFF0A0A0A),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DhruvCrest(modifier = Modifier.size(24.dp), tint = DhruvGold)
                            Text("dhruv finance", color = DhruvGold, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF9E9E9E))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A)),
                )
            },
            bottomBar = { MockBottomNav(selected = "Calc") },
        ) { padding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Display area
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End,
                ) {
                    Text("0", color = Color(0xFF9E9E9E), fontSize = 14.sp) // history indicator
                    Text(
                        "0",
                        color = Color.White,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.End,
                    )
                }

                // Keypad
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    keypadRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // "0" key is double-wide
                            row.forEach { key ->
                                val weight = if (key.label == "0") 2f else 1f
                                CalcKeyButton(key = key, modifier = Modifier.weight(weight))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalculatorMockPreviewLight() = CalculatorMock()

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CalculatorMockPreviewDark() = CalculatorMock()
