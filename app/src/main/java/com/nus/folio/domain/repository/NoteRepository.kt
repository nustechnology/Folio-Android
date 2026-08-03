package com.nus.folio.domain.repository

import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteLibrary

interface NoteRepository {
    suspend fun getNotes(spaceId: String): Result<NoteLibrary>
    suspend fun updateNote(note: Note): Result<Note>
    suspend fun deleteNote(noteId: String): Result<Unit>
}
