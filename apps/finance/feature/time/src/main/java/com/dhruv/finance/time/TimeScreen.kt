package com.dhruv.finance.time

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.dhruv.finance.time.stopwatch.StopwatchScreen
import com.dhruv.finance.time.stopwatch.StopwatchViewModel
import com.dhruv.finance.time.timer.TimerScreen
import com.dhruv.finance.time.timer.TimerViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun TimeScreen(
    viewModel: TimeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("Stopwatch", "Timer")

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
            }
        }
    }
}
