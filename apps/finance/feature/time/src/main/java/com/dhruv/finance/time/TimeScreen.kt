package com.dhruv.finance.time

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.dhruv.core.ui.components.SegmentedRow
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.finance.time.stopwatch.StopwatchScreen
import com.dhruv.finance.time.stopwatch.StopwatchViewModel
import com.dhruv.finance.time.timer.TimerScreen
import com.dhruv.finance.time.timer.TimerViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun TimeScreen(
    viewModel: TimeViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        SegmentedRow(
            options = TimeTabs,
            selectedIndex = uiState.selectedTab,
            onSelected = { viewModel.selectTab(it) },
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = DhruvNextSpacing.screenGutter,
                vertical = DhruvNextSpacing.interCardGap,
            ),
        )

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
