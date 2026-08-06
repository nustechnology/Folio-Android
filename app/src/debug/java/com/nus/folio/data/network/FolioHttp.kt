package com.nus.folio.data.network

import com.nus.folio.domain.model.AuthApiException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Shared HTTP connection helper for debug Folio API clients.
 * Handles open / headers / logging / read / disconnect; callers own URL building and parsing.
 */
internal object FolioHttp {

    const val DEFAULT_TIMEOUT_MS = 15_000
    const val LONG_TIMEOUT_MS = 60_000

    private const val CREDENTIALS_REQUIRE_HTTPS_MESSAGE =
        "Credentialed requests must use HTTPS"

    data class Response(
        val code: Int,
        val body: String,
    )

    fun jsonAcceptHeaders(accessToken: String? = null): Map<String, String> = buildMap {
        put("Accept", "application/json")
        put("ngrok-skip-browser-warning", "true")
        if (!accessToken.isNullOrBlank()) {
            put("Authorization", "Bearer $accessToken")
        }
    }

    fun jsonContentHeaders(accessToken: String? = null): Map<String, String> =
        jsonAcceptHeaders(accessToken) + ("Content-Type" to "application/json")

    /**
     * One-shot request/response cycle with debug logging and disconnect.
     *
     * @param writeBody when non-null, enables output and writes the request body
     * @param logBody optional body text for [HttpDebugLogger] (may differ from bytes written)
     * @param requireSuccess when true, non-2xx responses throw via [mapError] before [parse]
     */
    fun <T> execute(
        method: String,
        url: String,
        headers: Map<String, String> = jsonAcceptHeaders(),
        connectTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
        readTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
        writeBody: (OutputStream.() -> Unit)? = null,
        logBody: String? = null,
        logContentType: String? = null,
        failureLabel: String,
        mapError: (body: String, code: Int, label: String) -> Throwable = ::unauthorizedOrIo,
        requireSuccess: Boolean = true,
        parse: (Response) -> T,
    ): T {
        val connection = open(
            method = method,
            url = url,
            headers = headers,
            connectTimeoutMs = connectTimeoutMs,
            readTimeoutMs = readTimeoutMs,
            doOutput = writeBody != null,
        )

        var responseLogged = false
        try {
            HttpDebugLogger.logRequest(
                method = method,
                url = url,
                body = logBody,
                contentType = logContentType,
            )
            if (writeBody != null) {
                connection.outputStream.use { output -> output.writeBody() }
            }

            val code = connection.responseCode
            val body = readBody(
                if (code in 200..299) connection.inputStream else connection.errorStream,
            )
            HttpDebugLogger.logResponse(method = method, url = url, code = code, body = body)
            responseLogged = true

            val response = Response(code = code, body = body)
            if (requireSuccess && code !in 200..299) {
                throw mapError(body, code, failureLabel)
            }
            return parse(response)
        } catch (error: Throwable) {
            if (!responseLogged) {
                HttpDebugLogger.logError(method = method, url = url, error = error)
            }
            throw error
        } finally {
            connection.disconnect()
        }
    }

    fun <T> get(
        url: String,
        accessToken: String? = null,
        connectTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
        readTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
        failureLabel: String,
        mapError: (body: String, code: Int, label: String) -> Throwable = ::unauthorizedOrIo,
        requireSuccess: Boolean = true,
        parse: (Response) -> T,
    ): T = execute(
        method = "GET",
        url = url,
        headers = jsonAcceptHeaders(accessToken),
        connectTimeoutMs = connectTimeoutMs,
        readTimeoutMs = readTimeoutMs,
        failureLabel = failureLabel,
        mapError = mapError,
        requireSuccess = requireSuccess,
        parse = parse,
    )

    fun <T> postJson(
        url: String,
        jsonBody: String,
        accessToken: String? = null,
        connectTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
        readTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
        failureLabel: String,
        mapError: (body: String, code: Int, label: String) -> Throwable = ::unauthorizedOrIo,
        parse: (Response) -> T,
    ): T = execute(
        method = "POST",
        url = url,
        headers = jsonContentHeaders(accessToken),
        connectTimeoutMs = connectTimeoutMs,
        readTimeoutMs = readTimeoutMs,
        writeBody = { write(jsonBody.toByteArray(Charsets.UTF_8)) },
        logBody = jsonBody,
        logContentType = "application/json",
        failureLabel = failureLabel,
        mapError = mapError,
        parse = parse,
    )

    /**
     * JSON update with wire method PATCH.
     *
     * [HttpURLConnection.setRequestMethod] rejects `"PATCH"` on some API levels;
     * [open] applies a reflection fallback so the request still goes out as PATCH.
     */
    fun <T> patchJson(
        url: String,
        jsonBody: String,
        accessToken: String? = null,
        connectTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
        readTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
        failureLabel: String,
        mapError: (body: String, code: Int, label: String) -> Throwable = ::unauthorizedOrIo,
        parse: (Response) -> T,
    ): T = execute(
        method = "PATCH",
        url = url,
        headers = jsonContentHeaders(accessToken),
        connectTimeoutMs = connectTimeoutMs,
        readTimeoutMs = readTimeoutMs,
        writeBody = { write(jsonBody.toByteArray(Charsets.UTF_8)) },
        logBody = jsonBody,
        logContentType = "application/json",
        failureLabel = failureLabel,
        mapError = mapError,
        parse = parse,
    )

    fun <T> postEmpty(
        url: String,
        accessToken: String? = null,
        connectTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
        readTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
        fixedLengthZero: Boolean = false,
        failureLabel: String,
        mapError: (body: String, code: Int, label: String) -> Throwable = ::unauthorizedOrIo,
        parse: (Response) -> T,
    ): T {
        val headers = jsonAcceptHeaders(accessToken)
        val connection = open(
            method = "POST",
            url = url,
            headers = headers,
            connectTimeoutMs = connectTimeoutMs,
            readTimeoutMs = readTimeoutMs,
            doOutput = true,
        )
        if (fixedLengthZero) {
            connection.setFixedLengthStreamingMode(0)
        }

        var responseLogged = false
        try {
            HttpDebugLogger.logRequest(method = "POST", url = url, body = null)
            connection.outputStream.use { /* empty body */ }

            val code = connection.responseCode
            val body = readBody(
                if (code in 200..299) connection.inputStream else connection.errorStream,
            )
            HttpDebugLogger.logResponse(method = "POST", url = url, code = code, body = body)
            responseLogged = true

            val response = Response(code = code, body = body)
            if (code !in 200..299) {
                throw mapError(body, code, failureLabel)
            }
            return parse(response)
        } catch (error: Throwable) {
            if (!responseLogged) {
                HttpDebugLogger.logError(method = "POST", url = url, error = error)
            }
            throw error
        } finally {
            connection.disconnect()
        }
    }

    fun delete(
        url: String,
        accessToken: String,
        connectTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
        readTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
        failureLabel: String,
        mapError: (body: String, code: Int, label: String) -> Throwable = ::unauthorizedOrIo,
    ) {
        execute<Unit>(
            method = "DELETE",
            url = url,
            headers = jsonAcceptHeaders(accessToken),
            connectTimeoutMs = connectTimeoutMs,
            readTimeoutMs = readTimeoutMs,
            failureLabel = failureLabel,
            mapError = mapError,
            parse = { },
        )
    }

    /** Open a configured connection for callers that need streaming (multipart / SSE). */
    fun open(
        method: String,
        url: String,
        headers: Map<String, String>,
        connectTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
        readTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
        doOutput: Boolean = false,
    ): HttpURLConnection {
        requireHttpsForCredentialedRequest(url, headers)
        return (URL(url).openConnection() as HttpURLConnection).apply {
            setRequestMethodCompat(method)
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            doInput = true
            this.doOutput = doOutput
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
        }
    }

    /**
     * Sets the HTTP method, including PATCH which [HttpURLConnection.setRequestMethod]
     * rejects on older Android runtimes (ProtocolException on API 24).
     */
    internal fun HttpURLConnection.setRequestMethodCompat(method: String) {
        val normalized = method.uppercase(Locale.US)
        try {
            requestMethod = normalized
            return
        } catch (_: java.net.ProtocolException) {
            // Fall through to reflection for PATCH / other non-enumerated methods.
        }
        forceRequestMethod(this, normalized)
    }

    private fun forceRequestMethod(connection: HttpURLConnection, method: String) {
        val targets = mutableListOf<Any>(connection)
        try {
            val delegateField = connection.javaClass.getDeclaredField("delegate")
            delegateField.isAccessible = true
            delegateField.get(connection)?.let { targets += it }
        } catch (_: ReflectiveOperationException) {
            // JDK modules / non-HTTPS wrappers may block delegate access.
        } catch (_: RuntimeException) {
            // InaccessibleObjectException / SecurityException without --add-opens.
        }
        var applied = false
        for (target in targets) {
            var type: Class<*>? = target.javaClass
            while (type != null) {
                try {
                    val methodField = type.getDeclaredField("method")
                    methodField.isAccessible = true
                    methodField.set(target, method)
                    applied = true
                    break
                } catch (_: ReflectiveOperationException) {
                    type = type.superclass
                } catch (_: RuntimeException) {
                    // InaccessibleObjectException / SecurityException without --add-opens.
                    type = type.superclass
                }
            }
        }
        if (!applied) {
            throw java.net.ProtocolException("Unsupported HTTP method: $method")
        }
    }

    /**
     * Rejects cleartext HTTP before a connection is opened (CWE-319).
     * Use for Authorization-bearing calls and password / refresh-token bodies.
     */
    internal fun requireHttps(url: String) {
        val scheme = URL(url).protocol.lowercase(Locale.US)
        if (scheme != "https") {
            throw IOException(CREDENTIALS_REQUIRE_HTTPS_MESSAGE)
        }
    }

    /**
     * Rejects credentialed requests over cleartext HTTP before headers are applied.
     * Callers may pass custom base URLs; [Authorization] must not travel over non-HTTPS.
     */
    internal fun requireHttpsForCredentialedRequest(url: String, headers: Map<String, String>) {
        val hasCredentials = headers.any { (key, value) ->
            key.equals("Authorization", ignoreCase = true) && value.isNotBlank()
        }
        if (!hasCredentials) return
        requireHttps(url)
    }

    fun readBody(stream: InputStream?): String {
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }

    fun parseErrorMessage(
        responseBody: String,
        code: Int,
        failureLabel: String,
    ): String {
        val fallback = "$failureLabel failed (HTTP $code)"
        if (responseBody.isBlank()) return fallback
        for (key in listOf("message", "detail", "error")) {
            matchJsonString(responseBody, key)?.takeIf { it.isNotBlank() }?.let { return it }
        }
        // Prefer joined string array errors when present: "error":["a","b"]
        matchJsonStringArray(responseBody, "error")?.takeIf { it.isNotBlank() }?.let { return it }
        matchJsonStringArray(responseBody, "detail")?.takeIf { it.isNotBlank() }?.let { return it }
        return fallback
    }

    private fun matchJsonString(payload: String, key: String): String? {
        val regex = Regex("\"$key\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        return regex.find(payload)?.groupValues?.getOrNull(1)
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
            ?.takeIf { it.isNotBlank() }
    }

    private fun matchJsonStringArray(payload: String, key: String): String? {
        val regex = Regex("\"$key\"\\s*:\\s*\\[(.*?)]", RegexOption.DOT_MATCHES_ALL)
        val arrayBody = regex.find(payload)?.groupValues?.getOrNull(1) ?: return null
        val values = Regex("\"((?:\\\\.|[^\"\\\\])*)\"")
            .findAll(arrayBody)
            .map { it.groupValues[1] }
            .filter { it.isNotBlank() }
            .toList()
        return values.takeIf { it.isNotEmpty() }?.joinToString("; ")
    }

    fun unauthorizedOrIo(
        responseBody: String,
        code: Int,
        failureLabel: String,
    ): IOException {
        val message = parseErrorMessage(responseBody, code, failureLabel)
        return if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            UnauthorizedException(message)
        } else {
            IOException(message)
        }
    }

    fun authApiError(
        responseBody: String,
        code: Int,
        failureLabel: String,
    ): AuthApiException =
        AuthApiException(
            message = parseErrorMessage(responseBody, code, failureLabel),
            statusCode = code,
        )

    fun unauthorizedOrAuth(
        responseBody: String,
        code: Int,
        failureLabel: String,
    ): Exception {
        val message = parseErrorMessage(responseBody, code, failureLabel)
        return if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            UnauthorizedException(message)
        } else {
            AuthApiException(message = message, statusCode = code)
        }
    }
}
