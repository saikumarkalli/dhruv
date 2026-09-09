package com.dhruv.finance.data.tracker.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/** FR-015: name a filter combination, re-apply it later. Same plain temp-file-backed
 * `PreferenceDataStoreFactory` pattern as `SessionStoreTest` — deterministic on plain JVM, no
 * Robolectric/Keystore; the AES-GCM encryption itself is `EncryptedDataStoreFactory`'s own,
 * separately-owned responsibility. */
class SavedViewRepositoryTest {
    private lateinit var file: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: SavedViewRepository

    @Before
    fun setUp() {
        file = File.createTempFile("saved_views_test", ".preferences_pb")
        file.deleteOnExit()
        dataStore = PreferenceDataStoreFactory.create(produceFile = { file })
        repo = SavedViewRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        file.delete()
    }

    @Test
    fun `a new repository starts with no saved views`() =
        runTest {
            assertTrue(repo.listSavedViews().isEmpty())
        }

    @Test
    fun `saveView assigns an id and it round-trips through listSavedViews`() =
        runTest {
            val saved = repo.saveView(SavedView(id = "", name = "Big groceries", categoryIds = setOf("cat-1")))

            assertTrue(saved.id.isNotBlank())
            val listed = repo.listSavedViews()
            assertEquals(1, listed.size)
            assertEquals("Big groceries", listed.single().name)
            assertEquals(setOf("cat-1"), listed.single().categoryIds)
        }

    @Test
    fun `saveView with an existing id overwrites that view, not appends a duplicate`() =
        runTest {
            val first = repo.saveView(SavedView(id = "", name = "Rent", minPaise = 1_000_00))
            repo.saveView(first.copy(name = "Rent (renamed)"))

            val listed = repo.listSavedViews()
            assertEquals(1, listed.size)
            assertEquals("Rent (renamed)", listed.single().name)
        }

    @Test
    fun `deleteSavedView removes only that view`() =
        runTest {
            val a = repo.saveView(SavedView(id = "", name = "A"))
            val b = repo.saveView(SavedView(id = "", name = "B"))

            repo.deleteSavedView(a.id)

            val listed = repo.listSavedViews()
            assertEquals(listOf(b.id), listed.map { it.id })
        }

    @Test
    fun `a saved view survives a fresh repository instance reading the same backing store`() =
        runTest {
            repo.saveView(SavedView(id = "", name = "Weekend spend", accountId = "acc-1"))

            val reopened = SavedViewRepositoryImpl(dataStore)

            assertEquals(1, reopened.listSavedViews().size)
            assertEquals("Weekend spend", reopened.listSavedViews().single().name)
        }
}
