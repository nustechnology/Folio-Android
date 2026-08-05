package com.nus.folio.domain.model

import java.io.IOException

/**
 * Thrown when an auth API call receives a non-success HTTP response.
 * [message] is the server-supplied error text when present, otherwise a short client fallback.
 * [statusCode] is the HTTP status when known.
 */
class AuthApiException(
    message: String,
    val statusCode: Int? = null,
) : IOException(message) {
    fun indicatesEmailAlreadyExists(): Boolean {
        if (statusCode == HTTP_CONFLICT) return true
        val normalized = message.orEmpty().lowercase()
        return normalized.contains("already exists") ||
            normalized.contains("already registered") ||
            normalized.contains("email already") ||
            (normalized.contains("exist") && normalized.contains("email"))
    }

    private companion object {
        const val HTTP_CONFLICT = 409
    }
}
