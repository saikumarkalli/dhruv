package com.dhruv.finance.time.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
fun TimerScreen(viewModel: TimerViewModel) {
    val colors = LocalDhruvNextColors.current
    val state by viewModel.state.collectAsState()
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val responsiveFontSize = (screenWidth * 0.16).sp

    Column(
        modifier = Modifier.fillMaxSize().padding(DhruvNextSpacing.screenGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.isInputMode) {
            // Input Mode
            Spacer(modifier = Modifier.weight(1f))
            Text("Presets", color = colors.tx2, fontSize = DhruvNextType.body)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NxButton(text = "1 Min", onClick = { viewModel.setPreset(1) }, variant = NxButtonVariant.Soft)
                NxButton(text = "5 Min", onClick = { viewModel.setPreset(5) }, variant = NxButtonVariant.Soft)
                NxButton(text = "10 Min", onClick = { viewModel.setPreset(10) }, variant = NxButtonVariant.Soft)
                NxButton(text = "25 Min", onClick = { viewModel.setPreset(25) }, variant = NxButtonVariant.Soft)
            }
            Spacer(modifier = Modifier.weight(1f))
        } else {
            // Countdown Mode
            Spacer(modifier = Modifier.weight(1f))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(1f)) {
                val animatedProgress by animateFloatAsState(
                    targetValue = state.progress,
                    animationSpec = tween(50),
                    label = "progressAnim",
                )
                val primaryColor = colors.acc
                val trackColor = colors.surf2

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round),
                    )
                }

                Text(
                    text = formatCountdown(state.remainingTimeMs),
                    fontSize = responsiveFontSize,
                    fontWeight = FontWeight.Light,
                    color = colors.tx,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NxButton(
                    text = "Reset",
                    onClick = { viewModel.resetTimer() },
                    variant = NxButtonVariant.Ghost,
                    modifier = Modifier.defaultMinSize(minWidth = 100.dp, minHeight = 56.dp),
                )
                if (state.isRunning) {
                    NxButton(
                        text = "Pause",
                        onClick = { viewModel.pauseTimer() },
                        variant = NxButtonVariant.Destructive,
                        modifier = Modifier.defaultMinSize(minWidth = 100.dp, minHeight = 56.dp),
                    )
                } else {
                    NxButton(
                        text = "Resume",
                        onClick = { viewModel.startTimer() },
                        variant = NxButtonVariant.Primary,
                        modifier = Modifier.defaultMinSize(minWidth = 100.dp, minHeight = 56.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun formatCountdown(ms: Long): String {
    val h = ms / 3600000
    val m = (ms % 3600000) / 60000
    val s = (ms % 60000) / 1000
    return if (h > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }
}
