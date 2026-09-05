package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * One selectable row's label + stable id, for [SelectionSheet]/[NxSelect].
 */
data class SelectionOption(val id: String, val label: String)

/**
 * A bottom-sheet picker (design batch B9) over [DhruvModalSheet] — single or multi-select list of
 * [SelectionOption]s with a title and a Done action. [multiSelect] toggles a checkmark-style
 * multi-pick (D5's category filter, "+N more") vs a single tap-to-choose-and-dismiss list
 * (D3's category/account fields).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionSheet(
    title: String,
    options: List<SelectionOption>,
    selectedIds: Set<String>,
    onDismissRequest: () -> Unit,
    onSelectionChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    multiSelect: Boolean = false,
) {
    val colors = LocalDhruvNextColors.current
    DhruvModalSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
        Text(
            text = title,
            color = colors.tx,
            fontSize = DhruvNextType.title,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = DhruvNextSpacing.screenGutter, vertical = 8.dp),
        )
        LazyColumn {
            items(options, key = { it.id }) { option ->
                val isSelected = option.id in selectedIds
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (multiSelect) {
                                    onSelectionChanged(
                                        if (isSelected) selectedIds - option.id else selectedIds + option.id,
                                    )
                                } else {
                                    onSelectionChanged(setOf(option.id))
                                    onDismissRequest()
                                }
                            }
                            .padding(horizontal = DhruvNextSpacing.screenGutter, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = option.label,
                        color = colors.tx,
                        fontSize = DhruvNextType.body,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = colors.acc)
                    }
                }
                HorizontalDivider(color = colors.line, thickness = 1.dp)
            }
        }
        if (multiSelect) {
            Row(modifier = Modifier.padding(DhruvNextSpacing.screenGutter)) {
                NxButton(text = "Show ${selectedIds.size.let { if (it == 0) "all" else "$it selected" }}", onClick = onDismissRequest, block = true)
            }
        }
    }
}

/** Trailing chevron/summary count used by [NxSelect]'s "+N more" style summary. */
@Composable
private fun SelectionCountDot(count: Int) {
    if (count <= 0) return
    val colors = LocalDhruvNextColors.current
    Column(
        modifier =
            Modifier
                .size(20.dp)
                .background(colors.accSoft, CircleShape),
    ) {
        Text(
            text = "$count",
            color = colors.acc,
            fontSize = DhruvNextType.meta,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(2.dp),
        )
    }
}

/**
 * A read-only, tap-to-open field styled like [NxTextField] that opens a [SelectionSheet] — the
 * category/account/type picker shape D3/D5 need (design batch B6). [onClick] is expected to show
 * the caller's own [SelectionSheet]; this component only renders the closed-state trigger.
 */
@Composable
fun NxSelect(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select",
    errorMessage: String? = null,
    extraSelectedCount: Int = 0,
) {
    val colors = LocalDhruvNextColors.current
    val borderColor = if (errorMessage != null) colors.neg else colors.line
    Column(modifier = modifier) {
        Text(text = label, color = colors.tx2, fontSize = DhruvNextType.meta, modifier = Modifier.padding(bottom = 4.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value.ifBlank { placeholder },
                color = if (value.isBlank()) colors.tx3 else colors.tx,
                fontSize = DhruvNextType.body,
                modifier = Modifier.weight(1f),
            )
            SelectionCountDot(extraSelectedCount)
        }
        HorizontalDivider(color = borderColor, thickness = 1.dp)
        if (errorMessage != null) {
            Text(text = errorMessage, color = colors.neg, fontSize = DhruvNextType.meta, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
