package com.nus.folio.data.auth

/**
 * Release builds have no auth backend yet. Login UI must not pretend sign-in works.
 * Wire [com.nus.folio.data.datasource.AuthDataSource] to a real provider before enabling this.
 */
object AuthCapabilities {
    const val isBackendAvailable: Boolean = false
}
