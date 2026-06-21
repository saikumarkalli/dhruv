package com.dhruv.finance.mocks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp

private data class MockTab(val key: String, val label: String, val icon: ImageVector)

private val mockTabs = listOf(
    MockTab("Calc",      "Calc",      Icons.Default.Calculate),
    MockTab("Converter", "Converter", Icons.Default.SwapHoriz),
    MockTab("Date",      "Date",      Icons.Default.DateRange),
    MockTab("Finance",   "Finance",   Icons.AutoMirrored.Filled.TrendingUp),
    MockTab("Time",      "Time",      Icons.Default.AccessTime),
    MockTab("Assistant", "Assistant", Icons.Default.AutoAwesome),
    MockTab("Settings",  "Settings",  Icons.Default.Settings),
)

/** Reusable bottom navigation bar for Previews. [selected] must match one of the tab keys above. */
@Composable
internal fun MockBottomNav(selected: String) {
    NavigationBar(containerColor = Color(0xFF1A0001)) {
        mockTabs.forEach { tab ->
            NavigationBarItem(
                selected = tab.key == selected,
                onClick = {},
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF665080),
                    selectedTextColor = Color(0xFF665080),
                    unselectedIconColor = Color(0xFF9E9E9E),
                    unselectedTextColor = Color(0xFF9E9E9E),
                    indicatorColor = Color(0xFF0D1B2A)
                )
            )
        }
    }
}
