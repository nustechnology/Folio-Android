package com.nus.folio.data.network

import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteLibrary
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.domain.model.NotePaging
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.util.NoteUpdatedLabelFormatter
import com.nus.folio.domain.util.NotebookHtml
import java.io.IOException
import java.net.URLEncoder
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface NotesApi {
    suspend fun listNotes(
        accessToken: String,
        spaceId: String,
        sort: String = DEFAULT_SORT,
        search: String? = null,
        origin: String? = null,
        page: Int = NotePaging.DEFAULT_PAGE,
        limit: Int = NotePaging.DEFAULT_LIMIT,
    ): NoteLibrary

    suspend fun getNote(
        accessToken: String,
        spaceId: String,
        noteId: String,
    ): Note

    /**
     * Creates a note. [content] is markdown; the client converts to HTML for the API.
     */
    suspend fun createNote(
        accessToken: String,
        spaceId: String,
        title: String,
        content: String,
        conversationId: String? = null,
        messageId: String? = null,
    ): Note

    /**
     * Updates a note. [content] is markdown; the client converts to HTML for the API.
     */
    suspend fun updateNote(
        accessToken: String,
        spaceId: String,
        noteId: String,
        title: String,
        content: String,
    ): Note

    suspend fun deleteNote(
        accessToken: String,
        spaceId: String,
        noteId: String,
    )

    suspend fun convertNoteToSource(
        accessToken: String,
        spaceId: String,
        noteId: String,
        title: String,
    ): Source

    companion object {
        const val DEFAULT_SORT = "recently-updated"
    }
}

/**
 * Thin HTTP client for Folio notes endpoints.
 */
class NotesApiClient(
    private val baseUrl: String = FolioApiPaths.BASE_URL,
    private val nowInstant: () -> Instant = { Instant.now() },
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) : NotesApi {

    override suspend fun listNotes(
        accessToken: String,
        spaceId: String,
        sort: String,
        search: String?,
        origin: String?,
        page: Int,
        limit: Int,
    ): NoteLibrary = withContext(Dispatchers.IO) {
        val safePage = page.coerceAtLeast(1)
        val safeLimit = limit.coerceAtLeast(1)
        val query = buildString {
            append("sort=")
            append(URLEncoder.encode(sort, Charsets.UTF_8.name()))
            append("&page=")
            append(safePage)
            append("&limit=")
            append(safeLimit)
            val trimmedSearch = search?.trim().orEmpty()
            if (trimmedSearch.isNotEmpty()) {
                append("&search=")
                append(URLEncoder.encode(trimmedSearch, Charsets.UTF_8.name()))
            }
            val trimmedOrigin = origin?.trim().orEmpty()
            if (trimmedOrigin.isNotEmpty()) {
                append("&origin=")
                append(URLEncoder.encode(trimmedOrigin, Charsets.UTF_8.name()))
            }
        }
        FolioHttp.get(
            url = FolioApiPaths.spaceNotes(spaceId, baseUrl, query),
            accessToken = accessToken,
            failureLabel = "Get notes",
            parse = { response ->
                parseNotesPage(
                    responseBody = response.body,
                    page = safePage,
                    limit = safeLimit,
                )
            },
        )
    }

    override suspend fun getNote(
        accessToken: String,
        spaceId: String,
        noteId: String,
    ): Note = withContext(Dispatchers.IO) {
        FolioHttp.get(
            url = FolioApiPaths.spaceNote(spaceId, noteId, baseUrl),
            accessToken = accessToken,
            failureLabel = "Get note",
            parse = { response -> parseNotePayload(response.body, failureLabel = "Get note") },
        )
    }

    override suspend fun createNote(
        accessToken: String,
        spaceId: String,
        title: String,
        content: String,
        conversationId: String?,
        messageId: String?,
    ): Note = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("title", title)
            .put("content", contentToApi(content))
        val conversation = conversationId?.trim().orEmpty()
        val message = messageId?.trim().orEmpty()
        if (conversation.isNotEmpty() && message.isNotEmpty()) {
            body.put(
                "origin",
                JSONObject()
                    .put("conversationId", conversation)
                    .put("messageId", message),
            )
        }
        FolioHttp.postJson(
            url = FolioApiPaths.spaceNotes(spaceId, baseUrl),
            jsonBody = body.toString(),
            accessToken = accessToken,
            failureLabel = "Create note",
            parse = { response -> parseNotePayload(response.body, failureLabel = "Create note") },
        )
    }

    override suspend fun updateNote(
        accessToken: String,
        spaceId: String,
        noteId: String,
        title: String,
        content: String,
    ): Note = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("title", title)
            .put("content", contentToApi(content))
        FolioHttp.patchJson(
            url = FolioApiPaths.spaceNote(spaceId, noteId, baseUrl),
            jsonBody = body.toString(),
            accessToken = accessToken,
            failureLabel = "Update note",
            parse = { response ->
                if (response.body.isBlank()) {
                    Note(
                        id = noteId,
                        title = title,
                        content = content,
                        project = null,
                        updatedLabel = formatUpdatedLabel("", nowInstant()),
                        isPinned = false,
                        spaceId = spaceId,
                    )
                } else {
                    parseNotePayload(response.body, failureLabel = "Update note")
                }
            },
        )
    }

    override suspend fun deleteNote(
        accessToken: String,
        spaceId: String,
        noteId: String,
    ): Unit = withContext(Dispatchers.IO) {
        FolioHttp.delete(
            url = FolioApiPaths.spaceNote(spaceId, noteId, baseUrl),
            accessToken = accessToken,
            failureLabel = "Delete note",
        )
    }

    override suspend fun convertNoteToSource(
        accessToken: String,
        spaceId: String,
        noteId: String,
        title: String,
    ): Source = withContext(Dispatchers.IO) {
        val body = JSONObject().put("title", title)
        FolioHttp.postJson(
            url = FolioApiPaths.spaceNoteConvertToSource(spaceId, noteId, baseUrl),
            jsonBody = body.toString(),
            accessToken = accessToken,
            failureLabel = "Convert note to source",
            parse = { response ->
                SourcesJsonParsers.parseCreatedSource(response.body, nowMillis())
            },
        )
    }

    private fun parseNotePayload(responseBody: String, failureLabel: String): Note {
        if (responseBody.isBlank()) {
            throw IOException("$failureLabel failed: empty response")
        }
        val root = JSONObject(responseBody)
        val noteJson = root.optJSONObject("data")?.optJSONObject("note")
            ?: root.optJSONObject("note")
            ?: throw IOException("$failureLabel failed: missing note payload")
        return parseNote(noteJson, nowInstant())
    }

    private fun parseNotesPage(
        responseBody: String,
        page: Int,
        limit: Int,
    ): NoteLibrary {
        if (responseBody.isBlank()) {
            return NoteLibrary(
                notes = emptyList(),
                allCount = 0,
                userCreatedCount = 0,
                savedAnswerCount = 0,
                page = page,
                limit = limit,
                hasMore = false,
            )
        }
        val root = JSONObject(responseBody)
        val data = root.optJSONObject("data")
        val notesArray = data?.optJSONArray("notes")
            ?: root.optJSONArray("notes")
            ?: JSONArray()

        val notes = buildList {
            for (index in 0 until notesArray.length()) {
                val item = notesArray.optJSONObject(index) ?: continue
                add(parseNote(item, nowInstant()))
            }
        }

        val pagination = data?.optJSONObject("pagination")
            ?: root.optJSONObject("pagination")
        val aggregateCounts = listOfNotNull(
            data?.optJSONObject("counts"),
            root.optJSONObject("counts"),
            data,
            root,
            pagination,
        )
        val aggregateAllCount = firstAvailableCount(
            containers = aggregateCounts,
            keys = listOf("allCount", "totalCount", "noteCount"),
        )
        val aggregateUserCreatedCount = firstAvailableCount(
            containers = aggregateCounts,
            keys = listOf("userCreatedCount", "totalUserCreatedCount"),
        )
        val aggregateSavedAnswerCount = firstAvailableCount(
            containers = aggregateCounts,
            keys = listOf("savedAnswerCount", "totalSavedAnswerCount"),
        )
        val allCount = aggregateAllCount ?: notes.size
        val userCreatedCount = aggregateUserCreatedCount
            ?: notes.count { it.origin == NoteOrigin.USER_CREATED }
        val savedAnswerCount = aggregateSavedAnswerCount
            ?: notes.count { it.origin == NoteOrigin.SAVED_ANSWER }
        val totalPages = pagination?.takeIf { it.has("totalPages") }?.optInt("totalPages")
        val responsePage = pagination?.optInt("page", page) ?: page
        val responseLimit = pagination?.optInt("limit", limit) ?: limit
        val hasMore = when {
            totalPages != null -> responsePage < totalPages
            pagination != null && pagination.has("totalCount") ->
                responsePage * responseLimit < (aggregateAllCount ?: notes.size)
            else -> notes.size >= responseLimit
        }

        return NoteLibrary(
            notes = notes,
            allCount = allCount.coerceAtLeast(0),
            userCreatedCount = userCreatedCount.coerceAtLeast(0),
            savedAnswerCount = savedAnswerCount.coerceAtLeast(0),
            page = responsePage,
            limit = responseLimit,
            hasMore = hasMore,
        )
    }

    companion object {
        /** Markdown (editor/domain) → HTML for the notes API wire format. */
        internal fun contentToApi(markdown: String): String =
            NotebookHtml.markdownToHtml(markdown)

        /** HTML from the notes API → markdown for the editor/domain. */
        internal fun contentFromApi(htmlOrText: String): String =
            NotebookHtml.htmlToMarkdown(htmlOrText)

        internal fun parseNote(
            json: JSONObject,
            nowInstant: Instant = Instant.now(),
        ): Note {
            val id = json.optString("id").takeIf { it.isNotBlank() }
                ?: throw IOException("Note payload missing id")
            val spaceId = sequenceOf("researchSpaceId", "spaceId")
                .map { json.optString(it) }
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
            val title = json.optString("title").takeIf { it.isNotBlank() } ?: "Untitled note"
            // List payloads use contentPreview; create/detail use content.
            // API stores HTML (same subset as notebook); domain/editor keep markdown.
            val rawContent = sequenceOf("content", "contentPreview")
                .map { json.optString(it) }
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
            val content = contentFromApi(rawContent)
            val updatedAt = sequenceOf("updatedAt", "createdAt")
                .map { json.optString(it) }
                .firstOrNull { it.isNotBlank() }
                .orEmpty()

            return Note(
                id = id,
                title = title,
                content = content,
                project = json.optString("project").takeIf { it.isNotBlank() },
                updatedLabel = formatUpdatedLabel(updatedAt, nowInstant),
                isPinned = json.optBoolean("isPinned", false),
                spaceId = spaceId,
                origin = mapOriginType(json.optString("originType")),
                citationCount = json.optInt("citationCount", 0).coerceAtLeast(0),
            )
        }

        internal fun mapOriginType(raw: String?): NoteOrigin =
            when (raw?.trim()?.lowercase()) {
                "savedassistantanswer", "savedanswer", "saved_answer" -> NoteOrigin.SAVED_ANSWER
                else -> NoteOrigin.USER_CREATED
            }

        internal fun formatUpdatedLabel(isoInstant: String, nowInstant: Instant): String {
            val instant = parseIsoInstant(isoInstant) ?: return NoteUpdatedLabelFormatter.format(
                instant = nowInstant,
            )
            return NoteUpdatedLabelFormatter.format(instant = instant)
        }

        private fun parseIsoInstant(isoInstant: String): Instant? {
            val value = isoInstant.trim()
            if (value.isEmpty()) return null
            return runCatching { Instant.parse(value) }.getOrNull()
                ?: runCatching {
                    Instant.parse(value.replace(" ", "T"))
                }.getOrNull()
        }

        private fun firstAvailableCount(
            containers: List<JSONObject>,
            keys: List<String>,
        ): Int? {
            for (container in containers) {
                for (key in keys) {
                    if (!container.has(key)) continue
                    val count = container.optInt(key, -1)
                    if (count >= 0) return count
                }
            }
            return null
        }
    }
}
