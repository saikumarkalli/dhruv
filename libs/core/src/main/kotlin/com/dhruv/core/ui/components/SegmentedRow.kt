package com.dhruv.core.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * DhruvNext-styled convenience wrapper over Material3's
 * [SingleChoiceSegmentedButtonRow]/[SegmentedButton] (the app-design-standard's own §5 note: this
 * is a styling convention, not a new primitive) — accent-colored selected segment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedRow(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selectedIndex,
                onClick = { onSelected(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors =
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = colors.accSoft,
                        activeContentColor = colors.acc,
                        inactiveContainerColor = colors.surf,
                        inactiveContentColor = colors.tx2,
                    ),
            ) {
                Text(label)
            }
        }
    }
}
