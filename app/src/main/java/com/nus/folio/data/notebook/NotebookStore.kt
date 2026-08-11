package com.nus.folio.data.notebook

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nus.folio.domain.model.Notebook
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.notebookDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "folio_notebooks",
)

class NotebookStore(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.applicationContext.notebookDataStore)

    private val mutex = Mutex()

    suspend fun read(spaceId: String): Notebook = mutex.withLock {
        val prefs = dataStore.data.first()
        Notebook(
            spaceId = spaceId,
            content = prefs[stringPreferencesKey(contentKey(spaceId))].orEmpty(),
            updatedAtMillis = prefs[longPreferencesKey(updatedAtKey(spaceId))] ?: 0L,
        )
    }

    suspend fun write(spaceId: String, content: String): Notebook = mutex.withLock {
        val updatedAtMillis = System.currentTimeMillis()
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey(contentKey(spaceId))] = content
            prefs[longPreferencesKey(updatedAtKey(spaceId))] = updatedAtMillis
        }
        Notebook(
            spaceId = spaceId,
            content = content,
            updatedAtMillis = updatedAtMillis,
        )
    }

    private fun contentKey(spaceId: String): String = "notebook_content_$spaceId"

    private fun updatedAtKey(spaceId: String): String = "notebook_updated_at_$spaceId"
}
