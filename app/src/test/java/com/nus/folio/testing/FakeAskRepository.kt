package com.nus.folio.testing

import com.nus.folio.domain.model.AskTopic
import com.nus.folio.domain.repository.AskRepository

class FakeAskRepository : AskRepository {

    var getAskTopicsResult: Result<List<AskTopic>>? = null
    var getAskTopicsCallCount = 0
    var lastSpaceId: String? = null

    override suspend fun getAskTopics(spaceId: String): Result<List<AskTopic>> {
        getAskTopicsCallCount++
        lastSpaceId = spaceId
        getAskTopicsResult?.let { return it }
        return Result.success(sampleTopics.filter { it.spaceId == spaceId })
    }

    companion object {
        val sampleTopics = listOf(
            AskTopic("1a", "Core dissertation arguments", 4, 2, "1"),
            AskTopic("1b", "Turing and modern AI", 3, 1, "1"),
            AskTopic("2a", "Policy brief themes", 2, 2, "2"),
            AskTopic("3a", "Scientific manuscripts timeline", 1, 1, "3"),
            AskTopic("4a", "Week 7 lecture prep", 2, 1, "4"),
        )
    }
}
