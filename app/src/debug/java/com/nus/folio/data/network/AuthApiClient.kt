package com.nus.folio.data.network

import com.nus.folio.domain.model.AuthApiException
import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

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
 * Thin HTTP client for Folio auth endpoints (debug builds against the ngrok/dev backend).
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
        path = "/api/v1/auth/sign-up",
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
        path = "/api/v1/auth/login",
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
        path = "/api/v1/auth/refresh",
        body = JSONObject().put("refreshToken", refreshToken),
        fallbackEmail = "",
        fallbackName = "",
        failureLabel = "Refresh",
    )

    override suspend fun logout(accessToken: String?): Unit = withContext(Dispatchers.IO) {
        val connection = (URL("$baseUrl/api/v1/auth/logout").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doInput = true
            doOutput = true
            setFixedLengthStreamingMode(0)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("ngrok-skip-browser-warning", "true")
            if (!accessToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
        }

        try {
            connection.outputStream.use { /* empty body */ }

            val code = connection.responseCode
            val responseBody = readBody(
                if (code in 200..299) connection.inputStream else connection.errorStream,
            )
            if (code !in 200..299) {
                throw AuthApiException(parseErrorMessage(responseBody, code, "Logout"))
            }
        } finally {
            connection.disconnect()
        }
    }

    override suspend fun getUser(
        id: String,
        accessToken: String,
    ): UserProfile = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(id, Charsets.UTF_8.name())
        val connection = (URL("$baseUrl/api/v1/users/$encodedId").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doInput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("ngrok-skip-browser-warning", "true")
            setRequestProperty("Authorization", "Bearer $accessToken")
        }

        try {
            val code = connection.responseCode
            val responseBody = readBody(
                if (code in 200..299) connection.inputStream else connection.errorStream,
            )
            if (code !in 200..299) {
                val message = parseErrorMessage(responseBody, code, "Get user")
                throw if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    UnauthorizedException(message)
                } else {
                    AuthApiException(message)
                }
            }
            parseUserProfile(responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun postAuth(
        path: String,
        body: JSONObject,
        fallbackEmail: String,
        fallbackName: String,
        failureLabel: String,
    ): AuthSession = withContext(Dispatchers.IO) {
        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doInput = true
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("ngrok-skip-browser-warning", "true")
        }

        try {
            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            val responseBody = readBody(
                if (code in 200..299) connection.inputStream else connection.errorStream,
            )

            if (code !in 200..299) {
                throw AuthApiException(parseErrorMessage(responseBody, code, failureLabel))
            }

            parseAuthSession(
                responseBody = responseBody,
                fallbackName = fallbackName,
                fallbackEmail = fallbackEmail,
                failureLabel = failureLabel,
            )
        } finally {
            connection.disconnect()
        }
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

    private fun parseErrorMessage(
        responseBody: String,
        code: Int,
        failureLabel: String,
    ): String {
        val fallback = "$failureLabel failed (HTTP $code)"
        if (responseBody.isBlank()) return fallback
        return runCatching {
            val json = JSONObject(responseBody)
            listOf("message", "detail", "error")
                .map { key ->
                    when (val value = json.opt(key)) {
                        is String -> value
                        is org.json.JSONArray -> (0 until value.length())
                            .mapNotNull { index -> value.optString(index).takeIf { it.isNotBlank() } }
                            .joinToString("; ")
                        else -> ""
                    }
                }
                .firstOrNull { it.isNotBlank() }
                ?: fallback
        }.getOrDefault(fallback)
    }

    private fun readBody(stream: InputStream?): String {
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://shale-crowd-satin.ngrok-free.dev"
        private const val TIMEOUT_MS = 15_000
    }
}
