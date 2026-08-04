package com.nus.folio.data.network

import com.nus.folio.domain.model.Space
import com.nus.folio.domain.model.SpacePage
import com.nus.folio.domain.model.SpacePaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

interface SpacesApi {
    suspend fun listSpaces(
        accessToken: String,
        sort: String = DEFAULT_SORT,
        search: String? = null,
        page: Int = SpacePaging.DEFAULT_PAGE,
        limit: Int = SpacePaging.DEFAULT_LIMIT,
    ): SpacePage

    suspend fun createSpace(
        accessToken: String,
        name: String,
        researchObjective: String,
    ): Space

    companion object {
        const val DEFAULT_SORT = "recently-updated"
    }
}

/**
 * Thin HTTP client for Folio spaces endpoints (debug builds against the ngrok/dev backend).
 */
class SpacesApiClient(
    private val baseUrl: String = AuthApiClient.DEFAULT_BASE_URL,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) : SpacesApi {

    override suspend fun listSpaces(
        accessToken: String,
        sort: String,
        search: String?,
        page: Int,
        limit: Int,
    ): SpacePage = withContext(Dispatchers.IO) {
        val query = buildString {
            append("sort=")
            append(URLEncoder.encode(sort, Charsets.UTF_8.name()))
            append("&page=")
            append(page.coerceAtLeast(1))
            append("&limit=")
            append(limit.coerceAtLeast(1))
            val trimmedSearch = search?.trim().orEmpty()
            if (trimmedSearch.isNotEmpty()) {
                append("&search=")
                append(URLEncoder.encode(trimmedSearch, Charsets.UTF_8.name()))
            }
        }
        val connection = (URL("$baseUrl/api/v1/spaces?$query").openConnection() as HttpURLConnection).apply {
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
                throw httpException(responseBody, code, "Get spaces")
            }
            parseSpacesPage(responseBody, page = page.coerceAtLeast(1), limit = limit.coerceAtLeast(1))
        } finally {
            connection.disconnect()
        }
    }

    override suspend fun createSpace(
        accessToken: String,
        name: String,
        researchObjective: String,
    ): Space = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("name", name)
            .put("researchObjective", researchObjective)
        val connection = (URL("$baseUrl/api/v1/spaces").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doInput = true
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("ngrok-skip-browser-warning", "true")
            setRequestProperty("Authorization", "Bearer $accessToken")
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
                throw httpException(responseBody, code, "Create space")
            }
            parseCreatedSpace(responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseCreatedSpace(responseBody: String): Space {
        if (responseBody.isBlank()) {
            throw IOException("Create space failed: empty response")
        }
        val root = JSONObject(responseBody)
        val spaceJson = root.optJSONObject("data")?.optJSONObject("space")
            ?: root.optJSONObject("space")
            ?: throw IOException("Create space failed: missing space payload")
        return parseSpace(spaceJson)
    }

    private fun parseSpacesPage(
        responseBody: String,
        page: Int,
        limit: Int,
    ): SpacePage {
        if (responseBody.isBlank()) {
            return SpacePage(
                spaces = emptyList(),
                page = page,
                limit = limit,
                hasMore = false,
            )
        }
        val root = JSONObject(responseBody)
        val data = root.optJSONObject("data")
        val spacesArray = data?.optJSONArray("spaces")
            ?: root.optJSONArray("spaces")
            ?: JSONArray()

        val spaces = buildList {
            for (index in 0 until spacesArray.length()) {
                val item = spacesArray.optJSONObject(index) ?: continue
                add(parseSpace(item))
            }
        }

        val hasMore = parseHasMore(root, data) ?: (spaces.size >= limit)
        return SpacePage(
            spaces = spaces,
            page = page,
            limit = limit,
            hasMore = hasMore,
        )
    }

    private fun parseHasMore(root: JSONObject, data: JSONObject?): Boolean? {
        fun JSONObject.optHasMore(): Boolean? {
            if (has("hasMore")) return optBoolean("hasMore")
            optJSONObject("pagination")?.let { pagination ->
                if (pagination.has("hasMore")) return pagination.optBoolean("hasMore")
            }
            optJSONObject("meta")?.let { meta ->
                if (meta.has("hasMore")) return meta.optBoolean("hasMore")
            }
            return null
        }
        return data?.optHasMore() ?: root.optHasMore()
    }

    private fun parseSpace(json: JSONObject): Space {
        val id = json.optString("id").takeIf { it.isNotBlank() }
            ?: throw IOException("Space payload missing id")
        val title = sequenceOf("name", "title")
            .map { json.optString(it) }
            .firstOrNull { it.isNotBlank() }
            ?: "Untitled space"
        val description = sequenceOf("researchObjective", "description", "objective")
            .map { json.optString(it) }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        val updatedAt = sequenceOf("updatedAt", "lastOpenedAt", "createdAt")
            .map { json.optString(it) }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()

        return Space(
            id = id,
            title = title,
            description = description,
            sourceCount = json.optInt("sourceCount", 0),
            noteCount = json.optInt("noteCount", 0),
            updatedLabel = formatUpdatedLabel(updatedAt, nowMillis()),
        )
    }

    private fun httpException(
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
                        is JSONArray -> (0 until value.length())
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
        private const val TIMEOUT_MS = 15_000

        internal fun formatUpdatedLabel(isoInstant: String, nowMillis: Long): String {
            val millis = parseIsoToMillis(isoInstant) ?: return "Updated recently"
            val delta = (nowMillis - millis).coerceAtLeast(0L)
            val minutes = delta / 60_000L
            val hours = delta / 3_600_000L
            val days = delta / 86_400_000L
            return when {
                minutes < 1L -> "Updated just now"
                minutes < 60L -> "Updated ${minutes}m ago"
                hours < 24L -> "Updated ${hours}h ago"
                days < 7L -> "Updated ${days}d ago"
                else -> "Updated ${days / 7L}w ago"
            }
        }

        private fun parseIsoToMillis(isoInstant: String): Long? {
            val value = normalizeIsoTimestamp(isoInstant.trim())
            if (value.isEmpty()) return null
            val patterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
            )
            for (pattern in patterns) {
                val parsed = runCatching {
                    SimpleDateFormat(pattern, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                        isLenient = false
                    }.parse(value)?.time
                }.getOrNull()
                if (parsed != null) return parsed
            }
            return null
        }

        /**
         * SimpleDateFormat only accepts millisecond precision (SSS). Truncate or pad
         * fractional seconds so micro/nano timestamps still parse on minSdk 24.
         */
        internal fun normalizeIsoTimestamp(isoInstant: String): String {
            val value = isoInstant.trim()
            val dotIndex = value.indexOf('.')
            if (dotIndex < 0) return value

            var end = dotIndex + 1
            while (end < value.length && value[end].isDigit()) {
                end++
            }
            if (end == dotIndex + 1) return value

            val millis = value.substring(dotIndex + 1, end).padEnd(3, '0').take(3)
            return value.substring(0, dotIndex + 1) + millis + value.substring(end)
        }
    }
}
