package com.nus.folio.data.network

import java.net.URLEncoder

/**
 * Central Folio API path and URL builders for debug network clients.
 */
internal object FolioApiPaths {

    const val BASE_URL = "https://shale-crowd-satin.ngrok-free.dev"

    // Auth
    const val AUTH_SIGN_UP = "/api/v1/auth/sign-up"
    const val AUTH_LOGIN = "/api/v1/auth/login"
    const val AUTH_REFRESH = "/api/v1/auth/refresh"
    const val AUTH_LOGOUT = "/api/v1/auth/logout"

    // Users
    const val USERS = "/api/v1/users"

    // Spaces
    const val SPACES = "/api/v1/spaces"

    // Sources
    const val SOURCES = "/api/v1/sources"
    const val SOURCES_STATUS = "/api/v1/sources/status"

    fun url(baseUrl: String, path: String): String = "$baseUrl$path"

    fun authSignUp(baseUrl: String = BASE_URL): String = url(baseUrl, AUTH_SIGN_UP)

    fun authLogin(baseUrl: String = BASE_URL): String = url(baseUrl, AUTH_LOGIN)

    fun authRefresh(baseUrl: String = BASE_URL): String = url(baseUrl, AUTH_REFRESH)

    fun authLogout(baseUrl: String = BASE_URL): String = url(baseUrl, AUTH_LOGOUT)

    fun user(userId: String, baseUrl: String = BASE_URL): String =
        url(baseUrl, "$USERS/${encode(userId)}")

    fun spaces(baseUrl: String = BASE_URL, query: String? = null): String =
        urlWithQuery(baseUrl, SPACES, query)

    fun space(spaceId: String, baseUrl: String = BASE_URL): String =
        url(baseUrl, "$SPACES/${encode(spaceId)}")

    fun sources(baseUrl: String = BASE_URL, query: String? = null): String =
        urlWithQuery(baseUrl, SOURCES, query)

    fun source(sourceId: String, baseUrl: String = BASE_URL): String =
        url(baseUrl, "$SOURCES/${encode(sourceId)}")

    fun sourceRetry(sourceId: String, baseUrl: String = BASE_URL): String =
        url(baseUrl, "$SOURCES/${encode(sourceId)}/retry")

    fun sourcePreview(sourceId: String, baseUrl: String = BASE_URL): String =
        url(baseUrl, "$SOURCES/${encode(sourceId)}/preview")

    fun sourcesStatus(baseUrl: String = BASE_URL): String = url(baseUrl, SOURCES_STATUS)

    private fun urlWithQuery(baseUrl: String, path: String, query: String?): String {
        val trimmed = query?.trim().orEmpty()
        return if (trimmed.isEmpty()) {
            url(baseUrl, path)
        } else {
            url(baseUrl, "$path?$trimmed")
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())
}
