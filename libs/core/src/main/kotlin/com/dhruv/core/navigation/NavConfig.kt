package com.dhruv.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DonutSmall
import androidx.compose.material.icons.filled.Home
import com.dhruv.core.ui.components.BottomBarTab

val BottomNavItems = mapOf(
    TabKey.HOME to BottomBarTab("home", "Home", Icons.Default.Home),
    TabKey.CALC to BottomBarTab("calc", "Calc", Icons.Default.Calculate),
    TabKey.PLAN to BottomBarTab("plan", "Plan", Icons.Default.DonutSmall),
    TabKey.INSIGHTS to BottomBarTab("insights", "Insights", Icons.Default.BarChart),
)

fun TabKey.toBottomBarTab(): BottomBarTab =
    BottomNavItems[this] ?: error("Unknown tab: $this")
