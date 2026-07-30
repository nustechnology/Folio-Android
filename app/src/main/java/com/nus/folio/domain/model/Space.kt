package com.nus.folio.domain.model

data class Space(
    val id: String,
    val title: String,
    val description: String,
    val sourceCount: Int,
    val noteCount: Int,
    val updatedLabel: String,
)
