package com.nus.folio.data.datasource

import com.nus.folio.data.network.SpacesApi
import com.nus.folio.data.network.UnauthorizedException
import com.nus.folio.domain.model.Space
import com.nus.folio.domain.model.SpacePage
import com.nus.folio.domain.model.SpacePaging
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SpaceDataSourceTest {

    private class FakeSpacesApi : SpacesApi {
        var lastAccessToken: String? = null
        var lastSort: String? = null
        var lastSearch: String? = null
        var lastPage: Int? = null
        var lastLimit: Int? = null
        var lastCreateName: String? = null
        var lastCreateObjective: String? = null
        var lastUpdateSpaceId: String? = null
        var lastUpdateName: String? = null
        var lastUpdateObjective: String? = null
        var lastDeleteSpaceId: String? = null
        var listCallCount = 0
        var failUnauthorizedOnce = false
        var spaces: List<Space> = listOf(
            Space(
                id = "cae32514-0007-4f9f-86ed-ba022e7a336f",
                title = "AI Ethics Research",
                description = "Explore ethical frameworks",
                sourceCount = 2,
                noteCount = 0,
                updatedLabel = "Updated just now",
            ),
        )

        override suspend fun listSpaces(
            accessToken: String,
            sort: String,
            search: String?,
            page: Int,
            limit: Int,
        ): SpacePage {
            listCallCount++
            lastAccessToken = accessToken
            lastSort = sort
            lastSearch = search
            lastPage = page
            lastLimit = limit
            if (failUnauthorizedOnce && accessToken == "expired-token") {
                throw UnauthorizedException("Get spaces failed (HTTP 401)")
            }
            return SpacePage(
                spaces = spaces,
                page = page,
                limit = limit,
                hasMore = spaces.size >= limit,
            )
        }

        override suspend fun createSpace(
            accessToken: String,
            name: String,
            researchObjective: String,
        ): Space {
            lastAccessToken = accessToken
            lastCreateName = name
            lastCreateObjective = researchObjective
            return Space(
                id = "b2c3d4e5-f6a7-8901-bcde-f12345678901",
                title = name,
                description = researchObjective,
                sourceCount = 0,
                noteCount = 0,
                updatedLabel = "Updated just now",
            )
        }

        override suspend fun updateSpace(
            accessToken: String,
            spaceId: String,
            name: String,
            researchObjective: String,
        ): Space {
            lastAccessToken = accessToken
            lastUpdateSpaceId = spaceId
            lastUpdateName = name
            lastUpdateObjective = researchObjective
            return Space(
                id = spaceId,
                title = name,
                description = researchObjective,
                sourceCount = 2,
                noteCount = 0,
                updatedLabel = "Updated just now",
            )
        }

        override suspend fun deleteSpace(
            accessToken: String,
            spaceId: String,
        ) {
            lastAccessToken = accessToken
            lastDeleteSpaceId = spaceId
        }
    }

    @Test
    fun `fetchSpaces calls api with token and default sort`() = runTest {
        val api = FakeSpacesApi()
        val dataSource = SpaceDataSource(
            spacesApi = api,
            accessTokenProvider = { "access-token" },
        )

        val page = dataSource.fetchSpaces()

        assertEquals(1, page.spaces.size)
        assertEquals("AI Ethics Research", page.spaces.first().title)
        assertEquals("access-token", api.lastAccessToken)
        assertEquals(SpacesApi.DEFAULT_SORT, api.lastSort)
        assertEquals(SpacePaging.DEFAULT_PAGE, api.lastPage)
        assertEquals(SpacePaging.DEFAULT_LIMIT, api.lastLimit)
        assertNull(api.lastSearch)
        assertFalse(page.hasMore)
    }

    @Test
    fun `fetchSpaces forwards search query page and limit`() = runTest {
        val api = FakeSpacesApi()
        val dataSource = SpaceDataSource(
            spacesApi = api,
            accessTokenProvider = { "access-token" },
        )

        dataSource.fetchSpaces(searchQuery = "  Ethics  ", page = 3, limit = 5)

        assertEquals("Ethics", api.lastSearch)
        assertEquals(3, api.lastPage)
        assertEquals(5, api.lastLimit)
    }

    @Test(expected = IllegalStateException::class)
    fun `fetchSpaces throws when not authenticated`() = runTest {
        SpaceDataSource(
            spacesApi = FakeSpacesApi(),
            accessTokenProvider = { null },
        ).fetchSpaces()
    }

    @Test
    fun `createSpace posts name and objective`() = runTest {
        val api = FakeSpacesApi()
        val dataSource = SpaceDataSource(
            spacesApi = api,
            accessTokenProvider = { "access-token" },
        )

        val space = dataSource.createSpace(
            name = "  AI Ethics Research  ",
            researchObjective = "  Explore frameworks  ",
        )

        assertEquals("b2c3d4e5-f6a7-8901-bcde-f12345678901", space.id)
        assertEquals("AI Ethics Research", api.lastCreateName)
        assertEquals("Explore frameworks", api.lastCreateObjective)
        assertEquals("access-token", api.lastAccessToken)
    }

    @Test
    fun `updateSpace patches name and objective`() = runTest {
        val api = FakeSpacesApi()
        val dataSource = SpaceDataSource(
            spacesApi = api,
            accessTokenProvider = { "access-token" },
        )

        val space = dataSource.updateSpace(
            spaceId = "  cae32514-0007-4f9f-86ed-ba022e7a336f  ",
            name = "  Renamed space  ",
            researchObjective = "  Updated objective  ",
        )

        assertEquals("cae32514-0007-4f9f-86ed-ba022e7a336f", space.id)
        assertEquals("cae32514-0007-4f9f-86ed-ba022e7a336f", api.lastUpdateSpaceId)
        assertEquals("Renamed space", api.lastUpdateName)
        assertEquals("Updated objective", api.lastUpdateObjective)
        assertEquals("access-token", api.lastAccessToken)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createSpace throws when name is blank`() = runTest {
        SpaceDataSource(
            spacesApi = FakeSpacesApi(),
            accessTokenProvider = { "access-token" },
        ).createSpace(name = " ", researchObjective = "Objective")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateSpace throws when name is blank`() = runTest {
        SpaceDataSource(
            spacesApi = FakeSpacesApi(),
            accessTokenProvider = { "access-token" },
        ).updateSpace(
            spaceId = "cae32514-0007-4f9f-86ed-ba022e7a336f",
            name = " ",
            researchObjective = "Objective",
        )
    }

    @Test
    fun `deleteSpace calls api with token and space id`() = runTest {
        val api = FakeSpacesApi()
        val dataSource = SpaceDataSource(
            spacesApi = api,
            accessTokenProvider = { "access-token" },
        )

        dataSource.deleteSpace(spaceId = "  cae32514-0007-4f9f-86ed-ba022e7a336f  ")

        assertEquals("cae32514-0007-4f9f-86ed-ba022e7a336f", api.lastDeleteSpaceId)
        assertEquals("access-token", api.lastAccessToken)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `deleteSpace throws when space id is blank`() = runTest {
        SpaceDataSource(
            spacesApi = FakeSpacesApi(),
            accessTokenProvider = { "access-token" },
        ).deleteSpace(spaceId = " ")
    }

    @Test
    fun `fetchSpaces refreshes token and retries on 401`() = runTest {
        val api = FakeSpacesApi().apply { failUnauthorizedOnce = true }
        var token = "expired-token"
        val dataSource = SpaceDataSource(
            spacesApi = api,
            accessTokenProvider = { token },
            refreshAccessToken = {
                token = "fresh-token"
                token
            },
        )

        val page = dataSource.fetchSpaces()

        assertEquals(1, page.spaces.size)
        assertEquals(2, api.listCallCount)
        assertEquals("fresh-token", api.lastAccessToken)
    }

    @Test(expected = UnauthorizedException::class)
    fun `fetchSpaces rethrows when refresh fails after 401`() = runTest {
        val api = FakeSpacesApi().apply { failUnauthorizedOnce = true }
        SpaceDataSource(
            spacesApi = api,
            accessTokenProvider = { "expired-token" },
            refreshAccessToken = { null },
        ).fetchSpaces()
    }
}
