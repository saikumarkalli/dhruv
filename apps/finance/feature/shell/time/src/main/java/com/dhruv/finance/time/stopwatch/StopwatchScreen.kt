package com.dhruv.finance.time.stopwatch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxButtonVariant
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import java.util.Locale

@Composable
fun StopwatchScreen(viewModel: StopwatchViewModel) {
    val state by viewModel.state.collectAsState()
    val colors = LocalDhruvNextColors.current
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val responsiveFontSize = (screenWidth * 0.18).sp

    Column(
        modifier = Modifier.fillMaxSize().padding(DhruvNextSpacing.screenGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = formatTime(state.timeMs),
            fontSize = responsiveFontSize,
            fontWeight = FontWeight.Light,
            color = colors.tx,
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            NxButton(
                text = if (state.isRunning) "Lap" else "Reset",
                onClick = { viewModel.lapOrReset() },
                variant = NxButtonVariant.Ghost,
            )

            NxButton(
                text = if (state.isRunning) "Stop" else "Start",
                onClick = { viewModel.toggleStartStop() },
                variant = if (state.isRunning) NxButtonVariant.Destructive else NxButtonVariant.Primary,
            )
        }

        Spacer(modifier = Modifier.height(DhruvNextSpacing.sectionGap))

        LazyColumn(modifier = Modifier.weight(2f).fillMaxWidth()) {
            items(state.laps) { lap ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = DhruvNextSpacing.interCardGap),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Lap ${lap.lapNumber}", color = colors.tx2, fontSize = DhruvNextType.body)
                    Text(
                        formatTime(lap.lapTimeMs),
                        fontWeight = FontWeight.Medium,
                        fontSize = DhruvNextType.body,
                        color = colors.tx,
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = colors.line.copy(alpha = 0.5f))
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
