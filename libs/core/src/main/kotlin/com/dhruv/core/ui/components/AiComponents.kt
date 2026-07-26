package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/** DhruvNext §6.2's Home AI insight strip: accent-soft background, headline + pill actions. */
@Composable
fun AiInsightStrip(
    text: String,
    modifier: Modifier = Modifier,
    actionLabels: List<Pair<String, () -> Unit>> = emptyList(),
) {
    val colors = LocalDhruvNextColors.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DhruvNextRadii.innerTile))
                .background(colors.accSoft)
                .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = colors.acc)
            Text(
                text = text,
                modifier = Modifier.padding(start = 10.dp),
                color = colors.tx,
                fontSize = 13.5f.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        if (actionLabels.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actionLabels.forEach { (label, onClick) ->
                    Pill(label = label, onClick = onClick)
                }
            }
        }
    }
}

/** Tone for a [SmartInsightCard] — determines its icon and accent. */
enum class InsightTone { Positive, Negative, Warning }

/** DhruvNext §6.2/§6.9's smart-insight / notification card template. */
@Composable
fun SmartInsightCard(
    tone: InsightTone,
    headline: String,
    explanation: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val (icon, tint) =
        when (tone) {
            InsightTone.Positive -> Icons.AutoMirrored.Filled.TrendingUp to colors.pos
            InsightTone.Negative -> Icons.AutoMirrored.Filled.TrendingDown to colors.neg
            InsightTone.Warning -> Icons.Default.Warning to colors.warn
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DhruvNextRadii.innerTile))
                .background(colors.surf2)
                .padding(14.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(text = headline, color = colors.tx, fontSize = 13.5f.sp, fontWeight = FontWeight.Bold)
            Text(text = explanation, color = colors.tx2, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** DhruvNext §5's floating "Ask Dhruv" pill — caller positions it (typically bottom-end in a Box). */
@Composable
fun AskPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Ask Dhruv",
) {
    Pill(label = label, modifier = modifier, filled = true, leadingIcon = Icons.Default.AutoAwesome, onClick = onClick)
}

/** DhruvNext §6.8's chat bubble — user (right, accent-filled) vs assistant (left, card). */
@Composable
fun ChatBubble(
    text: String,
    isUser: Boolean,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val colors = LocalDhruvNextColors.current
    val background = if (isUser) colors.acc else colors.surf
    val textColor = if (isUser) colors.onAcc else colors.tx

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = BUBBLE_MAX_WIDTH)
                    .clip(RoundedCornerShape(DhruvNextRadii.innerTile))
                    .background(background)
                    .padding(12.dp),
        ) {
            Text(text = text, color = textColor, fontSize = 13.5f.sp)
            trailingContent?.invoke()
        }
    }
}

private val BUBBLE_MAX_WIDTH = 280.dp
