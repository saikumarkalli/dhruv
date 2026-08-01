package com.dhruv.core.observability

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureViewModelTest {

    private val recorder = RecordingCrashReporter()

    private class TestViewModel(reporter: CrashReporter) :
        FeatureViewModel(reporter, "test_module") {
        fun doReportError(t: Throwable) = reportFeatureError(t)
        fun getHandler() = exceptionHandler
    }

    private class RecordingCrashReporter : CrashReporter {
        var lastModule: String? = null
        var lastException: Throwable? = null
        var lastLog: String? = null

        override fun setModule(name: String) { lastModule = name }
        override fun recordException(t: Throwable) { lastException = t }
        override fun log(message: String) { lastLog = message }
    }

    @Test
    fun `init sets module on crashReporter`() {
        TestViewModel(recorder)
        assertEquals("test_module", recorder.lastModule)
    }

    @Test
    fun `featureError starts null`() {
        val vm = TestViewModel(recorder)
        assertNull(vm.featureError.value)
    }

    @Test
    fun `reportFeatureError records and publishes`() {
        val vm = TestViewModel(recorder)
        val error = RuntimeException("boom")
        vm.doReportError(error)
        assertEquals(error, recorder.lastException)
        assertEquals(error, vm.featureError.value)
    }

    @Test
    fun `exceptionHandler records and publishes`() = runTest {
        val vm = TestViewModel(recorder)
        val error = RuntimeException("handler boom")
        vm.getHandler().handleException(
            coroutineContext,
            error,
        )
        assertEquals(error, recorder.lastException)
        assertEquals(error, vm.featureError.value)
    }
}
