package com.dhruv.finance.app.ui.settings

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dhruv.core.security.SignInNonce
import com.dhruv.core.ui.components.InitialsTile
import com.dhruv.core.ui.components.ListGroup
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxButtonVariant
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.components.SwitchRow
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.app.BuildConfig
import com.dhruv.finance.app.R
import com.dhruv.finance.data.tracker.auth.ConsentRepository
import com.dhruv.finance.data.tracker.auth.ConsentState
import com.dhruv.finance.data.tracker.auth.SessionState
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Account tier's real screen (0b.2, US2, T046) — sign-in, sign-out, the three consent switches,
 * and erasure. Replaces [SettingsAccountBody] (0b.1's zero-regression stand-in, which had no
 * sign-in/sign-out at all — the two defects this sub-phase exists to fix).
 *
 * Sign-in is wired directly to [AccountSettingsViewModel] → `AuthRepository`, never to
 * `com.dhruv.finance.onboarding` (research R6, `SET-ARCH-003`) — the Credential Manager call is
 * therefore duplicated here rather than shared with `SignInScreen`, same shape, same reasoning
 * [SettingsAccountBody]'s own doc comment already gave for the consent copy.
 *
 * Screen states (T056, FR-044): signed-out (identity card + Sign in), offline (checked before the
 * Credential Manager call, same as onboarding's `SignInScreen`), error (inline text under Sign in;
 * a Toast for erasure failures — same shape [SettingsAccountBody] already used), loading (Sign in
 * disabled + label swap while a sign-in attempt is in flight).
 */
@Composable
fun AccountSettingsScreen(
    viewModel: AccountSettingsViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val consentState by viewModel.consentState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = LocalDhruvNextColors.current

    var isSigningIn by remember { mutableStateOf(false) }
    var signInErrorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteMyDataDialog by remember { mutableStateOf(false) }
    var showDeleteMyAccountDialog by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        AccountIdentityCard(sessionState = sessionState)

        when (sessionState) {
            is SessionState.Active -> {
                NxButton(
                    text = stringResource(R.string.settings_account_sign_out),
                    variant = NxButtonVariant.Outline,
                    onClick = { coroutineScope.launch { viewModel.signOut() } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SessionState.SignedOut, SessionState.Expired -> {
                Column {
                    if (signInErrorMessage != null) {
                        Text(
                            text = signInErrorMessage.orEmpty(),
                            color = colors.neg,
                            fontSize = DhruvNextType.meta,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    NxButton(
                        text =
                            if (isSigningIn) {
                                stringResource(R.string.settings_account_signing_in)
                            } else {
                                stringResource(R.string.settings_account_sign_in)
                            },
                        enabled = !isSigningIn,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            signInErrorMessage = null
                            if (!isDeviceOnline(context)) {
                                signInErrorMessage = context.getString(R.string.settings_account_sign_in_offline)
                            } else {
                                isSigningIn = true
                                coroutineScope.launch {
                                    performGoogleSignIn(
                                        context = context,
                                        onGoogleIdTokenReceived = viewModel::onGoogleIdTokenReceived,
                                        onError = { signInErrorMessage = it },
                                    )
                                    isSigningIn = false
                                }
                            }
                        },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(text = stringResource(R.string.settings_privacy_data_section), modifier = Modifier.padding(start = 4.dp))
            ListGroup(
                rows =
                    buildList<@Composable () -> Unit> {
                        localConsentRows().forEach { consentRow ->
                            add {
                                SwitchRow(
                                    label = stringResource(consentRow.labelRes),
                                    description = stringResource(consentRow.scopeRes),
                                    checked = consentRow.isChecked(consentState),
                                    onCheckedChange = { enabled ->
                                        coroutineScope.launch { consentRow.setChecked(viewModel, enabled) }
                                    },
                                )
                            }
                        }
                        // "Export my data" removed outright (T053, research R7) — no financial
                        // records repository exists yet to export from (FR-018 forbids an empty
                        // export). Reinstated once Phase 2's accounts/holdings repository lands.

                        // Spec Edge Cases: signed out, the erasure rows "show their signed-out
                        // state rather than acting on a session that is not there". Both RPCs are
                        // `auth.uid()`-scoped, so invoking them without a session can only produce
                        // a failure Toast that reads like a server problem — a worse answer than
                        // saying there is nothing signed in to erase. `onClick = null` renders
                        // DangerRow greyed and non-interactive (its own documented contract).
                        val signedIn = sessionState is SessionState.Active
                        add {
                            DangerRow(
                                title = stringResource(R.string.settings_delete_my_data_title),
                                subtitle =
                                    if (signedIn) {
                                        stringResource(R.string.settings_delete_my_data_subtitle)
                                    } else {
                                        stringResource(R.string.settings_erasure_signed_out)
                                    },
                                onClick = if (signedIn) ({ showDeleteMyDataDialog = true }) else null,
                            )
                        }
                        add {
                            DangerRow(
                                title = stringResource(R.string.settings_delete_my_account_title),
                                subtitle =
                                    if (signedIn) {
                                        stringResource(R.string.settings_delete_my_account_subtitle)
                                    } else {
                                        stringResource(R.string.settings_erasure_signed_out)
                                    },
                                onClick = if (signedIn) ({ showDeleteMyAccountDialog = true }) else null,
                            )
                        }
                    },
            )
        }
    }

    if (showDeleteMyDataDialog) {
        val successMessage = stringResource(R.string.settings_delete_my_data_success)
        val failureMessage = stringResource(R.string.settings_delete_my_data_failure)
        DeleteMyDataDialog(
            onConfirm = {
                showDeleteMyDataDialog = false
                coroutineScope.launch {
                    viewModel
                        .deleteMyData()
                        .onSuccess { Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show() }
                        .onFailure { Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show() }
                }
            },
            onDismiss = { showDeleteMyDataDialog = false },
        )
    }

    if (showDeleteMyAccountDialog) {
        val successMessage = stringResource(R.string.settings_delete_my_account_success)
        val failureMessage = stringResource(R.string.settings_delete_my_account_failure)
        DeleteMyAccountDialog(
            onConfirm = {
                showDeleteMyAccountDialog = false
                coroutineScope.launch {
                    viewModel
                        .deleteMyAccount()
                        .onSuccess { Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show() }
                        .onFailure { Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show() }
                }
            },
            onDismiss = { showDeleteMyAccountDialog = false },
        )
    }
}

@Composable
private fun AccountIdentityCard(sessionState: SessionState) {
    val colors = LocalDhruvNextColors.current
    NxCard(modifier = Modifier.fillMaxWidth()) {
        val active = sessionState as? SessionState.Active
        val localDeviceLabel = stringResource(R.string.settings_account_local_device)
        Row(verticalAlignment = Alignment.CenterVertically) {
            val avatarUrl = active?.avatarUrl
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(DhruvNextRadii.innerTile)),
                )
            } else {
                InitialsTile(name = active?.displayName ?: active?.email ?: localDeviceLabel, size = 34.dp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = active?.displayName ?: active?.email ?: localDeviceLabel,
                    color = colors.tx,
                    fontWeight = FontWeight.Bold,
                    fontSize = DhruvNextType.title,
                )
                Text(
                    text =
                        when {
                            active == null -> stringResource(R.string.settings_account_local_device_subtitle)
                            active.displayName != null && active.email != null -> active.email.orEmpty()
                            else -> stringResource(R.string.settings_account_signed_in)
                        },
                    color = colors.tx3,
                    fontSize = DhruvNextType.meta,
                )
            }
        }
    }
}

private data class LocalConsentRow(
    val labelRes: Int,
    val scopeRes: Int,
    val isChecked: (ConsentState) -> Boolean,
    val setChecked: suspend (AccountSettingsViewModel, Boolean) -> Unit,
)

/** Shell-owned copy of the three A3 consent switches (ONB-BR-004/005) — same labels/scope text and
 * the same [ConsentRepository] setters (via [AccountSettingsViewModel]'s passthroughs)
 * ConsentScreen's A3 step uses, held locally rather than imported from
 * `:apps:finance:feature:onboarding` (`SET-ARCH-003`). */
private fun localConsentRows(): List<LocalConsentRow> =
    listOf(
        LocalConsentRow(
            R.string.settings_consent_sync_label,
            R.string.settings_consent_sync_scope,
            { it.syncFinancialRecords },
            { vm, enabled -> vm.setSyncFinancialRecords(enabled) },
        ),
        LocalConsentRow(
            R.string.settings_consent_sms_label,
            R.string.settings_consent_sms_scope,
            { it.readTransactionSms },
            { vm, enabled -> vm.setReadTransactionSms(enabled) },
        ),
        LocalConsentRow(
            R.string.settings_consent_ask_dhruv_label,
            R.string.settings_consent_ask_dhruv_scope,
            { it.askDhruvAboutMoney },
            { vm, enabled -> vm.setAskDhruvAboutMoney(enabled) },
        ),
    )

/** The Credential Manager call — same shape as `SignInScreen`'s, duplicated per research R6 /
 * `SET-ARCH-003` rather than shared with the onboarding module. */
private suspend fun performGoogleSignIn(
    context: Context,
    onGoogleIdTokenReceived: suspend (idToken: String, rawNonce: String) -> Result<Unit>,
    onError: (String) -> Unit,
) {
    val nonce = SignInNonce.generate()
    runCatching {
        val googleIdOption =
            GetGoogleIdOption
                .Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setNonce(nonce.sha256Hex)
                .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
        CredentialManager.create(context).getCredential(context, request)
    }.onSuccess { response ->
        val credential = response.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            onGoogleIdTokenReceived(idToken, nonce.raw).onFailure {
                onError(
                    if (!isDeviceOnline(context)) {
                        context.getString(R.string.settings_account_sign_in_offline)
                    } else {
                        context.getString(R.string.settings_account_sign_in_error_backend)
                    },
                )
            }
        } else {
            onError(context.getString(R.string.settings_account_sign_in_error_wrong_account))
        }
    }.onFailure { error ->
        onError(
            when {
                !isDeviceOnline(context) -> context.getString(R.string.settings_account_sign_in_offline)
                error is GetCredentialException -> context.getString(R.string.settings_account_sign_in_error_cancelled)
                else -> context.getString(R.string.settings_account_sign_in_error_generic)
            },
        )
    }
}

/**
 * True only when the active network is both present and Android-validated as actually reaching the
 * internet (`NET_CAPABILITY_VALIDATED`) — not merely radio-connected. Kept local rather than
 * shared: it is a two-line platform query, and unlike the nonce pair (`SignInNonce`, `:libs:core`)
 * nothing about it is security-sensitive or correctness-coupled across call sites.
 */
private fun isDeviceOnline(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
