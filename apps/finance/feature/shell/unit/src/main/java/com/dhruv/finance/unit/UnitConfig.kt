// File named for what it holds (unit-converter config), not the `UnitCategoryOption` declaration
// name — same convention as DhruvNextTokens.kt/DhruvBrandColors.kt.
@file:Suppress("MatchingDeclarationName", "ktlint:standard:filename")

package com.dhruv.finance.unit

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.ui.graphics.vector.ImageVector

data class UnitCategoryOption(
    val label: String,
    val icon: ImageVector,
    val tab: UnitTab,
)

val UnitCategories =
    listOf(
        UnitCategoryOption("Length", Icons.Default.Straighten, UnitTab.LENGTH),
        UnitCategoryOption("Mass", Icons.Default.Scale, UnitTab.MASS),
        UnitCategoryOption("Temp", Icons.Default.Thermostat, UnitTab.TEMP),
        UnitCategoryOption("Area", Icons.Default.CropFree, UnitTab.AREA),
    )

val UnitCategoryLabels = UnitCategories.map { it.label }
val UnitCategoryIcons = UnitCategories.map { it.icon }
