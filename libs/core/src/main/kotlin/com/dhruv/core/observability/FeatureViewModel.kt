package com.dhruv.core.observability

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class FeatureViewModel(
    protected val crashReporter: CrashReporter,
    moduleKey: String,
) : ViewModel() {
    private val _featureError = MutableStateFlow<Throwable?>(null)
    val featureError: StateFlow<Throwable?> = _featureError.asStateFlow()

    protected val exceptionHandler =
        CoroutineExceptionHandler { _, throwable -> reportFeatureError(throwable) }

    init {
        crashReporter.setModule(moduleKey)
    }

    protected fun reportFeatureError(throwable: Throwable) {
        crashReporter.recordException(throwable)
        _featureError.value = throwable
    }
}
