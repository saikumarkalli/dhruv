// Named for the composable it hosts, not the small data class it also declares.
@file:Suppress("MatchingDeclarationName")

package com.dhruv.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/** One tappable preset on a [SliderWithPresets] — e.g. "10L" → 10_00_000f. */
data class SliderPreset(
    val label: String,
    val value: Float,
)

/**
 * DhruvNext §6.4's planner sliders (loan amount, interest rate, tenure, SIP instalment, …): a
 * label + current-value row, a [Slider], and a row of preset chips underneath.
 */
@Composable
fun SliderWithPresets(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    presets: List<SliderPreset> = emptyList(),
) {
    val colors = LocalDhruvNextColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, color = colors.tx2, fontSize = 13.sp)
            Text(text = valueText, color = colors.tx, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors =
                SliderDefaults.colors(
                    thumbColor = colors.acc,
                    activeTrackColor = colors.acc,
                    inactiveTrackColor = colors.surf2,
                ),
        )
        if (presets.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presets) { preset ->
                    Chip(
                        label = preset.label,
                        selected = value == preset.value,
                        onClick = { onValueChange(preset.value) },
                    )
                }
            }
        }
    }
}
