package com.nus.folio.domain.model

data class AuthSession(
    val email: String,
    val displayName: String = email.substringBefore("@"),
    val userId: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)
