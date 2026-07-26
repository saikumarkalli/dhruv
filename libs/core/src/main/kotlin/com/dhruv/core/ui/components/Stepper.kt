package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/** DhruvNext §6.4's people-count stepper: "−  N  +". */
@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = Int.MAX_VALUE,
) {
    val colors = LocalDhruvNextColors.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        StepperButton(label = "−", enabled = value > min, onClick = { onValueChange((value - 1).coerceAtLeast(min)) })
        Text(
            text = "$value",
            color = colors.tx,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        StepperButton(label = "+", enabled = value < max, onClick = { onValueChange((value + 1).coerceAtMost(max)) })
    }
}

@Composable
private fun StepperButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalDhruvNextColors.current
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.surf2)
                .let { if (enabled) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = if (enabled) colors.tx else colors.tx3, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
