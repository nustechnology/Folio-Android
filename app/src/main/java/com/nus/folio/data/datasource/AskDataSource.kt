package com.nus.folio.data.datasource

import com.nus.folio.domain.model.AskTopic
import kotlinx.coroutines.delay

class AskDataSource {

    suspend fun fetchAskTopics(): List<AskTopic> {
        delay(200)
        return sampleTopics
    }

    companion object {
        private val sampleTopics = listOf(
            AskTopic(
                id = "1",
                title = "Dissertation Research",
                sourceCount = 128,
                noteCount = 32,
            ),
            AskTopic(
                id = "2",
                title = "Public Policy Insights",
                sourceCount = 64,
                noteCount = 18,
            ),
            AskTopic(
                id = "3",
                title = "History of Science",
                sourceCount = 42,
                noteCount = 12,
            ),
            AskTopic(
                id = "4",
                title = "Teaching Prep",
                sourceCount = 27,
                noteCount = 8,
            ),
        )
    }
}
