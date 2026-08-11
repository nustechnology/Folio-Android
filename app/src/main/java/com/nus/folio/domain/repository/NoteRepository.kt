package com.nus.folio.domain.repository

import com.nus.folio.domain.model.CreateNoteRequest
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteLibrary
import com.nus.folio.domain.model.NotePaging
import com.nus.folio.domain.model.NoteSort
import com.nus.folio.domain.model.Source

interface NoteRepository {
    suspend fun getNotes(
        spaceId: String,
        search: String? = null,
        sort: NoteSort = NoteSort.DEFAULT,
        origin: String? = null,
        page: Int = NotePaging.DEFAULT_PAGE,
        limit: Int = NotePaging.DEFAULT_LIMIT,
    ): Result<NoteLibrary>

    suspend fun getNote(spaceId: String, noteId: String): Result<Note>

    suspend fun createNote(request: CreateNoteRequest): Result<Note>
    suspend fun updateNote(note: Note): Result<Note>
    suspend fun deleteNote(spaceId: String, noteId: String): Result<Unit>
    suspend fun convertNoteToSource(spaceId: String, noteId: String, title: String): Result<Source>
}
