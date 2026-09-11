package com.nus.folio.presentation.home.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.AnimatedModalSheet
import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.SourceType
import com.nus.folio.presentation.home.AskSuggestionShape
import com.nus.folio.presentation.home.HomeBadgeShape
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.presentation.home.sourceTypeBadgeColors
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeStatusProcessingBackground
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary

@Composable
internal fun CitationPreviewBottomSheet(
    citation: AskCitation,
    onDismiss: () -> Unit,
    onOpenInSource: () -> Unit,
) {
    AnimatedModalSheet(
        onDismiss = onDismiss,
    ) { requestDismiss ->
        AddSourceDragHandle()
        CitationPreviewSheetContent(
            citation = citation,
            onClose = { requestDismiss() },
            onOpenInSource = { requestDismiss { onOpenInSource() } },
        )
    }
}

@Composable
private fun CitationPreviewSheetContent(
    citation: AskCitation,
    onClose: () -> Unit,
    onOpenInSource: () -> Unit,
) {
    val badgeColors = sourceTypeBadgeColors(citation.sourceType)
    val typeLabel = citationBadgeLabel(citation)
    val canOpenInSource = citation.sourceId.isNotBlank() && citation.sourceId != "unknown"

    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.citation_preview_title),
                fontFamily = CormorantGaramond,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeTextPrimary,
            )
            Text(
                text = stringResource(R.string.citation_preview_index, citation.index),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = HomeTextSecondary,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = citation.sourceTitle,
                modifier = Modifier.weight(1f),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = typeLabel,
                modifier = Modifier
                    .clip(HomeBadgeShape)
                    .background(badgeColors.background)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = badgeColors.content,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.citation_preview_evidence_label),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextSecondary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp, max = 500.dp)
                .clip(AskSuggestionShape)
                .background(HomeCardBackground)
                .border(1.dp, HomeCardBorder, AskSuggestionShape)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (citation.locationLabel.isNotBlank()) {
                Text(
                    text = citation.locationLabel.uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = HomeTextSecondary,
                )
            }
            Text(
                text = citation.evidenceText.ifBlank {
                    stringResource(R.string.citation_preview_evidence_empty)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (citation.evidenceText.isNotBlank()) {
                            Modifier
                                .clip(AskSuggestionShape)
                                .background(HomeStatusProcessingBackground)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        } else {
                            Modifier
                        },
                    ),
                fontSize = 15.sp,
                color = HomeTextPrimary,
                lineHeight = 22.sp,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AddSourceCancelButton(
                onClick = onClose,
                modifier = Modifier.weight(1f),
                labelRes = R.string.citation_preview_close,
            )
            AddSourceSubmitButton(
                enabled = canOpenInSource,
                onClick = onOpenInSource,
                modifier = Modifier.weight(1f),
                labelRes = R.string.citation_preview_open_in_source,
            )
        }
    }
}

@Composable
private fun citationBadgeLabel(citation: AskCitation): String {
    when (citation.sourceType) {
        SourceType.TEXT -> return stringResource(R.string.home_type_text)
        SourceType.WEB -> return stringResource(R.string.home_type_web)
        SourceType.FILE, SourceType.BOOK -> Unit
    }
    val extension = citation.fileExtension.trim()
        .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    if (extension != null) return extension.uppercase()
    return when (citation.sourceType) {
        SourceType.BOOK -> stringResource(R.string.home_type_book)
        else -> stringResource(R.string.home_type_file)
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun CitationPreviewSheetContentPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(HomeSheetBackground, HomeSheetShape)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
            ) {
                AddSourceDragHandle()
                CitationPreviewSheetContent(
                    citation = AskCitation(
                        index = 1,
                        sourceId = "1",
                        sourceTitle = "Alan Turing: Computing Machinery",
                        sourceType = SourceType.FILE,
                        fileExtension = "pdf",
                        locationLabel = "Page 14",
                        evidenceText = "The new form of the problem can be described in terms of a game which we call the \"imitation game.\"",
                    ),
                    onClose = {},
                    onOpenInSource = {},
                )
            }
        }
    }
}
