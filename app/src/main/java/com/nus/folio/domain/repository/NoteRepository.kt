package com.nus.folio.domain.repository

import com.nus.folio.domain.model.NoteLibrary

interface NoteRepository {
    suspend fun getNotes(): Result<NoteLibrary>
}
