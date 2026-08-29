package com.dhruv.finance.app.ui.settings

import com.dhruv.core.navigation.NavTarget
import com.dhruv.core.navigation.TabKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeldTargetTest {
    // SET-FLOW-002: a target arriving while locked is held and dispatched exactly once after unlock.
    @Test
    fun `a held target is returned once, then cleared`() {
        val store = HeldTargetStore()
        val target = NavTarget.SelectTab(TabKey.PLAN)

        store.hold(target)

        assertEquals(target, store.takeAndClear())
        assertNull(store.takeAndClear())
    }

    // SET-BR-018: a second arrival replaces the first and only one is held.
    @Test
    fun `a second arrival while locked replaces the first`() {
        val store = HeldTargetStore()
        store.hold(NavTarget.SelectTab(TabKey.HOME))
        store.hold(NavTarget.SelectTab(TabKey.INSIGHTS))

        assertEquals(NavTarget.SelectTab(TabKey.INSIGHTS), store.takeAndClear())
        assertNull(store.takeAndClear())
    }

    @Test
    fun `nothing held returns null`() {
        val store = HeldTargetStore()
        assertNull(store.takeAndClear())
    }
}
