package com.nus.folio.domain.usecase

import com.nus.folio.domain.repository.NoteRepository

class DeleteNoteUseCase(
    private val repository: NoteRepository,
) {
    suspend operator fun invoke(noteId: String): Result<Unit> =
        repository.deleteNote(noteId)
}
