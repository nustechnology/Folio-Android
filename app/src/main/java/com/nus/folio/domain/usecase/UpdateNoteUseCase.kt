package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.Note
import com.nus.folio.domain.repository.NoteRepository

class UpdateNoteUseCase(
    private val repository: NoteRepository,
) {
    suspend operator fun invoke(note: Note): Result<Note> =
        repository.updateNote(note)
}
