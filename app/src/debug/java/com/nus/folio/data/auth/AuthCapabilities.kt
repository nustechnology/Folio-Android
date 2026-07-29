package com.nus.folio.data.auth

/**
 * Debug builds expose credential-gated mock auth for UI development.
 */
object AuthCapabilities {
    const val isBackendAvailable: Boolean = true
    const val defaultLoginEmail: String = "researcher@folio.app"
    const val defaultLoginPassword: String = "folio-debug"
}
    