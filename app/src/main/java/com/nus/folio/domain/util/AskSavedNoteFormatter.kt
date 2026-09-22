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

    /**
     * Answer body stored on the note. Keeps inline `[n]` citation markers, then
     * appends bold Limitation / Evidence headings. Evidence items are a markdown
     * bullet list of source + location (quote text opens on citation tap).
     */
    fun body(
        content: String,
        citations: List<AskCitation> = emptyList(),
        limitation: String? = null,
    ): String {
        val answer = content.trim()
        val limitationDetail = limitation
            ?.trim()
            ?.removePrefix(LIMITATION_LABEL)
            ?.trim()
            .orEmpty()
        val evidenceBlocks = citations
            .sortedBy { it.index }
            .mapNotNull { citation -> formatEvidenceLabel(citation) }
        return buildString {
            if (answer.isNotEmpty()) {
                append(answer)
            }
            if (limitationDetail.isNotEmpty()) {
                if (isNotEmpty()) append("\n\n")
                append("**")
                append(LIMITATION_LABEL)
                append("** ")
                append(limitationDetail)
            }
            if (evidenceBlocks.isNotEmpty()) {
                if (isNotEmpty()) append("\n\n")
                append("**")
                append(EVIDENCE_LABEL)
                append("**")
                append('\n')
                evidenceBlocks.forEach { label ->
                    append("\n- ")
                    append(label)
                }
            }
        }.take(NoteInputRules.MAX_CONTENT_LENGTH).trim()
    }

    private fun formatEvidenceLabel(citation: AskCitation): String? {
        val title = citation.sourceTitle.trim()
        val location = citation.locationLabel.trim()
        if (title.isEmpty() && location.isEmpty()) return null
        return buildString {
            append('[')
            append(citation.index)
            append("] ")
            if (title.isNotEmpty()) {
                append(title)
            }
            if (location.isNotEmpty()) {
                if (title.isNotEmpty()) append(" — ")
                append(location)
            }
        }
    }

    const val DEFAULT_TITLE = "Saved answer"
    const val LIMITATION_LABEL = "Limitation:"
    const val EVIDENCE_LABEL = "Evidence"
}
