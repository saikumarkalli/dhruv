package com.example.ui.time.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

@Composable
fun TimerScreen(viewModel: TimerViewModel) {
    val state by viewModel.state.collectAsState()
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val responsiveFontSize = (screenWidth * 0.16).sp

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.isInputMode) {
            // Input Mode
            Spacer(modifier = Modifier.weight(1f))
            Text("Presets", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.setPreset(1) }) { Text("1 Min") }
                Button(onClick = { viewModel.setPreset(5) }) { Text("5 Min") }
                Button(onClick = { viewModel.setPreset(10) }) { Text("10 Min") }
                Button(onClick = { viewModel.setPreset(25) }) { Text("25 Min") }
            }
            Spacer(modifier = Modifier.weight(1f))
        } else {
            // Countdown Mode
            Spacer(modifier = Modifier.weight(1f))
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(1f)) {
                val animatedProgress by animateFloatAsState(
                    targetValue = state.progress,
                    animationSpec = tween(50),
                    label = "progressAnim"
                )
                val primaryColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = formatCountdown(state.remainingTimeMs),
                    fontSize = responsiveFontSize,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { viewModel.resetTimer() }, 
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant, 
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.defaultMinSize(minWidth = 100.dp, minHeight = 56.dp)
                ) {
                    Text("Reset")
                }
                if (state.isRunning) {
                    Button(
                        onClick = { viewModel.pauseTimer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer, 
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.defaultMinSize(minWidth = 100.dp, minHeight = 56.dp)
                    ) { Text("Pause") }
                } else {
                    Button(
                        onClick = { viewModel.startTimer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary, 
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.defaultMinSize(minWidth = 100.dp, minHeight = 56.dp)
                    ) { Text("Resume") }
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
