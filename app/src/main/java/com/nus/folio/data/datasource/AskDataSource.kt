package com.nus.folio.data.datasource

import com.nus.folio.domain.model.AskTopic
import kotlinx.coroutines.delay

class AskDataSource {

    suspend fun fetchAskTopics(spaceId: String): List<AskTopic> {
        delay(200)
        return sampleTopics.filter { it.spaceId == spaceId }
    }

    companion object {
        private val sampleTopics = listOf(
            AskTopic(
                id = "1a",
                title = "Core dissertation arguments",
                sourceCount = 4,
                noteCount = 2,
                spaceId = "1",
            ),
            AskTopic(
                id = "1b",
                title = "Turing and modern AI",
                sourceCount = 3,
                noteCount = 1,
                spaceId = "1",
            ),
            AskTopic(
                id = "2a",
                title = "Policy brief themes",
                sourceCount = 2,
                noteCount = 2,
                spaceId = "2",
            ),
            AskTopic(
                id = "3a",
                title = "Scientific manuscripts timeline",
                sourceCount = 1,
                noteCount = 1,
                spaceId = "3",
            ),
            AskTopic(
                id = "4a",
                title = "Week 7 lecture prep",
                sourceCount = 2,
                noteCount = 1,
                spaceId = "4",
            ),
        )
    }
}
