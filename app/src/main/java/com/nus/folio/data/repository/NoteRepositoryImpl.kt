package com.nus.folio.data.repository

import com.nus.folio.data.datasource.NoteDataSource
import com.nus.folio.domain.model.CreateNoteRequest
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteLibrary
import com.nus.folio.domain.repository.NoteRepository
import kotlin.coroutines.cancellation.CancellationException

class NoteRepositoryImpl(
    private val dataSource: NoteDataSource,
) : NoteRepository {

    override suspend fun getNotes(spaceId: String): Result<NoteLibrary> =
        try {
            Result.success(dataSource.fetchNotes(spaceId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun createNote(request: CreateNoteRequest): Result<Note> =
        try {
            Result.success(dataSource.createNote(request))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun updateNote(note: Note): Result<Note> =
        try {
            Result.success(dataSource.updateNote(note))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun deleteNote(noteId: String): Result<Unit> =
        try {
            dataSource.deleteNote(noteId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
