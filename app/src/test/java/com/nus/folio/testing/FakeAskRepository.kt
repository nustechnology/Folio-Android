package com.nus.folio.testing

import com.nus.folio.domain.model.AskTopic
import com.nus.folio.domain.repository.AskRepository

class FakeAskRepository : AskRepository {

    var getAskTopicsResult: Result<List<AskTopic>> = Result.success(sampleTopics)
    var getAskTopicsCallCount = 0

    override suspend fun getAskTopics(): Result<List<AskTopic>> {
        getAskTopicsCallCount++
        return getAskTopicsResult
    }

    companion object {
        val sampleTopics = listOf(
            AskTopic("1", "Dissertation Research", 128, 32),
            AskTopic("2", "Public Policy Insights", 64, 18),
            AskTopic("3", "History of Science", 42, 12),
            AskTopic("4", "Teaching Prep", 27, 8),
        )
    }
}
