package com.nus.folio.domain.util

import com.nus.folio.domain.model.AskCitation

/**
 * Formats an Ask assistant answer into note title/body when saving to Notes (AC2).
 */
object AskSavedNoteFormatter {

    fun titleFromQuestion(
        question: String,
        fallback: String = DEFAULT_TITLE,
        maxLength: Int = NoteInputRules.MAX_TITLE_LENGTH,
    ): String =
        question.trim()
            .take(maxLength)
            .trim()
            .ifBlank { fallback }

    /** Answer body stored on the note; keeps inline `[n]` citation markers. */
    fun body(content: String): String = content.trim()

    /**
     * Plain-text body with a citations appendix (legacy / non-interactive fallback).
     */
    fun bodyWithCitationFootnotes(
        content: String,
        citations: List<AskCitation>,
        citationsHeading: String = DEFAULT_CITATIONS_HEADING,
    ): String {
        val answer = body(content)
        if (citations.isEmpty()) return answer
        val citationLines = citations
            .sortedBy { it.index }
            .distinctBy { it.index }
            .joinToString(separator = "\n") { citation ->
                buildString {
                    append('[')
                    append(citation.index)
                    append("] ")
                    append(citation.sourceTitle)
                    if (citation.locationLabel.isNotBlank()) {
                        append(" · ")
                        append(citation.locationLabel)
                    }
                }
            }
        return buildString {
            append(answer)
            append("\n\n")
            append(citationsHeading)
            append('\n')
            append(citationLines)
        }
    }

    const val DEFAULT_TITLE = "Saved answer"
    const val DEFAULT_CITATIONS_HEADING = "Citations"
}
