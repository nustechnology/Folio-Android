package com.nus.folio.data.datasource

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.nus.folio.data.network.NotebookApi
import com.nus.folio.data.network.NotebookNotFoundException
import com.nus.folio.data.network.UnauthorizedException
import com.nus.folio.data.notebook.NotebookStore
import com.nus.folio.domain.model.Notebook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private class FakeNotebookApi : NotebookApi {
        var lastAccessToken: String? = null
        var lastSpaceId: String? = null
        var lastMarkdown: String? = null
        var putCallCount = 0
        var getCallCount = 0
        var failUnauthorizedOnce = false
        var failGetUnauthorizedOnce = false
        private val markdownBySpace = mutableMapOf<String, String>()

        override suspend fun getNotebook(
            accessToken: String,
            spaceId: String,
        ): Notebook {
            getCallCount++
            lastAccessToken = accessToken
            lastSpaceId = spaceId
            if (failGetUnauthorizedOnce && accessToken == "expired-token") {
                failGetUnauthorizedOnce = false
                throw UnauthorizedException("Get notebook failed (HTTP 401)")
            }
            return Notebook(
                spaceId = spaceId,
                content = markdownBySpace[spaceId].orEmpty(),
                updatedAtMillis = 1L,
            )
        }

        override suspend fun putNotebook(
            accessToken: String,
            spaceId: String,
            markdown: String,
        ) {
            putCallCount++
            lastAccessToken = accessToken
            lastSpaceId = spaceId
            lastMarkdown = markdown
            if (failUnauthorizedOnce && accessToken == "expired-token") {
                failUnauthorizedOnce = false
                throw UnauthorizedException("Save notebook failed (HTTP 401)")
            }
            markdownBySpace[spaceId] = markdown
        }
    }

    @Test
    fun `saveNotebook writes locally then puts to API`() = runTest {
        val api = FakeNotebookApi()
        val store = createStore()
        val dataSource = NotebookDataSource(
            store = store,
            accessTokenProvider = { "access-token" },
            notebookApi = api,
        )

        val saved = dataSource.saveNotebook("space-1", "# Title\n\nBody")

        assertEquals(1, api.putCallCount)
        assertEquals("access-token", api.lastAccessToken)
        assertEquals("space-1", api.lastSpaceId)
        assertEquals("# Title\n\nBody", api.lastMarkdown)
        assertEquals("# Title\n\nBody", saved.content)
        assertEquals("# Title\n\nBody", store.read("space-1").content)
        assertEquals(false, store.isDirty("space-1"))
        assertEquals("# Title\n\nBody", dataSource.fetchNotebook("space-1").content)
        assertTrue(saved.updatedAtMillis > 0L)
    }

    @Test
    fun `saveNotebook retries once after 401`() = runTest {
        val api = FakeNotebookApi().apply { failUnauthorizedOnce = true }
        var token = "expired-token"
        val dataSource = NotebookDataSource(
            store = createStore(),
            accessTokenProvider = { token },
            refreshAccessToken = {
                token = "fresh-token"
                token
            },
            notebookApi = api,
        )

        dataSource.saveNotebook("space-1", "draft")

        assertEquals(2, api.putCallCount)
        assertEquals("fresh-token", api.lastAccessToken)
        assertEquals("draft", api.lastMarkdown)
    }

    @Test
    fun `saveNotebook accepts empty content`() = runTest {
        val api = FakeNotebookApi()
        val dataSource = NotebookDataSource(
            store = createStore(),
            accessTokenProvider = { "access-token" },
            notebookApi = api,
        )

        val saved = dataSource.saveNotebook("space-1", "")

        assertEquals("", api.lastMarkdown)
        assertEquals("", saved.content)
    }

    @Test
    fun `saveNotebook keeps dirty local copy when API fails`() = runTest {
        val api = object : NotebookApi {
            override suspend fun getNotebook(
                accessToken: String,
                spaceId: String,
            ): Notebook = Notebook(spaceId = spaceId, content = "remote")

            override suspend fun putNotebook(
                accessToken: String,
                spaceId: String,
                markdown: String,
            ) {
                throw java.io.IOException("Save notebook failed (HTTP 500)")
            }
        }
        val store = createStore()
        val dataSource = NotebookDataSource(
            store = store,
            accessTokenProvider = { "access-token" },
            notebookApi = api,
        )

        try {
            dataSource.saveNotebook("space-1", "draft")
            org.junit.Assert.fail("Expected saveNotebook to fail")
        } catch (error: java.io.IOException) {
            assertEquals("Save notebook failed (HTTP 500)", error.message)
        }

        assertEquals("draft", store.read("space-1").content)
        assertTrue(store.isDirty("space-1"))
        assertEquals("draft", dataSource.fetchNotebook("space-1").content)
    }

    @Test
    fun `saveNotebook clears dirty after a later successful PUT`() = runTest {
        var failPut = true
        val api = object : NotebookApi {
            override suspend fun getNotebook(
                accessToken: String,
                spaceId: String,
            ): Notebook = Notebook(spaceId = spaceId, content = "remote")

            override suspend fun putNotebook(
                accessToken: String,
                spaceId: String,
                markdown: String,
            ) {
                if (failPut) throw java.io.IOException("Save notebook failed (HTTP 500)")
            }
        }
        val store = createStore()
        val dataSource = NotebookDataSource(
            store = store,
            accessTokenProvider = { "access-token" },
            notebookApi = api,
        )

        try {
            dataSource.saveNotebook("space-1", "draft")
            org.junit.Assert.fail("Expected saveNotebook to fail")
        } catch (_: java.io.IOException) {
        }

        failPut = false
        dataSource.saveNotebook("space-1", "draft")

        assertEquals("draft", store.read("space-1").content)
        assertEquals(false, store.isDirty("space-1"))
    }

    @Test
    fun `overlapping save does not mark a newer failed draft clean`() = runTest {
        val firstPutStarted = CompletableDeferred<Unit>()
        val allowFirstPutToFinish = CompletableDeferred<Unit>()
        var putCount = 0
        val api = object : NotebookApi {
            override suspend fun getNotebook(
                accessToken: String,
                spaceId: String,
            ): Notebook = Notebook(spaceId = spaceId, content = "remote")

            override suspend fun putNotebook(
                accessToken: String,
                spaceId: String,
                markdown: String,
            ) {
                putCount++
                if (putCount == 1) {
                    firstPutStarted.complete(Unit)
                    allowFirstPutToFinish.await()
                    return
                }
                throw java.io.IOException("Save notebook failed (HTTP 500)")
            }
        }
        val store = createStore()
        val dataSource = NotebookDataSource(
            store = store,
            accessTokenProvider = { "access-token" },
            notebookApi = api,
        )

        val firstSave = launch { dataSource.saveNotebook("space-1", "first") }
        firstPutStarted.await()

        try {
            dataSource.saveNotebook("space-1", "second")
            org.junit.Assert.fail("Expected newer saveNotebook to fail")
        } catch (_: java.io.IOException) {
        }

        allowFirstPutToFinish.complete(Unit)
        firstSave.join()

        assertEquals("second", store.read("space-1").content)
        assertTrue(store.isDirty("space-1"))
        assertEquals("second", dataSource.fetchNotebook("space-1").content)
    }

    @Test
    fun `fetchNotebook prefers dirty local copy over remote`() = runTest {
        val api = FakeNotebookApi()
        api.putNotebook("access-token", "space-1", "remote snapshot")
        val store = createStore()
        store.write("space-1", "local draft", dirty = true)
        val dataSource = NotebookDataSource(
            store = store,
            accessTokenProvider = { "access-token" },
            notebookApi = api,
        )

        val fetched = dataSource.fetchNotebook("space-1")

        assertEquals("local draft", fetched.content)
        assertEquals("local draft", store.read("space-1").content)
        assertTrue(store.isDirty("space-1"))
        assertEquals(0, api.getCallCount)
    }

    @Test(expected = IllegalStateException::class)
    fun `saveNotebook requires authentication`() = runTest {
        val dataSource = NotebookDataSource(
            store = createStore(),
            accessTokenProvider = { null },
            notebookApi = FakeNotebookApi(),
        )
        dataSource.saveNotebook("space-1", "draft")
    }

    @Test
    fun `fetchNotebook gets from API then caches markdown locally`() = runTest {
        val api = FakeNotebookApi()
        api.putNotebook("access-token", "space-1", "# Aged-Care Operations")
        val dataSource = NotebookDataSource(
            store = createStore(),
            accessTokenProvider = { "access-token" },
            notebookApi = api,
        )

        val fetched = dataSource.fetchNotebook("space-1")

        assertEquals(1, api.getCallCount)
        assertEquals("access-token", api.lastAccessToken)
        assertEquals("space-1", api.lastSpaceId)
        assertEquals("# Aged-Care Operations", fetched.content)
        assertEquals("# Aged-Care Operations", dataSource.fetchNotebook("space-1").content)
        assertEquals(2, api.getCallCount)
    }

    @Test
    fun `fetchNotebook retries once after 401`() = runTest {
        val api = FakeNotebookApi().apply { failGetUnauthorizedOnce = true }
        api.putNotebook("fresh-token", "space-1", "draft")
        var token = "expired-token"
        val dataSource = NotebookDataSource(
            store = createStore(),
            accessTokenProvider = { token },
            refreshAccessToken = {
                token = "fresh-token"
                token
            },
            notebookApi = api,
        )

        val fetched = dataSource.fetchNotebook("space-1")

        assertEquals(2, api.getCallCount)
        assertEquals("fresh-token", api.lastAccessToken)
        assertEquals("draft", fetched.content)
    }

    @Test
    fun `fetchNotebook caches successful blank remote content`() = runTest {
        val store = createStore()
        store.write("space-1", "# Cached notes")
        val dataSource = NotebookDataSource(
            store = store,
            accessTokenProvider = { "access-token" },
            notebookApi = FakeNotebookApi(),
        )

        val fetched = dataSource.fetchNotebook("space-1")

        assertEquals("", fetched.content)
        assertEquals("", store.read("space-1").content)
    }

    @Test
    fun `fetchNotebook keeps cached markdown when notebook is not found`() = runTest {
        val store = createStore()
        val cached = store.write("space-1", "# Cached notes")
        val api = object : NotebookApi {
            override suspend fun getNotebook(
                accessToken: String,
                spaceId: String,
            ): Notebook = throw NotebookNotFoundException("Get notebook failed (HTTP 404)")

            override suspend fun putNotebook(
                accessToken: String,
                spaceId: String,
                markdown: String,
            ) = Unit
        }
        val dataSource = NotebookDataSource(
            store = store,
            accessTokenProvider = { "access-token" },
            notebookApi = api,
        )

        val fetched = dataSource.fetchNotebook("space-1")

        assertEquals("# Cached notes", fetched.content)
        assertEquals(cached.updatedAtMillis, fetched.updatedAtMillis)
        assertEquals("# Cached notes", store.read("space-1").content)
        assertEquals(false, fetched.isStale)
    }

    @Test
    fun `fetchNotebook returns cached markdown when GET fails with IOException`() = runTest {
        val store = createStore()
        val cached = store.write("space-1", "# Cached notes")
        val api = object : NotebookApi {
            override suspend fun getNotebook(
                accessToken: String,
                spaceId: String,
            ): Notebook = throw java.io.IOException("Get notebook failed (HTTP 500)")

            override suspend fun putNotebook(
                accessToken: String,
                spaceId: String,
                markdown: String,
            ) = Unit
        }
        val dataSource = NotebookDataSource(
            store = store,
            accessTokenProvider = { "access-token" },
            notebookApi = api,
        )

        val fetched = dataSource.fetchNotebook("space-1")

        assertEquals("# Cached notes", fetched.content)
        assertEquals(cached.updatedAtMillis, fetched.updatedAtMillis)
        assertTrue(fetched.isStale)
        assertEquals("# Cached notes", store.read("space-1").content)
    }

    @Test
    fun `fetchNotebook returns empty stale notebook when GET fails and cache is empty`() = runTest {
        val api = object : NotebookApi {
            override suspend fun getNotebook(
                accessToken: String,
                spaceId: String,
            ): Notebook = throw java.io.IOException("Unable to resolve host")

            override suspend fun putNotebook(
                accessToken: String,
                spaceId: String,
                markdown: String,
            ) = Unit
        }
        val dataSource = NotebookDataSource(
            store = createStore(),
            accessTokenProvider = { "access-token" },
            notebookApi = api,
        )

        val fetched = dataSource.fetchNotebook("space-1")

        assertEquals("", fetched.content)
        assertTrue(fetched.isStale)
    }

    @Test(expected = UnauthorizedException::class)
    fun `fetchNotebook rethrows UnauthorizedException after failed refresh`() = runTest {
        val store = createStore()
        store.write("space-1", "# Cached notes")
        val api = object : NotebookApi {
            override suspend fun getNotebook(
                accessToken: String,
                spaceId: String,
            ): Notebook = throw UnauthorizedException("Get notebook failed (HTTP 401)")

            override suspend fun putNotebook(
                accessToken: String,
                spaceId: String,
                markdown: String,
            ) = Unit
        }
        val dataSource = NotebookDataSource(
            store = store,
            accessTokenProvider = { "expired-token" },
            refreshAccessToken = { null },
            notebookApi = api,
        )

        dataSource.fetchNotebook("space-1")
    }

    @Test(expected = IllegalStateException::class)
    fun `fetchNotebook requires authentication`() = runTest {
        val dataSource = NotebookDataSource(
            store = createStore(),
            accessTokenProvider = { null },
            notebookApi = FakeNotebookApi(),
        )
        dataSource.fetchNotebook("space-1")
    }

    @Test
    fun `saveNotebook refuses to put a read-only notebook`() = runTest {
        val store = createStore()
        store.write("space-1", "# T\n\nSub", readOnly = true)
        var putCallCount = 0
        val api = object : NotebookApi {
            override suspend fun getNotebook(
                accessToken: String,
                spaceId: String,
            ): Notebook = Notebook(spaceId = spaceId, content = "")

            override suspend fun putNotebook(
                accessToken: String,
                spaceId: String,
                markdown: String,
            ) {
                putCallCount++
            }
        }
        val dataSource = NotebookDataSource(
            store = store,
            accessTokenProvider = { "access-token" },
            notebookApi = api,
        )

        try {
            dataSource.saveNotebook("space-1", "# T\n\nSub edited")
            org.junit.Assert.fail("Expected saveNotebook to refuse read-only notebook")
        } catch (error: IllegalStateException) {
            assertEquals("Notebook is read-only", error.message)
        }

        assertEquals(0, putCallCount)
        assertEquals("# T\n\nSub", store.read("space-1").content)
        assertTrue(store.read("space-1").isReadOnly)
        assertEquals(false, store.isDirty("space-1"))
    }

    @Test
    fun `fetchNotebook caches remote readOnly flag`() = runTest {
        val api = object : NotebookApi {
            override suspend fun getNotebook(
                accessToken: String,
                spaceId: String,
            ): Notebook = Notebook(
                spaceId = spaceId,
                content = "# T\n\nSub\n\ncode()\n\nBody",
                isReadOnly = true,
            )

            override suspend fun putNotebook(
                accessToken: String,
                spaceId: String,
                markdown: String,
            ) = Unit
        }
        val store = createStore()
        val dataSource = NotebookDataSource(
            store = store,
            accessTokenProvider = { "access-token" },
            notebookApi = api,
        )

        val fetched = dataSource.fetchNotebook("space-1")

        assertTrue(fetched.isReadOnly)
        assertTrue(store.read("space-1").isReadOnly)
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
