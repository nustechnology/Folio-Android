package com.nus.folio.data.network

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

interface AuthApi {
    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): AuthSession

    suspend fun login(
        email: String,
        password: String,
    ): AuthSession

    suspend fun refresh(
        refreshToken: String,
    ): AuthSession

    suspend fun logout(accessToken: String?)

    suspend fun getUser(
        id: String,
        accessToken: String,
    ): UserProfile
}

/**
 * Thin HTTP client for Folio auth endpoints.
 */
class AuthApiClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
) : AuthApi {

    override suspend fun signUp(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): AuthSession = postAuth(
        url = FolioApiPaths.authSignUp(baseUrl),
        body = JSONObject()
            .put("name", name)
            .put("email", email)
            .put("password", password)
            .put("confirmPassword", confirmPassword),
        fallbackEmail = email,
        fallbackName = name,
        failureLabel = "Sign-up",
    )

    override suspend fun login(
        email: String,
        password: String,
    ): AuthSession = postAuth(
        url = FolioApiPaths.authLogin(baseUrl),
        body = JSONObject()
            .put("email", email)
            .put("password", password),
        fallbackEmail = email,
        fallbackName = email.substringBefore("@"),
        failureLabel = "Login",
    )

    override suspend fun refresh(
        refreshToken: String,
    ): AuthSession = postAuth(
        url = FolioApiPaths.authRefresh(baseUrl),
        body = JSONObject().put("refreshToken", refreshToken),
        fallbackEmail = "",
        fallbackName = "",
        failureLabel = "Refresh",
    )

    override suspend fun logout(accessToken: String?): Unit = withContext(Dispatchers.IO) {
        FolioHttp.postEmpty(
            url = FolioApiPaths.authLogout(baseUrl),
            accessToken = accessToken,
            fixedLengthZero = true,
            failureLabel = "Logout",
            mapError = FolioHttp::authApiError,
            parse = { },
        )
    }

    override suspend fun getUser(
        id: String,
        accessToken: String,
    ): UserProfile = withContext(Dispatchers.IO) {
        FolioHttp.get(
            url = FolioApiPaths.user(id, baseUrl),
            accessToken = accessToken,
            failureLabel = "Get user",
            mapError = FolioHttp::unauthorizedOrAuth,
            parse = { response -> parseUserProfile(response.body) },
        )
    }

    private suspend fun postAuth(
        url: String,
        body: JSONObject,
        fallbackEmail: String,
        fallbackName: String,
        failureLabel: String,
    ): AuthSession = withContext(Dispatchers.IO) {
        // login / sign-up / refresh put secrets in the body without Authorization.
        FolioHttp.requireHttps(url)
        FolioHttp.postJson(
            url = url,
            jsonBody = body.toString(),
            failureLabel = failureLabel,
            mapError = FolioHttp::authApiError,
            parse = { response ->
                parseAuthSession(
                    responseBody = response.body,
                    fallbackName = fallbackName,
                    fallbackEmail = fallbackEmail,
                    failureLabel = failureLabel,
                )
            },
        )
    }

    private fun parseUserProfile(responseBody: String): UserProfile {
        val root = JSONObject(responseBody)
        val user = resolveUserObject(root)
            ?: throw IOException("Get user failed: missing user payload")

        val id = user.optString("id").takeIf { it.isNotBlank() }
            ?: throw IOException("Get user failed: missing user id")
        val email = user.optString("email").takeIf { it.isNotBlank() }
            ?: throw IOException("Get user failed: missing email")
        val name = sequenceOf("name", "displayName", "display_name")
            .map { user.optString(it) }
            .firstOrNull { it.isNotBlank() }
            ?: email.substringBefore("@")

        return UserProfile(id = id, name = name, email = email)
    }

    private fun parseAuthSession(
        responseBody: String,
        fallbackName: String,
        fallbackEmail: String,
        failureLabel: String,
    ): AuthSession {
        if (responseBody.isBlank()) {
            throw IOException("$failureLabel failed: missing access token")
        }

        val root = JSONObject(responseBody)
        val user = resolveUserObject(root) ?: root
        val email = sequenceOf(
            user.optString("email"),
            root.optString("email"),
        ).firstOrNull { it.isNotBlank() } ?: fallbackEmail
        val displayName = sequenceOf("name", "displayName", "display_name")
            .map { user.optString(it) }
            .firstOrNull { it.isNotBlank() }
            ?: fallbackName.ifBlank { email.substringBefore("@") }
        val userId = sequenceOf("id", "userId", "user_id")
            .map { user.optString(it) }
            .firstOrNull { it.isNotBlank() }
        val accessToken = readToken(root, "accessToken", "access_token")
            ?: throw IOException("$failureLabel failed: missing access token")

        return AuthSession(
            email = email,
            displayName = displayName,
            userId = userId,
            accessToken = accessToken,
            refreshToken = readToken(root, "refreshToken", "refresh_token"),
        )
    }

    private fun resolveUserObject(root: JSONObject): JSONObject? {
        root.optJSONObject("data")?.optJSONObject("user")?.let { return it }
        root.optJSONObject("user")?.let { return it }
        root.optJSONObject("data")?.let { return it }
        return null
    }

    private fun readToken(root: JSONObject, vararg keys: String): String? {
        val tokens = root.optJSONObject("tokens")
            ?: root.optJSONObject("data")?.optJSONObject("tokens")
        for (key in keys) {
            root.optString(key).takeIf { it.isNotBlank() }?.let { return it }
            root.optJSONObject("data")?.optString(key)?.takeIf { it.isNotBlank() }?.let { return it }
            tokens?.optString(key)?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    companion object {
        val DEFAULT_BASE_URL = FolioApiPaths.BASE_URL
    }
}
