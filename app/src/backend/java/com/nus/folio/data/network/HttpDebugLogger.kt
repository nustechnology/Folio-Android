package com.nus.folio.data.network

import android.util.Log
import com.nus.folio.BuildConfig

/**
 * Debug-only HTTP request/response logger for Folio API clients.
 * Filter Logcat by tag [TAG] (`FolioHttp`).
 *
 * No-ops in release (`BuildConfig.DEBUG == false`). Sensitive fields
 * (passwords, tokens, Authorization) are redacted when logging is enabled.
 */
internal object HttpDebugLogger {
    const val TAG = "FolioHttp"

    private const val MAX_BODY_CHARS = 4_000
    private const val REDACTED = "***"

    /**
     * Test override for [isEnabled]. `null` means use [BuildConfig.DEBUG].
     */
    @Volatile
    internal var enabledOverride: Boolean? = null

    internal val isEnabled: Boolean
        get() = enabledOverride ?: BuildConfig.DEBUG

    fun logRequest(
        method: String,
        url: String,
        body: String? = null,
        contentType: String? = null,
    ) {
        if (!isEnabled) return
        val meta = buildString {
            append("→ ")
            append(method)
            append(' ')
            append(url)
            if (!contentType.isNullOrBlank()) {
                append(" [")
                append(contentType)
                append(']')
            }
        }
        Log.d(TAG, meta)
        if (!body.isNullOrBlank()) {
            Log.d(TAG, "  request body: ${formatBody(body)}")
        }
    }

    fun logResponse(
        method: String,
        url: String,
        code: Int,
        body: String?,
    ) {
        if (!isEnabled) return
        Log.d(TAG, "← $method $url → HTTP $code")
        if (!body.isNullOrBlank()) {
            Log.d(TAG, "  response body: ${formatBody(body)}")
        }
    }

    fun logError(
        method: String,
        url: String,
        error: Throwable,
    ) {
        if (!isEnabled) return
        Log.e(TAG, "✖ $method $url → ${error.javaClass.simpleName}: ${error.message}", error)
    }

    fun logEvent(message: String) {
        if (!isEnabled) return
        Log.d(TAG, message)
    }

    fun formatBody(raw: String): String {
        val redacted = redactSensitive(raw.trim())
        return if (redacted.length <= MAX_BODY_CHARS) {
            redacted
        } else {
            redacted.take(MAX_BODY_CHARS) + "…[truncated ${redacted.length - MAX_BODY_CHARS} chars]"
        }
    }

    private fun redactSensitive(raw: String): String {
        if (raw.isEmpty()) return raw
        var result = SensitiveJsonStringRegex.replace(raw) { match ->
            """"${match.groupValues[1]}":"$REDACTED""""
        }
        result = BearerRegex.replace(result) { match ->
            "${match.groupValues[1]}$REDACTED"
        }
        return result
    }

    private val SensitiveJsonStringRegex = Regex(
        pattern = """"(password|confirmPassword|confirm_password|accessToken|access_token|refreshToken|refresh_token|token|authorization)"\s*:\s*"(?:\\.|[^"\\])*"""",
        option = RegexOption.IGNORE_CASE,
    )

    private val BearerRegex = Regex("""(Bearer\s+)\S+""", RegexOption.IGNORE_CASE)
}
