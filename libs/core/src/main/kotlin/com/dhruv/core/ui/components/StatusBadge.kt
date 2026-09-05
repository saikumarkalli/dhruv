package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/** Which token [StatusBadge] tints its dot with. */
enum class StatusBadgeTone { Success, Warning, Error, Accent }

/**
 * A small status dot (design batch B7) — success/warning/error/accent. Deliberately separate from
 * [CountBadge]: that component renders a number inside a pill, this one renders no text at all, so
 * extending it would mean threading an optional-count path through a component whose whole
 * contract is "there is a count" (design system §5.3's "extend, don't duplicate" is about not
 * re-inventing the counting mechanism — a pure status dot has none to re-invent).
 */
@Composable
fun StatusBadge(
    tone: StatusBadgeTone,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val colors = LocalDhruvNextColors.current
    val tint =
        when (tone) {
            StatusBadgeTone.Success -> colors.pos
            StatusBadgeTone.Warning -> colors.warn
            StatusBadgeTone.Error -> colors.neg
            StatusBadgeTone.Accent -> colors.acc
        }
    Box(
        modifier =
            modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(tint)
                .let { if (description != null) it.semantics { contentDescription = description } else it },
    )
}
