package com.nus.folio.data.repository

import com.nus.folio.data.datasource.NoteDataSource
import com.nus.folio.domain.model.CreateNoteRequest
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteLibrary
import com.nus.folio.domain.model.NoteSort
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.repository.NoteRepository
import kotlin.coroutines.cancellation.CancellationException

class NoteRepositoryImpl(
    private val dataSource: NoteDataSource,
) : NoteRepository {

    override suspend fun getNotes(
        spaceId: String,
        search: String?,
        sort: NoteSort,
        origin: String?,
        page: Int,
        limit: Int,
    ): Result<NoteLibrary> =
        try {
            Result.success(
                dataSource.fetchNotes(
                    spaceId = spaceId,
                    search = search,
                    sort = sort,
                    origin = origin,
                    page = page,
                    limit = limit,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun getNote(spaceId: String, noteId: String): Result<Note> =
        try {
            Result.success(dataSource.fetchNote(spaceId, noteId))
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

    override suspend fun deleteNote(spaceId: String, noteId: String): Result<Unit> =
        try {
            dataSource.deleteNote(spaceId, noteId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun convertNoteToSource(
        spaceId: String,
        noteId: String,
        title: String,
    ): Result<Source> =
        try {
            Result.success(dataSource.convertNoteToSource(spaceId, noteId, title))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
