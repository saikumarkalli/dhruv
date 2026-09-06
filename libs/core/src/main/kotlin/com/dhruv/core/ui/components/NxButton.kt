// File named for the composable it exports (`NxButton`), not `NxButtonVariant` — the enum is a
// supporting type for the button's own API, not the file's subject.
@file:Suppress("MatchingDeclarationName", "ktlint:standard:filename")

package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

enum class NxButtonVariant { Primary, Soft, Outline, Ghost, Destructive }

/** Button height/type-scale preset — Small for inline/dialog actions, Medium (default) elsewhere. */
enum class NxButtonSize { Small, Medium }

@Composable
fun NxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NxButtonVariant = NxButtonVariant.Primary,
    size: NxButtonSize = NxButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    block: Boolean = false,
) {
    val colors = LocalDhruvNextColors.current
    val shape = RoundedCornerShape(DhruvNextRadii.innerTile)
    val clickable = enabled && !loading
    val background =
        when (variant) {
            NxButtonVariant.Primary -> colors.acc
            NxButtonVariant.Soft -> colors.accSoft
            NxButtonVariant.Outline -> Color.Transparent
            NxButtonVariant.Ghost -> Color.Transparent
            NxButtonVariant.Destructive -> colors.neg
        }
    val textColor =
        when (variant) {
            NxButtonVariant.Primary -> colors.onAcc
            NxButtonVariant.Soft -> colors.acc
            NxButtonVariant.Outline -> colors.acc
            NxButtonVariant.Ghost -> colors.tx2
            NxButtonVariant.Destructive -> Color.White
        }
    val isDisabled = !enabled || loading
    val vPad = if (size == NxButtonSize.Small) 9.dp else 13.dp
    val hPad = if (size == NxButtonSize.Small) 14.dp else 20.dp
    val fontSize = if (size == NxButtonSize.Small) DhruvNextType.body else DhruvNextType.cardTitle
    Row(
        modifier =
            modifier
                .let { if (block) it.fillMaxWidth() else it }
                .alpha(if (isDisabled) 0.7f else 1f)
                .clip(shape)
                .background(background)
                .let {
                    when (variant) {
                        NxButtonVariant.Outline -> it.border(1.5.dp, colors.accLine, shape)
                        NxButtonVariant.Ghost -> it.border(1.dp, colors.line, shape)
                        else -> it
                    }
                }.clickable(enabled = !isDisabled, onClick = onClick)
                .padding(horizontal = hPad, vertical = vPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (block) Arrangement.Center else Arrangement.Start,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(if (size == NxButtonSize.Small) 14.dp else 18.dp),
                color = textColor,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = text,
                color = textColor,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun NxIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = LocalDhruvNextColors.current.tx2,
) {
    val colors = LocalDhruvNextColors.current
    val shape = RoundedCornerShape(DhruvNextRadii.innerTile)
    Box(
        modifier =
            modifier
                .size(44.dp)
                .clip(shape)
                .background(colors.surf2)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}
