package com.nus.folio.domain.model

data class Notebook(
    val spaceId: String,
    val content: String,
    val updatedAtMillis: Long = 0L,
)
