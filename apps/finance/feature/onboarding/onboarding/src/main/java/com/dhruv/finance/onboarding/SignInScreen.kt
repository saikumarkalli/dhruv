package com.dhruv.finance.onboarding

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.DhruvTheme
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * A2 — sign-in (functional spec §5 Group A). Bare, full-frame, no chrome (registry §1). Google is
 * the only sign-in method ("one account, every device"); consent (A3) is asked separately, never
 * bundled in here (ONB-BR-001 — [OnboardingViewModel.onGoogleIdTokenReceived] touches only
 * [com.dhruv.finance.data.tracker.auth.AuthRepository], never the consent repository).
 *
 * The Credential Manager call is an Android-framework concern that can't be exercised from a
 * ViewModel unit test (see [OnboardingViewModel]'s own doc comment), so it lives entirely in this
 * composable: launch the request, extract the raw Google ID token, hand it to
 * [OnboardingViewModel.onGoogleIdTokenReceived]. A failure at either step — the Credential Manager
 * call itself, or the backend sign-in [onGoogleIdTokenReceived] performs afterward (network blip,
 * bad config) — is a normal, retryable UX outcome here, not a feature crash, so both surface as the
 * same inline retry copy rather than routing through [OnboardingViewModel]'s feature-error state
 * (final whole-branch review — a backend failure used to permanently replace this whole screen with
 * a dead, no-retry error card via `FeatureHost`).
 *
 * `onGoogleIdTokenReceived`'s second parameter is the RAW nonce — see [generateRawNonce] and
 * [sha256Hex] below for why a nonce is generated here and hashed before reaching Credential
 * Manager, while the unhashed value goes to the backend call.
 */
@Composable
fun SignInScreen(
    onGoogleIdTokenReceived: suspend (idToken: String, rawNonce: String) -> Result<Unit>,
    onUseOfflineSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val googleSignInConfig: GoogleSignInConfig = koinInject()

    var isSigningIn by remember { mutableStateOf(false) }
    var signInErrorMessage by remember { mutableStateOf<String?>(null) }

    // Same feedback shape as Settings > Privacy's delete actions (SettingsScreen.kt) — a Toast for
    // the immediate ping plus the persistent inline text below, since a Toast alone would vanish
    // before an offline message that actually needs the user to go do something about it.
    fun showSignInError(message: String) {
        signInErrorMessage = message
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier.fillMaxSize().padding(DhruvNextSpacing.screenGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Same header, same position, as ConsentScreen (A3) and EmptyStartScreen (A4) — found
        // missing live: this screen previously had its own oversized "hero" wordmark instead,
        // which read as inconsistent against A3/A4 having none at all.
        OnboardingHeader()

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = OnboardingConfig.SIGN_IN_TAGLINE,
                color = colors.tx2,
                fontSize = DhruvNextType.body,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }

        if (signInErrorMessage != null) {
            Text(
                text = signInErrorMessage.orEmpty(),
                modifier = Modifier.padding(bottom = DhruvNextSpacing.interCardGap),
                color = colors.neg,
                fontSize = DhruvNextType.meta,
                textAlign = TextAlign.Center,
            )
        }

        NxButton(
            text = OnboardingConfig.SIGN_IN_GOOGLE_CTA,
            enabled = !isSigningIn,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                signInErrorMessage = null
                if (!isDeviceOnline(context)) {
                    // Checked proactively, before ever touching Credential Manager — a device can
                    // show full signal bars with its radio connected but DNS/actual internet
                    // reachability broken (found live: NET_CAPABILITY_VALIDATED is exactly the flag
                    // that catches this, where NET_CAPABILITY_INTERNET alone would not). Skipping the
                    // Credential Manager call entirely also avoids flashing a native picker that's
                    // guaranteed to fail.
                    showSignInError(OnboardingConfig.SIGN_IN_ERROR_OFFLINE)
                } else {
                    isSigningIn = true
                    coroutineScope.launch {
                        // Generated fresh per attempt, never reused. The RAW value goes to GoTrue
                        // (which hashes it itself to compare against the token's nonce claim); only
                        // the SHA-256 hash goes to Google via setNonce — Google puts the hash it's
                        // given verbatim into the id_token, it does not hash it again.
                        val rawNonce = generateRawNonce()
                        runCatching {
                            val googleIdOption =
                                GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(googleSignInConfig.webClientId)
                                    .setNonce(sha256Hex(rawNonce))
                                    .build()
                            val request =
                                GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()
                            CredentialManager.create(context).getCredential(context, request)
                        }.onSuccess { response ->
                            val credential = response.credential
                            if (credential is CustomCredential &&
                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                            ) {
                                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                                // Await the backend sign-in too, not just the on-device credential
                                // step — a failure here (network, bad SUPABASE_URL, or a nonce
                                // mismatch) is just as retryable and must not be silently dropped
                                // nor escalate to a feature crash.
                                val signInResult = onGoogleIdTokenReceived(idToken, rawNonce)
                                isSigningIn = false
                                signInResult.onFailure {
                                    showSignInError(
                                        if (!isDeviceOnline(context)) {
                                            // Connectivity can drop between the credential step
                                            // succeeding and the backend call running.
                                            OnboardingConfig.SIGN_IN_ERROR_OFFLINE
                                        } else {
                                            OnboardingConfig.SIGN_IN_ERROR_BACKEND
                                        },
                                    )
                                }
                            } else {
                                isSigningIn = false
                                showSignInError(OnboardingConfig.SIGN_IN_ERROR_WRONG_ACCOUNT)
                            }
                        }.onFailure { error ->
                            isSigningIn = false
                            // GetCredentialException covers cancellation, "no accounts", transient
                            // provider errors — every case is retryable, none is a feature crash.
                            // The pre-flight check above already catches the common offline case;
                            // this only still fires for a genuine mid-flow cancellation/provider
                            // error, or connectivity dropping in the few hundred ms since the check.
                            showSignInError(
                                when {
                                    !isDeviceOnline(context) -> OnboardingConfig.SIGN_IN_ERROR_OFFLINE
                                    error is GetCredentialException -> OnboardingConfig.SIGN_IN_ERROR_CANCELLED
                                    else -> OnboardingConfig.SIGN_IN_ERROR_GENERIC
                                },
                            )
                        }
                    }
                }
            },
        )

        TextButton(
            onClick = onUseOfflineSelected,
            modifier = Modifier.padding(top = DhruvNextSpacing.interCardGap),
        ) {
            Text(
                text = OnboardingConfig.SIGN_IN_OFFLINE_CTA,
                color = colors.tx2,
                fontSize = DhruvNextType.body,
            )
        }

        Row(
            modifier = Modifier.padding(top = DhruvNextSpacing.sectionGap),
            horizontalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap),
        ) {
            Text(text = OnboardingConfig.SIGN_IN_TERMS_LABEL, color = colors.tx3, fontSize = DhruvNextType.meta)
            Text(text = OnboardingConfig.SIGN_IN_PRIVACY_LABEL, color = colors.tx3, fontSize = DhruvNextType.meta)
        }
    }
}

/**
 * True only when the active network is both present and Android-validated as actually reaching
 * the internet (`NET_CAPABILITY_VALIDATED`) — not just radio-connected. A device can show full
 * signal with a broken DNS resolver (observed live testing this screen); `NET_CAPABILITY_INTERNET`
 * alone would still read true in that state, `NET_CAPABILITY_VALIDATED` correctly reads false.
 * Requires `ACCESS_NETWORK_STATE` (normal permission, declared in the app manifest, no runtime
 * prompt).
 */
@Suppress("ReturnCount")
private fun isDeviceOnline(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

/** 32 random bytes, hex-encoded — the raw (unhashed) nonce GoTrue expects to receive verbatim. */
private fun generateRawNonce(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return bytes.joinToString(separator = "") { "%02x".format(it) }
}

/** Google's `GetGoogleIdOption.setNonce` expects the SHA-256 hex digest of the raw nonce, not the
 * raw value itself — Google embeds whatever it's given verbatim into the id_token's nonce claim,
 * so the hash has to happen on this side before the value ever reaches Credential Manager. */
private fun sha256Hex(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return digest.joinToString(separator = "") { "%02x".format(it) }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SignInScreenPreview() {
    DhruvTheme {
        SignInScreen(onGoogleIdTokenReceived = { _, _ -> Result.success(Unit) }, onUseOfflineSelected = {})
    }
}
