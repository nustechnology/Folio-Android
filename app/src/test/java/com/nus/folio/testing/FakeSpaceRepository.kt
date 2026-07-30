package com.nus.folio.testing

import com.nus.folio.domain.model.Space
import com.nus.folio.domain.repository.SpaceRepository

class FakeSpaceRepository(
    var spacesResult: Result<List<Space>> = Result.success(sampleSpaces),
) : SpaceRepository {
    override suspend fun getSpaces(): Result<List<Space>> = spacesResult

    companion object {
        val sampleSpaces = listOf(
            Space("1", "Dissertation Research", "Primary research archive for doctoral thesis", 128, 32, "Updated 2d ago"),
            Space("2", "Public Policy Insights", "Policy papers and legislative analysis", 64, 18, "Updated 5h ago"),
            Space("3", "History of Science", "Scientific manuscripts and archival sources", 42, 12, "Updated 1w ago"),
            Space("4", "Teaching Prep", "Course materials and lecture notes", 27, 8, "Updated 3d ago"),
        )
    }
}
