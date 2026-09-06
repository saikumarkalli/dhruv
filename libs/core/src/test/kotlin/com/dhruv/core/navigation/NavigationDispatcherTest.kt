package com.dhruv.core.navigation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationDispatcherTest {
    @Test
    fun `navigate delivers the target to a collector`() =
        runTest {
            val dispatcher = NavigationDispatcher()
            val target = NavTarget.OpenPlanTool(PlanTool.INVEST)

            val received = mutableListOf<NavTarget>()
            val collectJob =
                launch {
                    received += dispatcher.targets.first()
                }
            // Let the launched coroutine actually reach `.first()`'s subscription point before
            // emitting — a SharedFlow with replay = 0 drops emissions with no active collector yet.
            runCurrent()
            dispatcher.navigate(target)
            collectJob.join()

            assertEquals(listOf(target), received)
        }

    @Test
    fun `navigate does not replay to a collector that starts after it fired`() =
        runTest {
            val dispatcher = NavigationDispatcher()
            dispatcher.navigate(NavTarget.SelectTab(TabKey.PLAN))

            var replayed = false
            val collectJob =
                launch {
                    // No second navigate() call here — if this ever emits, replay leaked history.
                    dispatcher.targets.collect { replayed = true }
                }
            advanceUntilIdle()
            collectJob.cancel()

            assertEquals(false, replayed)
        }
}
