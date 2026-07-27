package com.dhruv.finance.app.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.components.SegmentedRow
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * Reusable DhruvNext-styled row primitives for the Settings screen (replaces the old
 * `SettingsCategory`/`SettingsClickableItem`/`SettingsToggleItem`/`SettingsSegmentedControlItem`
 * pre-DhruvNext composables — grouping/dividers/chrome now come from `:libs:core`'s
 * [com.dhruv.core.ui.components.ListGroup]/[com.dhruv.core.ui.components.ListGroupRow], and this
 * file only holds the row *content* this screen needs that core doesn't already provide).
 */

/** Disabled/greyed alpha for not-yet-built rows — matches [com.dhruv.core.ui.components.QuickActionTile]'s convention. */
internal const val SETTINGS_DISABLED_ALPHA = 0.55f

/**
 * A neutral placeholder row for a feature that isn't built yet (e.g. "Export my data").
 * Non-interactive by design — do not wire [onClick]-shaped behaviour to anything that pretends
 * this works.
 */
@Composable
fun PlaceholderRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(SETTINGS_DISABLED_ALPHA)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = colors.tx, fontWeight = FontWeight.Medium, fontSize = DhruvNextType.cardTitle)
            Text(text = subtitle, color = colors.tx2, fontSize = DhruvNextType.meta)
        }
        Text(text = "Soon", color = colors.tx3, fontSize = DhruvNextType.meta, fontWeight = FontWeight.Bold)
    }
}

/**
 * A destructive-styled row (title tinted [com.dhruv.core.ui.theme.DhruvNextColors.neg]).
 * When [onClick] is null the row renders disabled/greyed — a placeholder for a destructive action
 * that isn't built yet (e.g. "Delete everything"). When non-null it's the one real action here
 * (e.g. "Clear history").
 */
@Composable
fun DangerRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailingLabel: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalDhruvNextColors.current
    val enabled = onClick != null
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .alpha(if (enabled) 1f else SETTINGS_DISABLED_ALPHA)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = colors.neg, fontWeight = FontWeight.Medium, fontSize = DhruvNextType.cardTitle)
            if (subtitle != null) {
                Text(text = subtitle, color = colors.tx2, fontSize = DhruvNextType.meta)
            }
        }
        if (trailingLabel != null) {
            Text(text = trailingLabel, color = colors.tx3, fontSize = DhruvNextType.meta, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * A [Switch] row that is permanently disabled — for settings with no backing implementation yet
 * (e.g. "Use wallpaper colours" / Material You — there is no dynamic-color plumbing in this
 * codebase today). A one-line comment at the call site is the "future hookup" note.
 */
@Composable
fun DisabledSwitchRow(
    label: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(SETTINGS_DISABLED_ALPHA)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = colors.tx, fontSize = DhruvNextType.cardTitle, fontWeight = FontWeight.Medium)
            Text(text = description, color = colors.tx2, fontSize = DhruvNextType.meta)
        }
        Switch(checked = false, onCheckedChange = null, enabled = false)
    }
}

/** A label above a full-width [SegmentedRow] — "Theme", "Angle mode", any single-choice preference. */
@Composable
fun LabeledSegmentedRow(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(text = label, color = colors.tx, fontSize = DhruvNextType.cardTitle, fontWeight = FontWeight.Medium)
        SegmentedRow(
            options = options,
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
}

/**
 * The DhruvNext §6.9 4-swatch global accent picker (ADR-0024 decision 2). Each swatch is a
 * 48.dp touch target (accessibility floor) with a smaller colored dot inside; the selected swatch
 * shows a check mark (never color alone — [selected] is also exposed via [Role.RadioButton]
 * semantics for TalkBack) tinted for contrast against its own background.
 */
@Composable
fun AccentColorPickerRow(
    swatches: List<AccentSwatch>,
    selectedHex: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(text = "Accent color", color = colors.tx, fontSize = DhruvNextType.cardTitle, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            swatches.forEach { swatch ->
                AccentSwatchButton(
                    swatch = swatch,
                    selected = swatch.hex.equals(selectedHex, ignoreCase = true),
                    onClick = { onColorSelected(swatch.hex) },
                )
            }
        }
    }
}

/** One accent option — id/label from [com.dhruv.core.ui.theme.ColorOptions], hex is "#RRGGBB". */
data class AccentSwatch(
    val id: String,
    val label: String,
    val hex: String,
    val color: Color,
)

@Composable
private fun AccentSwatchButton(
    swatch: AccentSwatch,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(targetValue = if (selected) 1f else 0.86f, label = "accentSwatchScale")
    val onSwatchColor = if (swatch.color.isLight()) Color.Black else Color.White
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(swatch.color),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "${swatch.label} selected",
                tint = onSwatchColor,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Perceived (not CIE-precise) luminance — enough to pick a readable check-mark tint. */
private fun Color.isLight(): Boolean = (0.299f * red + 0.587f * green + 0.114f * blue) > 0.6f
