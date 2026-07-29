package com.nus.folio.domain.model

data class AuthSession(
    val email: String,
    val displayName: String = email.substringBefore("@"),
)
