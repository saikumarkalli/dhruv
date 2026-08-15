package com.dhruv.finance.data.tracker.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.security.EncryptedDataStoreFactory
import com.dhruv.finance.data.tracker.dto.GoTrueSessionDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Synchronous snapshot of the current tokens — needed by [AuthInterceptor], which runs on a
 * blocking OkHttp interceptor thread and cannot suspend to read a [StateFlow] safely mid-chain. */
data class SessionTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
)

interface SessionStore {
    val state: StateFlow<SessionState>

    suspend fun save(session: GoTrueSessionDto)

    suspend fun clear()

    /** Synchronous snapshot for [AuthInterceptor]; null when signed out. */
    fun currentTokens(): SessionTokens?
}

private object SessionKeys {
    val ACCESS_TOKEN = stringPreferencesKey("access_token")
    val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    val USER_ID = stringPreferencesKey("user_id")
    val EMAIL = stringPreferencesKey("email")
    val EXPIRES_AT = longPreferencesKey("expires_at")
    val DISPLAY_NAME = stringPreferencesKey("display_name")
    val AVATAR_URL = stringPreferencesKey("avatar_url")
}

/**
 * Encrypted-DataStore-backed [SessionStore] (DAT-BR-004). Mirrors SettingsRepositoryImpl's
 * `secureStore` pattern: same [EncryptedDataStoreFactory], eager blocking initial read so
 * [state] has a correct value synchronously on construction. Token values are never written
 * anywhere except this store — no plaintext `SharedPreferences`, no logging.
 *
 * Unlike SettingsRepositoryImpl (many independently-settable keys, each its own lazily-collected
 * StateFlow), this store has exactly two mutation entry points ([save]/[clear]), so [state] and
 * [currentTokens] are updated synchronously and directly from the [Preferences] each of those
 * writes produces — no background collector, no read-after-write race.
 */
class SessionStoreImpl(
    private val secureStore: DataStore<Preferences>,
    private val crashReporter: CrashReporter,
) : SessionStore {
    constructor(context: Context, crashReporter: CrashReporter) : this(
        EncryptedDataStoreFactory.create(context, "tracker_session"),
        crashReporter,
    )

    // Deliberately broad: a corrupt DataStore file or a Keystore-invalidated encrypted blob can
    // fail in several exception shapes, and every one of them must degrade to SignedOut rather
    // than crash construction. Same accepted pattern as AuthRepositoryImpl's equivalent catch
    // block (final whole-branch review, Fix 3 — mirrors ConsentRepositoryImpl's identical case).
    @Suppress("TooGenericExceptionCaught")
    private val initialPrefs: Preferences =
        runBlocking {
            try {
                secureStore.data.first()
            } catch (e: Exception) {
                // Corrupt store / Keystore-invalidated blob must degrade to SignedOut, not crash —
                // but silently is a diagnosability gap, so it's still reported (mirrors
                // SettingsRepositoryImpl's equivalent catch block).
                crashReporter.recordException(e)
                emptyPreferences()
            }
        }

    private val _state = MutableStateFlow(deriveState(initialPrefs))
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    @Volatile
    private var tokens: SessionTokens? = deriveTokens(initialPrefs)

    override suspend fun save(session: GoTrueSessionDto) {
        val updated =
            secureStore.edit { prefs ->
                prefs[SessionKeys.ACCESS_TOKEN] = session.accessToken
                prefs[SessionKeys.REFRESH_TOKEN] = session.refreshToken
                prefs[SessionKeys.USER_ID] = session.user.id
                prefs.setOrRemove(SessionKeys.EMAIL, session.user.email)
                prefs.setOrRemove(SessionKeys.DISPLAY_NAME, session.user.userMetadata?.displayName)
                prefs.setOrRemove(SessionKeys.AVATAR_URL, session.user.userMetadata?.resolvedAvatarUrl)
                prefs[SessionKeys.EXPIRES_AT] = session.expiresAt
            }
        tokens = deriveTokens(updated)
        _state.value = deriveState(updated)
    }

    override suspend fun clear() {
        val updated = secureStore.edit { it.clear() }
        tokens = deriveTokens(updated)
        _state.value = deriveState(updated)
    }

    override fun currentTokens(): SessionTokens? = tokens

    // Deliberately multiple early returns: each missing field means "no tokens", and an early
    // return per required field reads far more clearly here than threading a nullable accumulator
    // through three checks. Same accepted pattern as AuthInterceptor.intercept's equivalent shape
    // (final whole-branch review, Fix 3).
    @Suppress("ReturnCount")
    private fun deriveTokens(prefs: Preferences): SessionTokens? {
        val access = prefs[SessionKeys.ACCESS_TOKEN] ?: return null
        val refresh = prefs[SessionKeys.REFRESH_TOKEN] ?: return null
        val expiresAt = prefs[SessionKeys.EXPIRES_AT] ?: return null
        return SessionTokens(access, refresh, expiresAt)
    }

    // Deliberately multiple early returns — same reasoning as deriveTokens above: each missing
    // field is an unambiguous SignedOut, checked as early as possible rather than nested.
    @Suppress("ReturnCount")
    private fun deriveState(prefs: Preferences): SessionState {
        val userId = prefs[SessionKeys.USER_ID] ?: return SessionState.SignedOut
        val expiresAt = prefs[SessionKeys.EXPIRES_AT] ?: return SessionState.SignedOut
        val email = prefs[SessionKeys.EMAIL]
        val nowSeconds = System.currentTimeMillis() / 1000
        return if (expiresAt > nowSeconds) {
            SessionState.Active(userId, email, prefs[SessionKeys.DISPLAY_NAME], prefs[SessionKeys.AVATAR_URL])
        } else {
            SessionState.Expired
        }
    }
}

/** `prefs[key] = value` when non-null, `prefs.remove(key)` when null — the same
 * set-or-remove-for-a-nullable-field shape [SessionStoreImpl.save] needs for email/displayName/
 * avatarUrl (all optional, all absent on the earliest Google accounts or a future non-Google
 * provider). */
private fun MutablePreferences.setOrRemove(
    key: Preferences.Key<String>,
    value: String?,
) {
    if (value != null) this[key] = value else remove(key)
}
