package com.dhruv.finance.data.tracker.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.dhruv.core.observability.NoOpCrashReporter
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * ONB-BR-004: any A3 switch is toggled -> app is force-killed and reopened -> the switch's value
 * is unchanged (persisted, not in-memory).
 * ONB-BR-005: a consent switch is ON -> user turns it OFF -> value persists OFF; other switches
 * are unaffected.
 * Plain (unencrypted) DataStore — booleans aren't secrets (brief, §5 auth/ConsentRepository).
 */
class ConsentRepositoryTest {
    private lateinit var file: File
    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        file = File.createTempFile("tracker_consent_test", ".preferences_pb")
        file.deleteOnExit()
        dataStore = PreferenceDataStoreFactory.create(produceFile = { file })
    }

    @After
    fun tearDown() {
        file.delete()
    }

    // ONB-BR-004
    @Test
    fun `defaults are all false before any consent is granted`() =
        runTest {
            val repo = ConsentRepositoryImpl(dataStore, NoOpCrashReporter)
            assertEquals(ConsentState(), repo.state.value)
        }

    // ONB-BR-005 — each switch persists independently, siblings unaffected
    @Test
    fun `setting one switch does not affect the other two`() =
        runTest {
            val repo = ConsentRepositoryImpl(dataStore, NoOpCrashReporter)

            repo.setSyncFinancialRecords(true)

            assertEquals(
                ConsentState(syncFinancialRecords = true, readTransactionSms = false, askDhruvAboutMoney = false),
                repo.state.value,
            )
        }

    // ONB-BR-005 — turning a switch back OFF persists OFF
    @Test
    fun `turning a switch off persists off`() =
        runTest {
            val repo = ConsentRepositoryImpl(dataStore, NoOpCrashReporter)
            repo.setReadTransactionSms(true)

            repo.setReadTransactionSms(false)

            assertEquals(false, repo.state.value.readTransactionSms)
        }

    // ONB-BR-004 — round-trip across a fresh instance over the same backing store (simulated restart)
    @Test
    fun `a new instance over the same store observes persisted switches`() =
        runTest {
            val first = ConsentRepositoryImpl(dataStore, NoOpCrashReporter)
            first.setSyncFinancialRecords(true)
            first.setAskDhruvAboutMoney(true)

            val second = ConsentRepositoryImpl(dataStore, NoOpCrashReporter)

            assertEquals(
                ConsentState(syncFinancialRecords = true, readTransactionSms = false, askDhruvAboutMoney = true),
                second.state.value,
            )
        }

    // Not a catalogued ONB/DAT row — no dedicated catalog entry exists for this specific flag
    // (Task 3 decision 1). Mirrors the existing switch-persistence tests above: defaults false,
    // set persists true, round-trips across a fresh instance over the same backing store.
    @Test
    fun `hasCompletedOnboarding defaults false and persists across a fresh instance`() =
        runTest {
            val first = ConsentRepositoryImpl(dataStore, NoOpCrashReporter)
            assertEquals(false, first.state.value.hasCompletedOnboarding)

            first.setHasCompletedOnboarding(true)
            assertEquals(true, first.state.value.hasCompletedOnboarding)

            val second = ConsentRepositoryImpl(dataStore, NoOpCrashReporter)
            assertEquals(true, second.state.value.hasCompletedOnboarding)
        }
}
