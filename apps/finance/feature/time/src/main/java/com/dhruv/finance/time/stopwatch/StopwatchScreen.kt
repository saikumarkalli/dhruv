package com.dhruv.finance.time.stopwatch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun StopwatchScreen(viewModel: StopwatchViewModel) {
    val state by viewModel.state.collectAsState()
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val responsiveFontSize = (screenWidth * 0.18).sp

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = formatTime(state.timeMs),
            fontSize = responsiveFontSize,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(
                onClick = { viewModel.lapOrReset() },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                modifier = Modifier.defaultMinSize(minWidth = 100.dp, minHeight = 56.dp),
            ) {
                Text(if (state.isRunning) "Lap" else "Reset")
            }

            Button(
                onClick = { viewModel.toggleStartStop() },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            if (state.isRunning) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        contentColor =
                            if (state.isRunning) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            },
                    ),
                modifier = Modifier.defaultMinSize(minWidth = 100.dp, minHeight = 56.dp),
            ) {
                Text(if (state.isRunning) "Stop" else "Start")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        LazyColumn(modifier = Modifier.weight(2f).fillMaxWidth()) {
            items(state.laps) { lap ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Lap ${lap.lapNumber}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    Text(
                        formatTime(lap.lapTimeMs),
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val minutes = (ms / 60000) % 60
    val seconds = (ms / 1000) % 60
    val centis = (ms / 10) % 100
    return String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, centis)
}
