package com.nus.folio.testing

import com.nus.folio.domain.model.Space
import com.nus.folio.domain.model.SpacePage
import com.nus.folio.domain.model.SpacePaging
import com.nus.folio.domain.model.SpaceSort
import com.nus.folio.domain.repository.SpaceRepository

class FakeSpaceRepository(
    var spacesResult: Result<SpacePage> = Result.success(
        SpacePage(
            spaces = sampleSpaces,
            page = SpacePaging.DEFAULT_PAGE,
            limit = SpacePaging.DEFAULT_LIMIT,
            hasMore = false,
        ),
    ),
    var createSpaceResult: Result<Space> = Result.success(
        Space("new", "New Space", "Objective", 0, 0, "Updated just now"),
    ),
) : SpaceRepository {

    var lastSearchQuery: String? = null
    var lastSort: SpaceSort? = null
    var lastPage: Int? = null
    var lastLimit: Int? = null
    var lastCreateName: String? = null
    var lastCreateObjective: String? = null
    var getSpacesCallCount = 0
    var createSpaceCallCount = 0
    var getSpacesGate: (suspend (searchQuery: String?) -> Unit)? = null

    /** Optional per-call overrides keyed by page for load-more tests. */
    var pageResults: Map<Int, Result<SpacePage>>? = null

    override suspend fun getSpaces(
        searchQuery: String?,
        sort: SpaceSort,
        page: Int,
        limit: Int,
    ): Result<SpacePage> {
        getSpacesCallCount++
        lastSearchQuery = searchQuery
        lastSort = sort
        lastPage = page
        lastLimit = limit
        getSpacesGate?.invoke(searchQuery)
        return pageResults?.get(page) ?: spacesResult
    }

    override suspend fun createSpace(
        name: String,
        researchObjective: String,
    ): Result<Space> {
        createSpaceCallCount++
        lastCreateName = name
        lastCreateObjective = researchObjective
        return createSpaceResult
    }

    companion object {
        val sampleSpaces = listOf(
            Space("1", "Dissertation Research", "Primary research archive for doctoral thesis", 128, 32, "Updated 2d ago"),
            Space("2", "Public Policy Insights", "Policy papers and legislative analysis", 64, 18, "Updated 5h ago"),
            Space("3", "History of Science", "Scientific manuscripts and archival sources", 42, 12, "Updated 1w ago"),
            Space("4", "Teaching Prep", "Course materials and lecture notes", 27, 8, "Updated 3d ago"),
        )

        fun pageOf(
            spaces: List<Space>,
            page: Int = SpacePaging.DEFAULT_PAGE,
            limit: Int = SpacePaging.DEFAULT_LIMIT,
            hasMore: Boolean = false,
        ): SpacePage = SpacePage(
            spaces = spaces,
            page = page,
            limit = limit,
            hasMore = hasMore,
        )
    }
}
