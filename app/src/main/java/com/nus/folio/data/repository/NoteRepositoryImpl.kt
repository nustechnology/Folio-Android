package com.nus.folio.data.repository

import com.nus.folio.data.datasource.NoteDataSource
import com.nus.folio.domain.model.NoteLibrary
import com.nus.folio.domain.repository.NoteRepository
import kotlin.coroutines.cancellation.CancellationException

class NoteRepositoryImpl(
    private val dataSource: NoteDataSource,
) : NoteRepository {

    override suspend fun getNotes(): Result<NoteLibrary> =
        try {
            Result.success(dataSource.fetchNotes())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
