package com.dhruv.finance.data.tracker.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dhruv.core.security.EncryptedDataStoreFactory
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * A named ledger filter combination a user can re-apply later (FR-015, data-model.md "Saved
 * view"). Deliberately layer-neutral (no dependency on `:apps:finance:feature:money`'s
 * `LedgerFilter` — `feature -> data` is the only allowed direction, Article III): plain
 * primitives that the feature module's ViewModel maps to/from its own `LedgerFilter`.
 */
@JsonClass(generateAdapter = true)
data class SavedView(
    val id: String,
    val name: String,
    val typeName: String? = null,
    val categoryIds: Set<String> = emptySet(),
    val minPaise: Long? = null,
    val maxPaise: Long? = null,
    val accountId: String? = null,
)

interface SavedViewRepository {
    suspend fun listSavedViews(): List<SavedView>

    suspend fun saveView(view: SavedView): SavedView

    suspend fun deleteSavedView(id: String)
}

private val SAVED_VIEWS_KEY = stringPreferencesKey("saved_views_json")

/** Encrypted-DataStore-backed (FR-015 — "persisted in the existing encrypted settings DataStore,
 * not a new table"), same construction pattern as `SessionStoreImpl`/`ConsentRepositoryImpl`. */
@OptIn(ExperimentalStdlibApi::class)
class SavedViewRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SavedViewRepository {
    constructor(context: Context) : this(EncryptedDataStoreFactory.create(context.applicationContext, "money_saved_views"))

    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter<List<SavedView>>()

    override suspend fun listSavedViews(): List<SavedView> {
        val json = dataStore.data.first()[SAVED_VIEWS_KEY]
        return json?.let { adapter.fromJson(it) } ?: emptyList()
    }

    override suspend fun saveView(view: SavedView): SavedView {
        val toSave = if (view.id.isBlank()) view.copy(id = UUID.randomUUID().toString()) else view
        val current = listSavedViews().filterNot { it.id == toSave.id }
        val updated = current + toSave
        dataStore.edit { prefs -> prefs[SAVED_VIEWS_KEY] = adapter.toJson(updated) }
        return toSave
    }

    override suspend fun deleteSavedView(id: String) {
        val updated = listSavedViews().filterNot { it.id == id }
        dataStore.edit { prefs -> prefs[SAVED_VIEWS_KEY] = adapter.toJson(updated) }
    }
}
