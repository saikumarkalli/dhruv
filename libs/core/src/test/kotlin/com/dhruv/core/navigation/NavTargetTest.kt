package com.dhruv.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavTargetTest {
    private val allTabs = listOf(TabKey.HOME, TabKey.MONEY, TabKey.CALC, TabKey.PLAN, TabKey.INSIGHTS)

    @Test
    fun `SelectTab resolves to its own tab`() {
        assertEquals(TabKey.INSIGHTS, NavTarget.SelectTab(TabKey.INSIGHTS).tab)
    }

    @Test
    fun `OpenPlanTool always belongs to the Plan tab`() {
        assertEquals(TabKey.PLAN, NavTarget.OpenPlanTool(PlanTool.LOAN).tab)
    }

    @Test
    fun `pageIndexFor finds the tab by key, not fixed position`() {
        assertEquals(0, allTabs.pageIndexFor(NavTarget.SelectTab(TabKey.HOME)))
        assertEquals(1, allTabs.pageIndexFor(NavTarget.SelectTab(TabKey.MONEY)))
        assertEquals(3, allTabs.pageIndexFor(NavTarget.SelectTab(TabKey.PLAN)))
        assertEquals(4, allTabs.pageIndexFor(NavTarget.SelectTab(TabKey.INSIGHTS)))
    }

    @Test
    fun `pageIndexFor falls back to the first visible tab when the target tab is hidden`() {
        val insightsHidden = listOf(TabKey.HOME, TabKey.MONEY, TabKey.CALC, TabKey.PLAN)
        assertEquals(0, insightsHidden.pageIndexFor(NavTarget.SelectTab(TabKey.INSIGHTS)))
    }

    @Test
    fun `pageIndexFor tracks a reordered tab list by key`() {
        val reordered = listOf(TabKey.CALC, TabKey.HOME, TabKey.PLAN, TabKey.INSIGHTS, TabKey.MONEY)
        assertEquals(1, reordered.pageIndexFor(NavTarget.SelectTab(TabKey.HOME)))
    }
}
