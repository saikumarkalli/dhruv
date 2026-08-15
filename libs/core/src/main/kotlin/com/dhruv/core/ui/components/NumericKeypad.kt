@file:Suppress("MatchingDeclarationName")

package com.dhruv.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * One key on a [NumericKeypad].
 *
 * [span] lets a key (e.g. a full-width "Save") take extra weight.
 * [isOperator] + [fillAccent] control the 3-role visual system (see [KeypadButton]).
 * [icon] renders an icon instead of text when set.
 */
data class KeypadKey(
    val label: String? = null,
    val icon: ImageVector? = null,
    val span: Int = 1,
    val isAccent: Boolean = false,
    val isOperator: Boolean = false,
    val fillAccent: Boolean = false,
    val solidAccent: Boolean = false,
    val contentColorOverride: Color? = null,
    val fontWeightOverride: FontWeight? = null,
    val fontSize: TextUnit? = null,
    val tag: String? = null,
)

/**
 * A generic key grid — DhruvNext §6.3 / §6.7. Delegates each cell to [KeypadButton] for
 * consistent ripple, haptics, and 3-role visuals. [onKeyPress] receives the tapped key's
 * label verbatim; [onKeyLongPress] fires on long-press if provided.
 */
@Composable
fun NumericKeypad(
    rows: List<List<KeypadKey>>,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier,
    onKeyLongPress: ((String) -> Unit)? = null,
    keyHeight: Dp = 56.dp,
    keyGap: Dp = 8.dp,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(keyGap)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(keyHeight),
                horizontalArrangement = Arrangement.spacedBy(keyGap),
            ) {
                row.forEach { key ->
                    val effectiveLabel = key.label ?: ""
                    KeypadButton(
                        modifier = Modifier.weight(key.span.toFloat()),
                        text = key.label,
                        icon = key.icon,
                        isOperator = key.isOperator || key.isAccent,
                        fillAccent = key.fillAccent || key.isAccent,
                        solidAccent = key.solidAccent,
                        contentColorOverride = key.contentColorOverride,
                        fontWeightOverride = key.fontWeightOverride,
                        tag = key.tag,
                        keyHeight = keyHeight,
                        fontSize = key.fontSize ?: com.dhruv.core.ui.theme.DhruvNextKeypad.digit,
                        onClick = { onKeyPress(effectiveLabel) },
                        onLongClick = onKeyLongPress?.let { { it(effectiveLabel) } },
                    )
                }
            }
        }
    }
}
