package com.nus.folio.data.network

import java.io.IOException

/** Thrown when GET notebook receives HTTP 404. */
class NotebookNotFoundException(
    message: String,
) : IOException(message)
