package com.nus.folio.data.datasource

import com.nus.folio.domain.model.Space
import com.nus.folio.domain.model.SpacePage
import com.nus.folio.domain.model.SpacePaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Release stub — spaces API is not wired yet; returns local sample data.
 */
class SpaceDataSource(
    @Suppress("UNUSED_PARAMETER")
    accessTokenProvider: () -> String? = { null },
    @Suppress("UNUSED_PARAMETER")
    refreshAccessToken: suspend () -> String? = { null },
) {

    private val spacesMutex = Mutex()

    @Volatile
    private var spaces: List<Space> = sampleSpaces

    suspend fun fetchSpaces(
        searchQuery: String? = null,
        sort: String = DEFAULT_SORT,
        page: Int = SpacePaging.DEFAULT_PAGE,
        limit: Int = SpacePaging.DEFAULT_LIMIT,
    ): SpacePage {
        delay(200)
        val snapshot = spaces
        val query = searchQuery?.trim().orEmpty()
        val filtered = if (query.isEmpty()) {
            snapshot
        } else {
            snapshot.filter { it.title.contains(query, ignoreCase = true) }
        }
        val sorted = when (sort) {
            "alphabetical-az" -> filtered.sortedBy { it.title.lowercase() }
            "alphabetical-za" -> filtered.sortedByDescending { it.title.lowercase() }
            else -> filtered
        }
        val safePage = page.coerceAtLeast(1)
        val safeLimit = limit.coerceAtLeast(1)
        val offset = (safePage - 1) * safeLimit
        val pageItems = sorted.drop(offset).take(safeLimit)
        return SpacePage(
            spaces = pageItems,
            page = safePage,
            limit = safeLimit,
            hasMore = offset + pageItems.size < sorted.size,
        )
    }

    suspend fun createSpace(
        name: String,
        researchObjective: String,
    ): Space {
        require(name.isNotBlank()) { "Name is required" }
        delay(200)
        val space = Space(
            id = UUID.randomUUID().toString(),
            title = name.trim(),
            description = researchObjective.trim(),
            sourceCount = 0,
            noteCount = 0,
            updatedLabel = "Updated just now",
        )
        spacesMutex.withLock {
            spaces = listOf(space) + spaces
        }
        return space
    }

    suspend fun updateSpace(
        spaceId: String,
        name: String,
        researchObjective: String,
    ): Space {
        require(spaceId.isNotBlank()) { "Space id is required" }
        require(name.isNotBlank()) { "Name is required" }
        delay(200)
        val trimmedId = spaceId.trim()
        val updated = spacesMutex.withLock {
            val index = spaces.indexOfFirst { it.id == trimmedId }
            if (index < 0) {
                throw NoSuchElementException("Space not found: $trimmedId")
            }
            val space = spaces[index].copy(
                title = name.trim(),
                description = researchObjective.trim(),
                updatedLabel = "Updated just now",
            )
            spaces = spaces.toMutableList().also { it[index] = space }
            space
        }
        return updated
    }

    suspend fun deleteSpace(spaceId: String) {
        require(spaceId.isNotBlank()) { "Space id is required" }
        delay(200)
        val trimmedId = spaceId.trim()
        spacesMutex.withLock {
            val index = spaces.indexOfFirst { it.id == trimmedId }
            if (index < 0) {
                throw NoSuchElementException("Space not found: $trimmedId")
            }
            spaces = spaces.toMutableList().also { it.removeAt(index) }
        }
    }

    companion object {
        const val DEFAULT_SORT = "recently-updated"

        private val sampleSpaces = listOf(
            Space(
                id = "1",
                title = "Dissertation Research",
                description = "Primary research archive for doctoral thesis",
                sourceCount = 128,
                noteCount = 32,
                updatedLabel = "Updated 2d ago",
            ),
            Space(
                id = "2",
                title = "Public Policy Insights",
                description = "Policy papers and legislative analysis",
                sourceCount = 64,
                noteCount = 18,
                updatedLabel = "Updated 5h ago",
            ),
            Space(
                id = "3",
                title = "History of Science",
                description = "Scientific manuscripts and archival sources",
                sourceCount = 42,
                noteCount = 12,
                updatedLabel = "Updated 1w ago",
            ),
            Space(
                id = "4",
                title = "Teaching Prep",
                description = "Course materials and lecture notes",
                sourceCount = 27,
                noteCount = 8,
                updatedLabel = "Updated 3d ago",
            ),
        )
    }
}
