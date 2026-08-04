package com.nus.folio.domain.model

import java.io.IOException

/**
 * Thrown when an auth API call receives a non-success HTTP response.
 * [message] is the server-supplied error text when present, otherwise a short client fallback.
 */
class AuthApiException(
    message: String,
) : IOException(message)
