package com.nus.folio.data.network

import java.io.IOException

/** Thrown when an authenticated API call receives HTTP 401. */
class UnauthorizedException(
    message: String,
) : IOException(message)
