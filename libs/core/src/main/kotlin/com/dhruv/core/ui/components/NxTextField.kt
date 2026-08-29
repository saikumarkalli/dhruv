package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

private val LabelSize = 12.sp
private val CornerRadius = 13.dp
private val BorderWidth = 1.5.dp
private val FocusRingWidth = 3.dp
private val HorizontalPadding = 14.dp
private val VerticalPadding = 11.dp
private val LabelBottomSpacing = 6.dp
private val PrefixSuffixSpacing = 6.dp

@Composable
fun NxTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    prefix: String? = null,
    suffix: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    /** Design system §5.3's built-but-narrower gap, closed here (T097): non-null renders in
     * [com.dhruv.core.ui.theme.DhruvNextColors.neg] below the field and switches the border to it,
     * same visual language [SettingsRowRenderer]'s row-level error text already uses. */
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = LocalDhruvNextColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(CornerRadius)

    val borderColor =
        when {
            errorMessage != null -> colors.neg
            isFocused -> colors.acc
            else -> colors.line
        }
    val textStyle =
        TextStyle(
            color = colors.tx,
            fontSize = DhruvNextType.cardTitle,
        )

    Column(modifier = modifier.alpha(if (enabled) 1f else 0.45f)) {
        if (label != null) {
            Text(
                text = label,
                color = colors.tx2,
                fontSize = LabelSize,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.padding(bottom = LabelBottomSpacing))
        }

        val focusRingShape = RoundedCornerShape(CornerRadius + 4.dp)
        Box(
            modifier =
                Modifier
                    .border(
                        width = FocusRingWidth,
                        color = if (isFocused) colors.accSoft else Color.Transparent,
                        shape = focusRingShape,
                    ).padding(2.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(colors.surf)
                        .border(BorderWidth, borderColor, shape)
                        .padding(horizontal = HorizontalPadding, vertical = VerticalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (prefix != null) {
                    Text(
                        text = prefix,
                        color = colors.tx3,
                        fontSize = DhruvNextType.cardTitle,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(Modifier.width(PrefixSuffixSpacing))
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            color = colors.tx3,
                            fontSize = DhruvNextType.cardTitle,
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier,
                        enabled = enabled,
                        readOnly = readOnly,
                        textStyle = textStyle,
                        singleLine = singleLine,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        interactionSource = interactionSource,
                        cursorBrush = SolidColor(colors.acc),
                    )
                }

                if (suffix != null) {
                    Spacer(Modifier.width(PrefixSuffixSpacing))
                    Text(
                        text = suffix,
                        color = colors.tx3,
                        fontSize = DhruvNextType.cardTitle,
                    )
                }
            }
        }

        if (errorMessage != null) {
            Spacer(Modifier.padding(top = LabelBottomSpacing))
            Text(
                text = errorMessage,
                color = colors.neg,
                fontSize = LabelSize,
            )
        }
    }
}
