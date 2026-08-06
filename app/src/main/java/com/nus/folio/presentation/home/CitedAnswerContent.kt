package com.nus.folio.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.domain.model.AskCitation
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CitedAnswerContent(
    content: String,
    citations: List<AskCitation>,
    onCitationClick: (AskCitation) -> Unit,
    modifier: Modifier = Modifier,
    interactiveCitations: Boolean = true,
) {
    val parts = remember(content) { parseAskContent(content) }
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        parts.forEach { part ->
            when (part) {
                is AskContentPart.Text -> {
                    Text(
                        text = part.value,
                        fontSize = 15.sp,
                        color = HomeTextPrimary,
                        lineHeight = 20.sp,
                    )
                }
                is AskContentPart.Citation -> {
                    val citation = resolveAskCitation(citations, part.index)
                    Text(
                        text = "[${part.index}]",
                        modifier = Modifier
                            .clip(AskSourceChipShape)
                            .background(AskSourceChipBackground)
                            .then(
                                if (interactiveCitations && citation != null) {
                                    Modifier
                                        .clickable { onCitationClick(citation) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                } else {
                                    Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                },
                            ),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HomeHeader,
                    )
                }
            }
        }
    }
}
