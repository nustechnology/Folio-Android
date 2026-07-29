package com.nus.folio.domain.repository

import com.nus.folio.domain.model.AskTopic

interface AskRepository {
    suspend fun getAskTopics(): Result<List<AskTopic>>
}
