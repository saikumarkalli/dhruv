// Named for the composable it hosts, not the small data class it also declares (BarChart.kt
// precedent).
@file:Suppress("MatchingDeclarationName")

package com.dhruv.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/** One row's identity + label for a [SelectionSheet] — e.g. a sector, a currency, an account. */
data class SelectionOption(
    val id: String,
    val label: String,
    val subtitle: String? = null,
)

/**
 * DhruvNext's picker sheet (design system B9, e.g. "Choose currency") — a [DhruvModalSheet] listing
 * [options] with a check mark on [selectedId]; tapping a row calls [onSelect] and the caller
 * decides whether to dismiss (the sheet doesn't dismiss itself, so a confirm-style picker is
 * possible too, not just tap-to-close).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionSheet(
    title: String,
    options: List<SelectionOption>,
    selectedId: String?,
    onSelect: (SelectionOption) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    val colors = LocalDhruvNextColors.current
    DhruvModalSheet(onDismissRequest = onDismissRequest, modifier = modifier, sheetState = sheetState) {
        Text(
            text = title,
            color = colors.tx,
            fontSize = DhruvNextType.title,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = DhruvNextSpacing.screenGutter, vertical = 8.dp),
        )
        ListGroup(
            modifier = Modifier.padding(horizontal = DhruvNextSpacing.screenGutter, vertical = 8.dp),
            rows =
                options.map { option ->
                    {
                        ListGroupRow(
                            title = option.label,
                            subtitle = option.subtitle,
                            showChevron = false,
                            onClick = { onSelect(option) },
                            trailing = {
                                if (option.id == selectedId) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = colors.acc,
                                    )
                                }
                            },
                        )
                    }
                },
        )
    }
}
