// Named for the composable it hosts, not the small data class it also declares.
@file:Suppress("MatchingDeclarationName")

package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/** One key on a [NumericKeypad]. [span] lets a key (e.g. a full-width "Save") take extra weight. */
data class KeypadKey(
    val label: String,
    val span: Int = 1,
    val isAccent: Boolean = false,
)

/**
 * A generic key grid — DhruvNext's Calc-tab keypad and AddTxn's 4×4 + full-width "Save" key
 * (§6.3, §6.7). Purely presentational: [onKeyPress] receives the tapped key's label verbatim.
 */
@Composable
fun NumericKeypad(
    rows: List<List<KeypadKey>>,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyHeight: androidx.compose.ui.unit.Dp = 56.dp,
) {
    val colors = LocalDhruvNextColors.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(keyHeight),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    val background = if (key.isAccent) colors.acc else colors.surf2
                    val textColor = if (key.isAccent) colors.onAcc else colors.tx
                    Box(
                        modifier =
                            Modifier
                                .weight(key.span.toFloat())
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(background)
                                .clickable { onKeyPress(key.label) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = key.label, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
