package com.dhruv.finance.mocks

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.components.DhruvWordmark
import com.dhruv.core.ui.theme.DhruvGold
import com.dhruv.core.ui.theme.DhruvNavy
import com.dhruv.core.ui.theme.DhruvTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantMock() {
    DhruvTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            DhruvWordmark(
                                appName = "finance",
                                crestTint = DhruvGold,
                                textColor = DhruvGold,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Info, contentDescription = "About AI", tint = Color(0xFF9E9E9E))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A)),
                )
            },
            bottomBar = { MockBottomNav(selected = "Assistant") },
        ) { padding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
            ) {
                // Consent banner
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(DhruvGold.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Using Gemini API · Tap for privacy info",
                        fontSize = 12.sp,
                        color = DhruvGold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {}) {
                        Text("Details", fontSize = 12.sp, color = DhruvGold)
                    }
                }

                // Chat area
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // AI message bubble
                    Column(
                        modifier =
                            Modifier
                                .widthIn(max = 280.dp)
                                .background(DhruvNavy, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                                .border(1.dp, DhruvGold.copy(alpha = 0.4f), RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                                .padding(12.dp),
                    ) {
                        Text("Dhruv", fontSize = 11.sp, color = DhruvGold, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Hello! I'm your Dhruv Finance assistant. I can help you calculate EMIs, compare investment options, explain financial concepts, and more. What would you like to know?",
                            fontSize = 14.sp,
                            color = Color(0xFFC0C0C0),
                        )
                    }

                    // User message bubble (right-aligned)
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Box(
                            modifier =
                                Modifier
                                    .widthIn(max = 240.dp)
                                    .background(DhruvGold.copy(alpha = 0.2f), RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                                    .padding(12.dp),
                        ) {
                            Text(
                                "What's a good SIP amount for ₹10L goal in 5 years?",
                                fontSize = 14.sp,
                                color = Color.White,
                            )
                        }
                    }
                }

                // Input bar
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1A1A1A))
                            .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("Ask about finance…", color = Color(0xFF666666)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    IconButton(
                        onClick = {},
                        modifier =
                            Modifier
                                .size(48.dp)
                                .background(DhruvGold, CircleShape),
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AssistantMockPreviewLight() = AssistantMock()

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AssistantMockPreviewDark() = AssistantMock()
