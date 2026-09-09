package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * A multi-line text field with helper text below (design batch B6 remainder) — D3's note field.
 * Not a `singleLine = false` [NxTextField] because a note field wants a fixed minimum height and
 * a persistent helper line, not just an error message that appears conditionally.
 */
@Composable
fun NxTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    minLines: Int = 3,
    enabled: Boolean = true,
) {
    val colors = LocalDhruvNextColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(13.dp)
    val borderColor = if (isFocused) colors.acc else colors.line

    Column(modifier = modifier) {
        if (label != null) {
            Text(text = label, color = colors.tx2, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.padding(bottom = 6.dp))
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = (minLines * 22).dp)
                    .clip(shape)
                    .background(colors.surf)
                    .border(1.5.dp, borderColor, shape)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
        ) {
            if (value.isEmpty() && placeholder != null) {
                Text(text = placeholder, color = colors.tx3, fontSize = DhruvNextType.body)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                minLines = minLines,
                textStyle = TextStyle(color = colors.tx, fontSize = DhruvNextType.body),
                interactionSource = interactionSource,
                cursorBrush = SolidColor(colors.acc),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (helperText != null) {
            Text(
                text = helperText,
                color = colors.tx3,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
