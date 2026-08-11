package com.nus.folio.presentation.home

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.domain.model.Source

/** Allowlisted validation keys — never shown raw; mapped in [toHomeActionError]. */
internal const val ERROR_FILE_REQUIRED = "FILE_REQUIRED"
internal const val ERROR_FILE_UNSUPPORTED = "FILE_UNSUPPORTED"
internal const val ERROR_FILE_TOO_LARGE = "FILE_TOO_LARGE"

internal const val ASK_STREAM_FALLBACK_ERROR =
    "Something went wrong. Please try again."

internal fun HomeUiState.withCurrentUser(session: AuthSession?): HomeUiState =
    copy(
        userDisplayName = session?.displayName.orEmpty(),
        userEmail = session?.email.orEmpty(),
    )

/**
 * Maps failures to safe UI codes. Never surfaces [Throwable.message] (CWE-209).
 */
internal fun Throwable.toHomeActionError(): HomeActionError = when (this) {
    is java.io.IOException -> HomeActionError.NETWORK
    is IllegalArgumentException -> when (message) {
        ERROR_FILE_REQUIRED -> HomeActionError.FILE_REQUIRED
        ERROR_FILE_UNSUPPORTED -> HomeActionError.FILE_UNSUPPORTED
        ERROR_FILE_TOO_LARGE -> HomeActionError.FILE_TOO_LARGE
        else -> HomeActionError.GENERIC
    }
    else -> HomeActionError.GENERIC
}

internal fun filterSources(state: HomeUiState): List<Source> = state.allSources

internal fun filterNotes(state: HomeUiState): List<Note> =
    baseFilteredNotes(state).filterBySearchQuery(state.searchQuery)

private fun baseFilteredNotes(state: HomeUiState): List<Note> =
    when (state.selectedNoteFilter) {
        NoteFilter.ALL -> state.allNotes
        NoteFilter.USER_CREATED -> state.allNotes.filter { it.origin == NoteOrigin.USER_CREATED }
        NoteFilter.SAVED_ANSWER -> state.allNotes.filter { it.origin == NoteOrigin.SAVED_ANSWER }
    }

private fun List<Note>.filterBySearchQuery(searchQuery: String): List<Note> {
    val query = searchQuery.trim()
    if (query.isEmpty()) return this
    return filter { note ->
        note.title.contains(query, ignoreCase = true) ||
            note.content.contains(query, ignoreCase = true) ||
            note.project?.contains(query, ignoreCase = true) == true
    }
}
