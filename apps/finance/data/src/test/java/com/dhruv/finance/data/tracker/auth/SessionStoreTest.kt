package com.dhruv.finance.data.tracker.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.finance.data.tracker.dto.GoTrueSessionDto
import com.dhruv.finance.data.tracker.dto.GoTrueUserDto
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * DAT-BR-004: a session token is issued -> stored -> written only to the (injected) encrypted-
 * shaped DataStore, round-trips correctly, and survives a fresh [SessionStoreImpl] instance
 * reading the same backing store (simulating process restart). The AES-GCM encryption itself is
 * [com.dhruv.core.security.EncryptedDataStoreFactory]'s existing, separately-owned responsibility
 * (already relied on unencrypted-free by SettingsRepositoryImpl for the Gemini key) — this test
 * exercises SessionStoreImpl's own read/write/derive logic against a plain, temp-file-backed
 * Preferences DataStore so it runs deterministically on plain JVM (no Robolectric/Keystore).
 */
class SessionStoreTest {
    private lateinit var file: File
    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        file = File.createTempFile("tracker_session_test", ".preferences_pb")
        file.deleteOnExit()
        dataStore = PreferenceDataStoreFactory.create(produceFile = { file })
    }

    @After
    fun tearDown() {
        file.delete()
    }

    private fun session(
        accessToken: String = "access-1",
        refreshToken: String = "refresh-1",
        expiresAt: Long = (System.currentTimeMillis() / 1000) + 3600,
        userId: String = "user-1",
        email: String? = "user@example.com",
    ) = GoTrueSessionDto(
        accessToken = accessToken,
        tokenType = "bearer",
        expiresIn = 3600,
        expiresAt = expiresAt,
        refreshToken = refreshToken,
        user = GoTrueUserDto(id = userId, email = email),
    )

    // DAT-BR-004
    @Test
    fun `save persists tokens and exposes Active state`() =
        runTest {
            val store = SessionStoreImpl(dataStore, NoOpCrashReporter)
            assertEquals(SessionState.SignedOut, store.state.value)

            store.save(session())

            assertEquals(SessionState.Active("user-1", "user@example.com"), store.state.value)
            assertEquals("access-1", store.currentTokens()?.accessToken)
            assertEquals("refresh-1", store.currentTokens()?.refreshToken)
        }

    // DAT-BR-004
    @Test
    fun `clear resets state to SignedOut and drops tokens`() =
        runTest {
            val store = SessionStoreImpl(dataStore, NoOpCrashReporter)
            store.save(session())

            store.clear()

            assertEquals(SessionState.SignedOut, store.state.value)
            assertNull(store.currentTokens())
        }

    // DAT-BR-004 — round-trip across a fresh instance reading the same backing store
    @Test
    fun `a new instance over the same store observes the persisted session`() =
        runTest {
            val first = SessionStoreImpl(dataStore, NoOpCrashReporter)
            first.save(session(accessToken = "access-2", userId = "user-2", email = null))

            val second = SessionStoreImpl(dataStore, NoOpCrashReporter)

            assertEquals(SessionState.Active("user-2", null), second.state.value)
            assertEquals("access-2", second.currentTokens()?.accessToken)
        }

    // DAT-BR-004 — an expired expires_at yields Expired, not Active
    @Test
    fun `a session whose expires_at has passed is Expired`() =
        runTest {
            val store = SessionStoreImpl(dataStore, NoOpCrashReporter)

            store.save(session(expiresAt = (System.currentTimeMillis() / 1000) - 60))

            assertEquals(SessionState.Expired, store.state.value)
            // Tokens remain available so AuthInterceptor can still attempt a refresh.
            assertEquals("access-1", store.currentTokens()?.accessToken)
        }

    // DAT-BR-004 — no plaintext SharedPreferences write: SessionStoreImpl never touches the
    // legacy SharedPreferences API at all (verified by code review of SessionStoreImpl, which
    // only ever calls into the injected androidx.datastore.core.DataStore<Preferences>).
    @Test
    fun `save does not throw and the backing file actually changes on disk`() =
        runTest {
            val before = if (file.exists()) file.readBytes() else ByteArray(0)
            val store = SessionStoreImpl(dataStore, NoOpCrashReporter)

            store.save(session())

            val after = file.readBytes()
            assertTrue("expected the backing DataStore file to change after save()", !after.contentEquals(before))
        }
}
