package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.Note
import com.nus.folio.domain.repository.NoteRepository

class GetNoteDetailUseCase(
    private val repository: NoteRepository,
) {
    suspend operator fun invoke(spaceId: String, noteId: String): Result<Note> =
        repository.getNote(spaceId, noteId)
}
