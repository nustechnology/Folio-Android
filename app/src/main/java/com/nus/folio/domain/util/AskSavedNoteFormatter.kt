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
     * appends limitation and evidence excerpts so the content field is self-contained.
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
            .mapNotNull { citation -> formatEvidenceBlock(citation) }
        return buildString {
            if (answer.isNotEmpty()) {
                append(answer)
            }
            if (limitationDetail.isNotEmpty()) {
                if (isNotEmpty()) append("\n\n")
                append(LIMITATION_LABEL)
                append(' ')
                append(limitationDetail)
            }
            if (evidenceBlocks.isNotEmpty()) {
                if (isNotEmpty()) append("\n\n")
                append(EVIDENCE_LABEL)
                append("\n\n")
                append(evidenceBlocks.joinToString("\n\n"))
            }
        }.take(NoteInputRules.MAX_CONTENT_LENGTH).trim()
    }

    private fun formatEvidenceBlock(citation: AskCitation): String? {
        val evidence = citation.evidenceText.trim()
        if (evidence.isEmpty()) return null
        return buildString {
            append('[')
            append(citation.index)
            append("] ")
            val title = citation.sourceTitle.trim()
            if (title.isNotEmpty()) {
                append(title)
            }
            val location = citation.locationLabel.trim()
            if (location.isNotEmpty()) {
                if (title.isNotEmpty()) append(" — ")
                append(location)
            }
            append('\n')
            append(evidence)
        }
    }

    const val DEFAULT_TITLE = "Saved answer"
    const val LIMITATION_LABEL = "Limitation:"
    const val EVIDENCE_LABEL = "Evidence"
}
