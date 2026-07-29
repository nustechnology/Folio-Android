package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.NoteLibrary
import com.nus.folio.domain.repository.NoteRepository

class GetNotesUseCase(
    private val repository: NoteRepository,
) {
    suspend operator fun invoke(): Result<NoteLibrary> = repository.getNotes()
}
