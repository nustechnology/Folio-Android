package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.Source
import com.nus.folio.domain.repository.NoteRepository

class ConvertNoteToSourceUseCase(
    private val repository: NoteRepository,
) {
    suspend operator fun invoke(
        spaceId: String,
        noteId: String,
        title: String,
    ): Result<Source> = repository.convertNoteToSource(spaceId, noteId, title)
}
