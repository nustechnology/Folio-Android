package com.nus.folio.domain.model

data class AskTopic(
    val id: String,
    val title: String,
    val sourceCount: Int,
    val noteCount: Int,
    val spaceId: String,
)
