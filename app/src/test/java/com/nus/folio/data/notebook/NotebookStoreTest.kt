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

    @Test
    fun `write dirty then markClean preserves content`() = runTest {
        val store = createStore()

        val saved = store.write("space-a", "unsynced", dirty = true)

        assertTrue(store.isDirty("space-a"))
        store.markClean("space-a", saved.revision)

        assertEquals(false, store.isDirty("space-a"))
        assertEquals("unsynced", store.read("space-a").content)
        assertEquals(saved.updatedAtMillis, store.read("space-a").updatedAtMillis)
    }

    @Test
    fun `isDirty is false when space has never been saved`() = runTest {
        val store = createStore()

        assertEquals(false, store.isDirty("space-a"))
    }

    @Test
    fun `writeIfNotDirty skips remote snapshot when local is dirty`() = runTest {
        val store = createStore()
        store.write("space-a", "local draft", dirty = true)

        val result = store.writeIfNotDirty("space-a", "remote snapshot")

        assertEquals("local draft", result.content)
        assertEquals("local draft", store.read("space-a").content)
        assertTrue(store.isDirty("space-a"))
    }

    @Test
    fun `writeIfNotDirty caches remote snapshot when local is clean`() = runTest {
        val store = createStore()
        store.write("space-a", "stale cache", dirty = false)

        val result = store.writeIfNotDirty("space-a", "remote snapshot")

        assertEquals("remote snapshot", result.content)
        assertEquals("remote snapshot", store.read("space-a").content)
        assertEquals(false, store.isDirty("space-a"))
    }

    @Test
    fun `write persists readOnly flag`() = runTest {
        val store = createStore()

        val saved = store.write("space-a", "unsupported", readOnly = true)

        assertTrue(saved.isReadOnly)
        assertTrue(store.read("space-a").isReadOnly)
    }

    @Test
    fun `writeIfNotDirty caches remote readOnly flag when local is clean`() = runTest {
        val store = createStore()

        val result = store.writeIfNotDirty("space-a", "remote snapshot", readOnly = true)

        assertTrue(result.isReadOnly)
        assertTrue(store.read("space-a").isReadOnly)
    }

    @Test
    fun `write assigns a new revision on every local write`() = runTest {
        val store = createStore()

        val first = store.write("space-a", "first")
        val second = store.write("space-a", "second")

        assertTrue(first.revision > 0L)
        assertTrue(second.revision > first.revision)
        assertEquals(second.revision, store.read("space-a").revision)
    }

    @Test
    fun `markClean of an older revision does not clear a newer dirty draft`() = runTest {
        val store = createStore()
        val first = store.write("space-a", "first", dirty = true)
        val second = store.write("space-a", "second", dirty = true)

        store.markClean("space-a", first.revision)

        assertTrue(store.isDirty("space-a"))
        assertEquals("second", store.read("space-a").content)
        assertEquals(second.revision, store.read("space-a").revision)

        store.markClean("space-a", second.revision)

        assertEquals(false, store.isDirty("space-a"))
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
