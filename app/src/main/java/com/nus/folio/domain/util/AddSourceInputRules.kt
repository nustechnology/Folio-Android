package com.nus.folio.domain.util

import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Client-side rules for the Add Source form (Files / Web / Manual Text).
 */
object AddSourceInputRules {
    const val MAX_FILE_BYTES = 50L * 1024 * 1024
    const val MAX_TITLE_LENGTH = 255
    const val MAX_AUTHOR_LENGTH = 100
    const val MIN_CONTENT_LENGTH = 10
    const val MAX_CONTENT_LENGTH = 100_000

    val SUPPORTED_EXTENSIONS = setOf(
        "pdf",
        "docx",
        "txt",
        "md",
        "pptx",
        "xlsx",
        "csv",
        "epub",
    )

    enum class FileValidationError {
        UNSUPPORTED_FORMAT,
        SIZE_EXCEEDED,
    }

    enum class ContentValidationError {
        TOO_SHORT,
        TOO_LONG,
    }

    fun extensionOf(fileName: String): String =
        fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()

    fun isSupportedExtension(fileName: String): Boolean =
        extensionOf(fileName) in SUPPORTED_EXTENSIONS

    fun validateFile(fileName: String, sizeBytes: Long?): FileValidationError? {
        if (!isSupportedExtension(fileName)) return FileValidationError.UNSUPPORTED_FORMAT
        if (sizeBytes != null && sizeBytes > MAX_FILE_BYTES) return FileValidationError.SIZE_EXCEEDED
        return null
    }

    fun isValidHttpUrl(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false
        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase()
            (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }

    fun isContentValid(content: String): Boolean =
        content.length in MIN_CONTENT_LENGTH..MAX_CONTENT_LENGTH

    fun contentValidationError(content: String): ContentValidationError? = when {
        content.length > MAX_CONTENT_LENGTH -> ContentValidationError.TOO_LONG
        content.length < MIN_CONTENT_LENGTH -> ContentValidationError.TOO_SHORT
        else -> null
    }

    fun limitTitle(value: String): String = value.take(MAX_TITLE_LENGTH)

    fun limitAuthor(value: String): String = value.take(MAX_AUTHOR_LENGTH)

    fun defaultManualTitle(date: String = todayIsoDate()): String =
        "Untitled Source - $date"

    /** ISO-8601 calendar date (`yyyy-MM-dd`), API-24-safe (no `java.time`). */
    private fun todayIsoDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun defaultWebAuthor(url: String): String {
        val host = try {
            URI(url.trim()).host
        } catch (_: Exception) {
            null
        }
        return host
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }
            ?: "Unknown Author"
    }

    fun currentUserDisplayName(displayName: String?, email: String?): String =
        displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@")?.trim()?.takeIf { it.isNotBlank() }
            ?: "Unknown Author"
}
