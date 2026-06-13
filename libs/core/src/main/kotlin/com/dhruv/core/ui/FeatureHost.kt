package com.dhruv.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dhruv.core.observability.CrashReporter

/**
 * Top-level wrapper for every feature route.
 *
 * Error state is caller-owned: the feature ViewModel catches coroutine exceptions via
 * CoroutineExceptionHandler and exposes featureError: StateFlow<Throwable?>. The screen
 * collects that state and passes it here.
 *
 * Usage:
 *   val featureError by viewModel.featureError.collectAsStateWithLifecycle()
 *   FeatureHost(
 *       featureKey = "calculator",
 *       isEnabled = featureEnabled,
 *       featureError = featureError,
 *       crashReporter = crashReporter,
 *   ) { CalculatorContent(...) }
 */
@Composable
fun FeatureHost(
    featureKey: String,
    isEnabled: Boolean,
    featureError: Throwable?,
    crashReporter: CrashReporter,
    content: @Composable () -> Unit
) {
    when {
        !isEnabled -> FeatureDisabledCard(featureKey)
        featureError != null -> {
            LaunchedEffect(featureError) {
                crashReporter.setModule(featureKey)
                crashReporter.recordException(featureError)
            }
            FeatureErrorCard(error = featureError, featureKey = featureKey)
        }
        else -> content()
    }
}

@Composable
fun FeatureErrorCard(error: Throwable, featureKey: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error.message ?: "Unknown error in $featureKey",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FeatureDisabledCard(featureKey: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Feature unavailable",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$featureKey is not available right now.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
