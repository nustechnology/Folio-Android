package com.nus.folio.presentation.home.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.domain.model.SourceType
import com.nus.folio.presentation.home.CitedAnswerContent
import com.nus.folio.presentation.home.HomeBadgeShape
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.presentation.home.HomeUploadZoneShape
import com.nus.folio.presentation.home.SourceTypeBadgeColors
import com.nus.folio.presentation.home.noteOriginBadgeColors
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeReadOnlyFieldBackground
import com.nus.folio.ui.theme.HomeReadOnlyFieldBorder
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary

private val ViewNoteContentHeight = 160.dp

@Composable
internal fun ViewNoteBottomSheet(
    note: Note,
    onDismiss: () -> Unit,
    onConvertClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onCitationClick: (AskCitation) -> Unit = {},
) {
    AnimatedModalSheet(
        onDismiss = onDismiss,
        contentWindowInsets = WindowInsets.navigationBars,
    ) { requestDismiss ->
        AddSourceDragHandle()
        ViewNoteSheetContent(
            note = note,
            onConvertClick = { requestDismiss { onConvertClick() } },
            onEditClick = { requestDismiss { onEditClick() } },
            onCloseClick = { requestDismiss() },
            onCitationClick = onCitationClick,
        )
    }
}

@Composable
private fun ViewNoteSheetContent(
    note: Note,
    onConvertClick: () -> Unit,
    onEditClick: () -> Unit,
    onCloseClick: () -> Unit,
    onCitationClick: (AskCitation) -> Unit = {},
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = note.title,
                fontFamily = CormorantGaramond,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeTextPrimary,
                modifier = Modifier.weight(1f),
            )
            SheetCloseIconButton(onClick = onCloseClick)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ViewNoteOriginBadges(note = note)
            Text(
                text = note.updatedLabel,
                fontSize = 13.sp,
                color = HomeTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        ViewNoteContentBox(
            content = note.content,
            citations = note.citations,
            onCitationClick = onCitationClick,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AddSourceCancelButton(
                onClick = onConvertClick,
                labelRes = R.string.note_options_convert,
                modifier = Modifier.weight(2f),
            )
            AddSourceSubmitButton(
                enabled = true,
                onClick = onEditClick,
                labelRes = R.string.note_options_edit,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ViewNoteOriginBadges(note: Note) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (note.origin) {
            NoteOrigin.USER_CREATED -> {
                ViewNoteBadge(
                    label = stringResource(R.string.home_note_badge_user_created),
                    colors = noteOriginBadgeColors(NoteOrigin.USER_CREATED),
                )
            }
            NoteOrigin.SAVED_ANSWER -> {
                ViewNoteBadge(
                    label = stringResource(R.string.home_note_badge_saved_answer),
                    colors = noteOriginBadgeColors(NoteOrigin.SAVED_ANSWER),
                )
                if (note.citationCount > 0) {
                    ViewNoteBadge(
                        label = stringResource(
                            R.string.home_note_badge_citations,
                            note.citationCount,
                        ),
                        colors = noteOriginBadgeColors(NoteOrigin.SAVED_ANSWER),
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewNoteBadge(
    label: String,
    colors: SourceTypeBadgeColors,
) {
    Text(
        text = label,
        modifier = Modifier
            .clip(HomeBadgeShape)
            .background(colors.background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ViewNoteContentBox(
    content: String,
    citations: List<AskCitation>,
    onCitationClick: (AskCitation) -> Unit,
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ViewNoteContentHeight)
            .clip(HomeUploadZoneShape)
            .border(1.dp, HomeReadOnlyFieldBorder, HomeUploadZoneShape)
            .background(HomeReadOnlyFieldBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (citations.isNotEmpty() || content.contains(Regex("""\[\d+\]"""))) {
            CitedAnswerContent(
                content = content,
                citations = citations,
                onCitationClick = onCitationClick,
                interactiveCitations = citations.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
            )
        } else {
            Text(
                text = content,
                color = HomeTextPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun ViewNoteSheetContentPreview() {
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
                ViewNoteSheetContent(
                    note = Note(
                        id = "3",
                        title = "Turing Test — Key Takeaways",
                        content = "The imitation game reframes intelligence as observable linguistic behavior [1].",
                        project = "Dissertation Research",
                        updatedLabel = "Updated 3d ago",
                        isPinned = false,
                        spaceId = "1",
                        origin = NoteOrigin.SAVED_ANSWER,
                        citationCount = 1,
                        citations = listOf(
                            AskCitation(
                                index = 1,
                                sourceId = "1",
                                sourceTitle = "Computing Machinery",
                                sourceType = SourceType.FILE,
                                locationLabel = "Page 14",
                            ),
                        ),
                    ),
                    onConvertClick = {},
                    onEditClick = {},
                    onCloseClick = {},
                )
            }
        }
    }
}
