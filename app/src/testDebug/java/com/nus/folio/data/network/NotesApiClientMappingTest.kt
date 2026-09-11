package com.nus.folio.data.network

import com.nus.folio.domain.model.NoteOrigin
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class NotesApiClientMappingTest {

    @Test
    fun `mapOriginType maps API values`() {
        assertEquals(NoteOrigin.USER_CREATED, NotesApiClient.mapOriginType("UserCreated"))
        assertEquals(NoteOrigin.SAVED_ANSWER, NotesApiClient.mapOriginType("SavedAnswer"))
        assertEquals(NoteOrigin.SAVED_ANSWER, NotesApiClient.mapOriginType("SavedAssistantAnswer"))
        assertEquals(NoteOrigin.SAVED_ANSWER, NotesApiClient.mapOriginType("saved_answer"))
        assertEquals(NoteOrigin.USER_CREATED, NotesApiClient.mapOriginType("unknown"))
        assertEquals(NoteOrigin.USER_CREATED, NotesApiClient.mapOriginType(null))
    }

    @Test
    fun `formatUpdatedLabel uses note date style`() {
        val zone = ZoneId.systemDefault()
        val instant = Instant.parse("2026-08-06T06:17:59.348Z")
        val expected = instant.atZone(zone)
            .format(DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault()))

        assertEquals(
            expected,
            NotesApiClient.formatUpdatedLabel(
                "2026-08-06T06:17:59.348Z",
                nowInstant = Instant.parse("2026-08-06T12:00:00Z"),
            ),
        )
    }

    @Test
    fun `formatUpdatedLabel falls back when timestamp blank`() {
        val now = Instant.parse("2026-08-06T12:00:00Z")
        val expected = now.atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault()))

        assertEquals(expected, NotesApiClient.formatUpdatedLabel("", now))
    }

    @Test
    fun `spaceNotes path encodes space id`() {
        assertEquals(
            "https://example.test/api/v1/spaces/space%2F1/notes",
            FolioApiPaths.spaceNotes("space/1", baseUrl = "https://example.test"),
        )
    }

    @Test
    fun `spaceNotes path appends query`() {
        assertEquals(
            "https://example.test/api/v1/spaces/space-1/notes?sort=recently-updated&page=1&limit=10",
            FolioApiPaths.spaceNotes(
                spaceId = "space-1",
                baseUrl = "https://example.test",
                query = "sort=recently-updated&page=1&limit=10",
            ),
        )
    }

    @Test
    fun `spaceNote path encodes ids`() {
        assertEquals(
            "https://example.test/api/v1/spaces/space%2F1/notes/note%2F2",
            FolioApiPaths.spaceNote(
                spaceId = "space/1",
                noteId = "note/2",
                baseUrl = "https://example.test",
            ),
        )
    }

    @Test
    fun `spaceNoteConvertToSource path encodes ids`() {
        assertEquals(
            "https://example.test/api/v1/spaces/space%2F1/notes/note%2F2/convert-to-source",
            FolioApiPaths.spaceNoteConvertToSource(
                spaceId = "space/1",
                noteId = "note/2",
                baseUrl = "https://example.test",
            ),
        )
    }

    @Test
    fun `contentToApi converts markdown to html like notebook`() {
        assertEquals(
            "<p><strong>Hello</strong> and <em>world</em></p>",
            NotesApiClient.contentToApi("**Hello** and _world_"),
        )
    }

    @Test
    fun `contentFromApi converts html to markdown like notebook`() {
        assertEquals(
            "**Hello** and _world_",
            NotesApiClient.contentFromApi("<p><strong>Hello</strong> and <em>world</em></p>"),
        )
    }

    @Test
    fun `contentFromApi keeps plain text without html tags`() {
        assertEquals(
            "Already markdown **bold**",
            NotesApiClient.contentFromApi("Already markdown **bold**"),
        )
    }

    @Test
    fun `contentToApi converts lists and links`() {
        assertEquals(
            "<ul><li>First</li><li><a href=\"https://example.com\">Second</a></li></ul>",
            NotesApiClient.contentToApi("- First\n- [Second](https://example.com)"),
        )
    }
}
