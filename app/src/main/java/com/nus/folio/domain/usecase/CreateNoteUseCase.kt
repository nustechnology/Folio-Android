package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.CreateNoteRequest
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.repository.NoteRepository

class CreateNoteUseCase(
    private val repository: NoteRepository,
) {
    suspend operator fun invoke(request: CreateNoteRequest): Result<Note> =
        repository.createNote(request)
}
