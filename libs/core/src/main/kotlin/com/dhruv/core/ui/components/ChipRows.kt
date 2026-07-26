package com.dhruv.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A horizontally-scrolling row of selectable [Pill]s. Backs both [ModeChipRow] (Calc-tab
 * Basic/Scientific/Currency/Units, DhruvNext §6.3) and [PeriodChipRow] (Insights month/quarter/FY
 * picker, §6.5) — identical shape, different call sites.
 */
@Composable
private fun ChipSelectorRow(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options.size) { index ->
            Pill(label = options[index], selected = index == selectedIndex, onClick = { onSelected(index) })
        }
    }
}

/** Calc-tab mode selector: Basic / Scientific / Currency / Units. */
@Composable
fun ModeChipRow(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) = ChipSelectorRow(options, selectedIndex, onSelected, modifier)

/** Insights period selector: Jul 2026 / Jun / May / Quarter / FY. */
@Composable
fun PeriodChipRow(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) = ChipSelectorRow(options, selectedIndex, onSelected, modifier)
