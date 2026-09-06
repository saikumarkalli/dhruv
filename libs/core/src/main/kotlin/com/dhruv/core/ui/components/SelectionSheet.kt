package com.dhruv.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
 * One selectable row's identity + label for [SelectionSheet]/[NxSelect] — e.g. a category, an
 * account, a sector. [subtitle] is optional secondary text under the label (a currency's code, an
 * account's balance).
 */
data class SelectionOption(val id: String, val label: String, val subtitle: String? = null)

/**
 * A bottom-sheet picker (design batch B9) over [DhruvModalSheet] — single or multi-select list of
 * [SelectionOption]s with a title. [multiSelect] toggles a checkmark-style multi-pick (D5's
 * category filter, "+N more") vs a single tap-to-choose-and-dismiss list (D3's category/account
 * fields, C4's sector/liability-type pickers) — a single-select tap calls [onSelectionChanged]
 * with a one-element set and dismisses itself; a multi-select tap toggles membership and leaves
 * the sheet open for [onDismissRequest] (typically a "Show N" button) to close.
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = option.label, color = colors.tx, fontSize = DhruvNextType.body)
                        option.subtitle?.let {
                            Text(text = it, color = colors.tx3, fontSize = DhruvNextType.meta)
                        }
                    }
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
