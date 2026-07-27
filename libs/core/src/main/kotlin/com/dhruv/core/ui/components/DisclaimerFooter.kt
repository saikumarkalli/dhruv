package com.dhruv.core.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * A persistent small-print line — AI disclaimers ("Estimate from the numbers on this device. Not
 * financial advice."), retirement-projection caveats.
 */
@Composable
fun DisclaimerFooter(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Text(text = text, modifier = modifier, color = colors.tx3, fontSize = DhruvNextType.meta)
}
