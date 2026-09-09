package com.nus.folio.data.datasource

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.nus.folio.data.notebook.NotebookStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NotebookDataSourceSaveTest {

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
    fun `saveNotebook writes locally`() = runTest {
        val dataSource = NotebookDataSource(createStore())

        val saved = dataSource.saveNotebook("space-1", "# Title")

        assertEquals("# Title", saved.content)
        assertEquals("# Title", dataSource.fetchNotebook("space-1").content)
    }

    @Test
    fun `fetchNotebook reads locally`() = runTest {
        val dataSource = NotebookDataSource(createStore())
        dataSource.saveNotebook("space-1", "# Title")

        assertEquals("# Title", dataSource.fetchNotebook("space-1").content)
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
