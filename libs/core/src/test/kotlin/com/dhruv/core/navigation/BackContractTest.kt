package com.dhruv.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/** Covers NAV-FLOW rules N2/N3 (functional spec §4) and the QA catalog's back-contract scope. */
class BackContractTest {
    @Test
    fun `a shown detail route closes first, regardless of tab or nested state`() {
        assertEquals(
            BackAction.CLOSE_DETAIL,
            resolveBackAction(hasDetailRoute = true, activeTabHasNestedBackStack = true, currentTabIndex = 3),
        )
        assertEquals(
            BackAction.CLOSE_DETAIL,
            resolveBackAction(hasDetailRoute = true, activeTabHasNestedBackStack = false, currentTabIndex = 0),
        )
    }

    @Test
    fun `with no detail route, a tab's own nested back stack pops next`() {
        assertEquals(
            BackAction.POP_NESTED,
            resolveBackAction(hasDetailRoute = false, activeTabHasNestedBackStack = true, currentTabIndex = 3),
        )
    }

    @Test
    fun `with no detail route and no nested stack, a non-home tab returns to the first tab`() {
        assertEquals(
            BackAction.RETURN_TO_FIRST_TAB,
            resolveBackAction(hasDetailRoute = false, activeTabHasNestedBackStack = false, currentTabIndex = 2),
        )
    }

    @Test
    fun `at the first tab with nothing else to unwind, the app exits`() {
        assertEquals(
            BackAction.EXIT_APP,
            resolveBackAction(hasDetailRoute = false, activeTabHasNestedBackStack = false, currentTabIndex = 0),
        )
    }
}
