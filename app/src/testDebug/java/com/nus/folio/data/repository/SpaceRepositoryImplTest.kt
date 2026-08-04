package com.nus.folio.data.repository

import com.nus.folio.data.datasource.SpaceDataSource
import com.nus.folio.data.network.SpacesApi
import com.nus.folio.domain.model.Space
import com.nus.folio.domain.model.SpacePage
import com.nus.folio.domain.model.SpacePaging
import com.nus.folio.domain.model.SpaceSort
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceRepositoryImplTest {

    private class RecordingSpacesApi : SpacesApi {
        var lastSearch: String? = null
        var lastSort: String? = null
        var lastPage: Int? = null
        var lastLimit: Int? = null
        var lastCreateName: String? = null
        var lastCreateObjective: String? = null

        override suspend fun listSpaces(
            accessToken: String,
            sort: String,
            search: String?,
            page: Int,
            limit: Int,
        ): SpacePage {
            lastSearch = search
            lastSort = sort
            lastPage = page
            lastLimit = limit
            return SpacePage(
                spaces = listOf(
                    Space(
                        id = "1",
                        title = "AI Ethics Research",
                        description = "Explore ethical frameworks",
                        sourceCount = 2,
                        noteCount = 0,
                        updatedLabel = "Updated just now",
                    ),
                ),
                page = page,
                limit = limit,
                hasMore = false,
            )
        }

        override suspend fun createSpace(
            accessToken: String,
            name: String,
            researchObjective: String,
        ): Space {
            lastCreateName = name
            lastCreateObjective = researchObjective
            return Space(
                id = "created",
                title = name,
                description = researchObjective,
                sourceCount = 0,
                noteCount = 0,
                updatedLabel = "Updated just now",
            )
        }
    }

    private val api = RecordingSpacesApi()
    private val repository = SpaceRepositoryImpl(
        SpaceDataSource(
            spacesApi = api,
            accessTokenProvider = { "token" },
        ),
    )

    @Test
    fun `getSpaces returns success page`() = runTest {
        val result = repository.getSpaces()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.spaces?.size)
        assertEquals("AI Ethics Research", result.getOrNull()?.spaces?.first()?.title)
        assertEquals(SpaceSort.DEFAULT.apiValue, api.lastSort)
        assertEquals(SpacePaging.DEFAULT_PAGE, api.lastPage)
        assertEquals(SpacePaging.DEFAULT_LIMIT, api.lastLimit)
        assertFalse(result.getOrNull()!!.hasMore)
    }

    @Test
    fun `getSpaces forwards search query`() = runTest {
        repository.getSpaces(searchQuery = "Ethics")

        assertEquals("Ethics", api.lastSearch)
    }

    @Test
    fun `getSpaces forwards sort api value`() = runTest {
        repository.getSpaces(sort = SpaceSort.RECENTLY_CREATED)

        assertEquals("recently-created", api.lastSort)
    }

    @Test
    fun `getSpaces forwards page and limit`() = runTest {
        repository.getSpaces(page = 2, limit = 10)

        assertEquals(2, api.lastPage)
        assertEquals(10, api.lastLimit)
    }

    @Test
    fun `createSpace returns created space`() = runTest {
        val result = repository.createSpace(
            name = "New Space",
            researchObjective = "Objective",
        )

        assertTrue(result.isSuccess)
        assertEquals("created", result.getOrNull()?.id)
        assertEquals("New Space", api.lastCreateName)
        assertEquals("Objective", api.lastCreateObjective)
    }
}
