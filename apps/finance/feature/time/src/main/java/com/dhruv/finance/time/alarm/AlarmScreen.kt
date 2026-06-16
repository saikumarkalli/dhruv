package com.dhruv.finance.time.alarm

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlarmScreen(viewModel: AlarmViewModel) {
    val alarms by viewModel.alarms.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    if (showAddSheet) {
        AddAlarmSheet(
            onDismiss = { showAddSheet = false },
            onSave = { timeInMillis, label, difficulty ->
                viewModel.addAlarm(timeInMillis, label, difficulty)
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm")
            }
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)
        ) {
            if (alarms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No alarms set", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(alarms) { alarm ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                    Text(
                                        text = formatter.format(Date(alarm.timeInMillis)),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Light,
                                        color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = alarm.label,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = alarm.isEnabled,
                                        onCheckedChange = { viewModel.toggleAlarm(alarm, it) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { viewModel.deleteAlarm(alarm) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Alarm", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
