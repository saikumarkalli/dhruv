package com.example.ui.time

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.time.alarm.AlarmScreen
import com.example.ui.time.alarm.AlarmViewModel
import com.example.ui.time.stopwatch.StopwatchScreen
import com.example.ui.time.stopwatch.StopwatchViewModel
import com.example.ui.time.timer.TimerScreen
import com.example.ui.time.timer.TimerViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun TimeScreen(
    viewModel: TimeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("Stopwatch", "Timer", "Alarm")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = uiState.selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = uiState.selectedTab == index,
                    onClick = { viewModel.selectTab(index) },
                    text = { Text(title) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when (uiState.selectedTab) {
                0 -> {
                    val stopwatchViewModel: StopwatchViewModel = koinViewModel()
                    StopwatchScreen(viewModel = stopwatchViewModel)
                }
                1 -> {
                    val timerViewModel: TimerViewModel = koinViewModel()
                    TimerScreen(viewModel = timerViewModel)
                }
                2 -> {
                    val alarmViewModel: AlarmViewModel = koinViewModel()
                    AlarmScreen(viewModel = alarmViewModel)
                }
            }
        }
    }
}
