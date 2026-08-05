package com.nus.folio.data.datasource

import com.nus.folio.data.network.SpacesApi
import com.nus.folio.data.network.SpacesApiClient
import com.nus.folio.data.network.UnauthorizedException
import com.nus.folio.domain.model.Space
import com.nus.folio.domain.model.SpacePage
import com.nus.folio.domain.model.SpacePaging

/**
 * Debug spaces data source — loads/creates spaces via the real API.
 * On HTTP 401, refreshes the access token once and retries.
 */
class SpaceDataSource(
    private val spacesApi: SpacesApi = SpacesApiClient(),
    private val accessTokenProvider: () -> String? = { null },
    private val refreshAccessToken: suspend () -> String? = { null },
) {

    suspend fun fetchSpaces(
        searchQuery: String? = null,
        sort: String = SpacesApi.DEFAULT_SORT,
        page: Int = SpacePaging.DEFAULT_PAGE,
        limit: Int = SpacePaging.DEFAULT_LIMIT,
    ): SpacePage {
        val search = searchQuery?.trim()?.takeIf { it.isNotEmpty() }
        return withAuthRetry { accessToken ->
            spacesApi.listSpaces(
                accessToken = accessToken,
                sort = sort,
                search = search,
                page = page,
                limit = limit,
            )
        }
    }

    suspend fun createSpace(
        name: String,
        researchObjective: String,
    ): Space {
        require(name.isNotBlank()) { "Name is required" }
        val trimmedName = name.trim()
        val trimmedObjective = researchObjective.trim()
        return withAuthRetry { accessToken ->
            spacesApi.createSpace(
                accessToken = accessToken,
                name = trimmedName,
                researchObjective = trimmedObjective,
            )
        }
    }

    suspend fun updateSpace(
        spaceId: String,
        name: String,
        researchObjective: String,
    ): Space {
        require(spaceId.isNotBlank()) { "Space id is required" }
        require(name.isNotBlank()) { "Name is required" }
        val trimmedName = name.trim()
        val trimmedObjective = researchObjective.trim()
        return withAuthRetry { accessToken ->
            spacesApi.updateSpace(
                accessToken = accessToken,
                spaceId = spaceId.trim(),
                name = trimmedName,
                researchObjective = trimmedObjective,
            )
        }
    }

    suspend fun deleteSpace(spaceId: String) {
        require(spaceId.isNotBlank()) { "Space id is required" }
        withAuthRetry { accessToken ->
            spacesApi.deleteSpace(
                accessToken = accessToken,
                spaceId = spaceId.trim(),
            )
        }
    }

    private suspend fun <T> withAuthRetry(block: suspend (accessToken: String) -> T): T {
        val accessToken = requireAccessToken()
        return try {
            block(accessToken)
        } catch (unauthorized: UnauthorizedException) {
            val refreshedToken = refreshAccessToken()?.takeIf { it.isNotBlank() }
                ?: throw unauthorized
            block(refreshedToken)
        }
    }

    private fun requireAccessToken(): String =
        accessTokenProvider()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Not authenticated")
}
