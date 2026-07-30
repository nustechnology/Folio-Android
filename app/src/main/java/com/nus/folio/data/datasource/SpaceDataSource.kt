package com.nus.folio.data.datasource

import com.nus.folio.domain.model.Space
import kotlinx.coroutines.delay

class SpaceDataSource {

    suspend fun fetchSpaces(): List<Space> {
        delay(200)
        return sampleSpaces
    }

    companion object {
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
