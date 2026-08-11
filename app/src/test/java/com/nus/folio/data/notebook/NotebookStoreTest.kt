package com.nus.folio.data.notebook

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NotebookStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `read returns empty notebook when space has never been saved`() = runTest {
        val store = createStore()

        val notebook = store.read("space-a")

        assertEquals("space-a", notebook.spaceId)
        assertEquals("", notebook.content)
        assertEquals(0L, notebook.updatedAtMillis)
    }

    @Test
    fun `write then read round trips content for space`() = runTest {
        val store = createStore()

        val saved = store.write("space-a", "# Title\nBody")
        val loaded = store.read("space-a")

        assertEquals("# Title\nBody", saved.content)
        assertEquals("# Title\nBody", loaded.content)
        assertEquals(saved.updatedAtMillis, loaded.updatedAtMillis)
        assertTrue(loaded.updatedAtMillis > 0L)
    }

    @Test
    fun `write isolates content by spaceId`() = runTest {
        val store = createStore()

        store.write("space-a", "Alpha")
        store.write("space-b", "Beta")

        assertEquals("Alpha", store.read("space-a").content)
        assertEquals("Beta", store.read("space-b").content)
    }

    @Test
    fun `write overwrites previous content for same space`() = runTest {
        val store = createStore()
        store.write("space-a", "first")

        val updated = store.write("space-a", "second")

        assertEquals("second", updated.content)
        assertEquals("second", store.read("space-a").content)
    }

    private fun createStore(): NotebookStore {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = {
                File(temporaryFolder.root, "notebook-${System.nanoTime()}.preferences_pb")
            },
        )
        return NotebookStore(dataStore)
    }
}
