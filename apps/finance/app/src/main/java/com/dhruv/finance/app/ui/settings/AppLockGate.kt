package com.dhruv.finance.app.ui.settings

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.dhruv.core.security.LockState
import com.dhruv.finance.app.R
import com.dhruv.core.ui.components.DhruvWordmarkImage
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.theme.DhruvBrand

private const val ALLOWED_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

/**
 * True when the device can present *some* usable authenticator right now (biometric or device
 * PIN/pattern/password) — checked fresh at both toggle time (FR-022) and gate-resolve time
 * (gate §1 rule 5 / §2 rule 12), never cached. Takes a plain [Context] (application context is
 * fine) — only the actual prompt (`promptBiometric`, below) needs a [FragmentActivity].
 */
fun hasEnrolledCredential(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

/**
 * The effect half of `contracts/app-lock-gate.md` — the decision (`AppLockDecision.kt`) says
 * LOCKED/UNLOCKED; this composable is what a LOCKED decision actually does. While [lockState] is
 * LOCKED, [content] is never composed at all (gate §2 rule 7: "not dimmed, not blurred — absent"),
 * only [LockedSurface] is. [onAuthenticated] is called on a successful `BiometricPrompt` result;
 * the caller (`MainActivity`) is responsible for flipping [lockState] to UNLOCKED in response and
 * for the held-intent dispatch that follows (gate §3).
 */
@Composable
fun AppLockGate(
    activity: FragmentActivity,
    lockState: LockState,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when (lockState) {
        LockState.UNLOCKED -> content()
        LockState.LOCKED -> {
            var attemptToken by remember { mutableStateOf(0) }
            var showRetry by remember { mutableStateOf(false) }

            LaunchedEffect(attemptToken) {
                promptBiometric(
                    activity = activity,
                    onSuccess = onAuthenticated,
                    onFailureOrCancel = { showRetry = true },
                )
            }

            LockedSurface(
                showRetry = showRetry,
                onRetry = {
                    showRetry = false
                    attemptToken++
                },
                modifier = modifier,
            )
        }
    }
}

private fun promptBiometric(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onFailureOrCancel: () -> Unit,
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val callback =
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            // Gate §2 rule 10: no attempt limit and no lockout of our own — every error (including
            // the platform's own lockout codes) just leaves the gate LOCKED with a retry affordance.
            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence,
            ) {
                onFailureOrCancel()
            }

            override fun onAuthenticationFailed() {
                onFailureOrCancel()
            }
        }
    val promptInfo =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.app_lock_prompt_title))
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            // No setNegativeButtonText: mutually exclusive with DEVICE_CREDENTIAL in the allowed
            // authenticators — the system prompt supplies its own "Use PIN" / cancel affordances.
            .build()
    BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
}

/**
 * Gate §2 rule 7: static app chrome and the unlock affordance only — no session-derived data
 * (name, avatar, email) is shown before authentication succeeds.
 */
@Composable
private fun LockedSurface(
    showRetry: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(DhruvBrand.navy)
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DhruvWordmarkImage(height = 40.dp)
        if (showRetry) {
            Text(
                text = stringResource(R.string.app_lock_retry_message),
                color = DhruvBrand.silverLight,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
            )
            NxButton(text = stringResource(R.string.app_lock_retry_action), onClick = onRetry)
        }
    }
}
