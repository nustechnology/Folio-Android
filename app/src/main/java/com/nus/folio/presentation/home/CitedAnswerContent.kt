package com.nus.folio.presentation.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.nus.folio.domain.model.AskCitation
import com.nus.folio.presentation.home.notebook.NotebookMarkdownVisuals
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeTextPrimary

private val AskCitationRegex = Regex("""\[(\d+)\]""")

internal sealed interface AskContentPart {
    data class Text(val value: String) : AskContentPart
    data class Citation(val index: Int) : AskContentPart
}

internal fun parseAskContent(content: String): List<AskContentPart> {
    if (content.isEmpty()) return emptyList()
    val parts = mutableListOf<AskContentPart>()
    var lastIndex = 0
    AskCitationRegex.findAll(content).forEach { match ->
        val start = match.range.first
        if (start > lastIndex) {
            parts += AskContentPart.Text(content.substring(lastIndex, start))
        }
        val index = match.groupValues[1].toIntOrNull()
        if (index != null) {
            parts += AskContentPart.Citation(index)
        } else {
            parts += AskContentPart.Text(match.value)
        }
        lastIndex = match.range.last + 1
    }
    if (lastIndex < content.length) {
        parts += AskContentPart.Text(content.substring(lastIndex))
    }
    return parts
}

internal fun resolveAskCitation(citations: List<AskCitation>, index: Int): AskCitation? =
    citations.find { it.index == index }

@Composable
internal fun CitedAnswerContent(
    content: String,
    citations: List<AskCitation>,
    onCitationClick: (AskCitation) -> Unit,
    modifier: Modifier = Modifier,
    interactiveCitations: Boolean = true,
    renderMarkdown: Boolean = false,
) {
    val annotated = remember(content, citations, interactiveCitations, renderMarkdown, onCitationClick) {
        buildCitedAnswerString(
            content = content,
            citations = citations,
            interactiveCitations = interactiveCitations,
            renderMarkdown = renderMarkdown,
            onCitationClick = onCitationClick,
        )
    }
    Text(
        text = annotated,
        modifier = modifier.fillMaxWidth(),
        fontSize = 15.sp,
        color = HomeTextPrimary,
        lineHeight = 20.sp,
    )
}

private fun buildCitedAnswerString(
    content: String,
    citations: List<AskCitation>,
    interactiveCitations: Boolean,
    renderMarkdown: Boolean,
    onCitationClick: (AskCitation) -> Unit,
) = buildAnnotatedString {
    parseAskContent(content).forEach { part ->
        when (part) {
            is AskContentPart.Text -> {
                if (renderMarkdown) {
                    append(NotebookMarkdownVisuals.visualize(part.value).text)
                } else {
                    append(part.value)
                }
            }
            is AskContentPart.Citation -> {
                val citation = resolveAskCitation(citations, part.index)
                val marker = "[${part.index}]"
                val style = SpanStyle(
                    fontWeight = FontWeight.SemiBold,
                    color = HomeHeader,
                    fontSize = 13.sp,
                    background = AskSourceChipBackground,
                )
                if (interactiveCitations && citation != null) {
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "citation-${part.index}",
                            linkInteractionListener = { onCitationClick(citation) },
                        ),
                    ) {
                        withStyle(style) { append(marker) }
                    }
                } else {
                    withStyle(style) { append(marker) }
                }
            }
        }
    }
}
