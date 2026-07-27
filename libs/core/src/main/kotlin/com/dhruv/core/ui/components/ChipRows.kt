package com.dhruv.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * A horizontally-scrolling row of selectable [Pill]s. Backs both [ModeChipRow] (Calc-tab
 * Basic/Scientific/Currency/Units, DhruvNext §6.3) and [PeriodChipRow] (Insights month/quarter/FY
 * picker, §6.5) — identical shape, different call sites. [icons], when given, must be the same
 * size as [options]; a null entry at an index omits that option's leading icon.
 */
@Composable
private fun ChipSelectorRow(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector?>? = null,
    strong: Boolean = false,
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options.size) { index ->
            Pill(
                label = options[index],
                selected = index == selectedIndex,
                strong = strong,
                leadingIcon = icons?.getOrNull(index),
                onClick = { onSelected(index) },
            )
        }
    }
}

/**
 * Calc-tab mode selector: Basic / Scientific / Currency / Units. Renders via [Pill]'s
 * strong/segment variant (solid `tx` selected bg vs. a `line`-bordered `surf` unselected chip),
 * per DhruvNext §6.3.
 */
@Composable
fun ModeChipRow(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) = ChipSelectorRow(options, selectedIndex, onSelected, modifier, strong = true)

/**
 * Icon-carrying overload of [ModeChipRow] — each option may show a leading icon (e.g. Currency/
 * Units modes in the calc mode row). [icons] must be the same size as [options]; pass `null` for
 * an option with no icon.
 */
@Composable
fun ModeChipRow(
    options: List<String>,
    icons: List<ImageVector?>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) = ChipSelectorRow(options, selectedIndex, onSelected, modifier, icons = icons, strong = true)

/** Insights period selector: Jul 2026 / Jun / May / Quarter / FY. Keeps [Pill]'s accent-soft
 * selected look (unchanged by the strong/segment variant added for [ModeChipRow]). */
@Composable
fun PeriodChipRow(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) = ChipSelectorRow(options, selectedIndex, onSelected, modifier)
