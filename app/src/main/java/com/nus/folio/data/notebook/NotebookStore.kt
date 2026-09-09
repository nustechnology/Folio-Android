package com.nus.folio.data.notebook

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
        notebookFrom(dataStore.data.first(), spaceId)
    }

    suspend fun isDirty(spaceId: String): Boolean = mutex.withLock {
        dataStore.data.first()[booleanPreferencesKey(dirtyKey(spaceId))] == true
    }

    suspend fun write(
        spaceId: String,
        content: String,
        dirty: Boolean = false,
        readOnly: Boolean = false,
    ): Notebook = mutex.withLock {
        writeLocked(spaceId, content, dirty, readOnly)
    }

    /**
     * Cache a remote snapshot only if there is no unsynced local edit.
     * Returns the local notebook unchanged when [spaceId] is dirty.
     */
    suspend fun writeIfNotDirty(
        spaceId: String,
        content: String,
        readOnly: Boolean = false,
    ): Notebook = mutex.withLock {
        val prefs = dataStore.data.first()
        if (prefs[booleanPreferencesKey(dirtyKey(spaceId))] == true) {
            notebookFrom(prefs, spaceId)
        } else {
            writeLocked(spaceId, content, dirty = false, readOnly = readOnly)
        }
    }

    /**
     * Clears dirty only when [revision] is still the latest local write for [spaceId].
     * An older PUT completing after a newer draft was written must not mark that draft clean.
     */
    suspend fun markClean(spaceId: String, revision: Long) {
        mutex.withLock {
            dataStore.edit { prefs ->
                val current = prefs[longPreferencesKey(revisionKey(spaceId))] ?: 0L
                if (current == revision) {
                    prefs[booleanPreferencesKey(dirtyKey(spaceId))] = false
                }
            }
        }
    }

    private suspend fun writeLocked(
        spaceId: String,
        content: String,
        dirty: Boolean,
        readOnly: Boolean,
    ): Notebook {
        val updatedAtMillis = System.currentTimeMillis()
        var revision = 0L
        dataStore.edit { prefs ->
            revision = (prefs[longPreferencesKey(revisionKey(spaceId))] ?: 0L) + 1L
            prefs[stringPreferencesKey(contentKey(spaceId))] = content
            prefs[longPreferencesKey(updatedAtKey(spaceId))] = updatedAtMillis
            prefs[longPreferencesKey(revisionKey(spaceId))] = revision
            prefs[booleanPreferencesKey(dirtyKey(spaceId))] = dirty
            prefs[booleanPreferencesKey(readOnlyKey(spaceId))] = readOnly
        }
        return Notebook(
            spaceId = spaceId,
            content = content,
            updatedAtMillis = updatedAtMillis,
            isReadOnly = readOnly,
            revision = revision,
        )
    }

    private fun notebookFrom(prefs: Preferences, spaceId: String): Notebook = Notebook(
        spaceId = spaceId,
        content = prefs[stringPreferencesKey(contentKey(spaceId))].orEmpty(),
        updatedAtMillis = prefs[longPreferencesKey(updatedAtKey(spaceId))] ?: 0L,
        isReadOnly = prefs[booleanPreferencesKey(readOnlyKey(spaceId))] == true,
        revision = prefs[longPreferencesKey(revisionKey(spaceId))] ?: 0L,
    )

    private fun contentKey(spaceId: String): String = "notebook_content_$spaceId"

    private fun updatedAtKey(spaceId: String): String = "notebook_updated_at_$spaceId"

    private fun dirtyKey(spaceId: String): String = "notebook_dirty_$spaceId"

    private fun readOnlyKey(spaceId: String): String = "notebook_readonly_$spaceId"

    private fun revisionKey(spaceId: String): String = "notebook_revision_$spaceId"
}
