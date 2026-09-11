package com.nus.folio.presentation.home.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.AnimatedModalSheet
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTypeBadgeBackground
import com.nus.folio.ui.theme.LoginCopper

private val ConvertSnapshotMaxHeight = 320.dp
private val ConvertBannerShape = RoundedCornerShape(12.dp)

@Composable
internal fun ConvertNoteBottomSheet(
    note: Note,
    onDismiss: () -> Unit,
    onCreateSource: (title: String, snapshot: String) -> Unit = { _, _ -> },
) {
    AnimatedModalSheet(
        onDismiss = onDismiss,
        contentWindowInsets = WindowInsets.navigationBars,
    ) { requestDismiss ->
        AddSourceDragHandle()
        ConvertNoteSheetContent(
            note = note,
            onCancelClick = { requestDismiss() },
            onCreateSource = { title, snapshot ->
                requestDismiss { onCreateSource(title, snapshot) }
            },
        )
    }
}

@Composable
private fun ConvertNoteSheetContent(
    note: Note,
    onCancelClick: () -> Unit,
    onCreateSource: (title: String, snapshot: String) -> Unit,
) {
    val sourceTitle = note.title
    val snapshot = note.content
    val canCreate = sourceTitle.isNotBlank() && snapshot.isNotBlank()

    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.note_convert_title),
            fontFamily = CormorantGaramond,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextPrimary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        ConvertSnapshotBanner()
        Spacer(modifier = Modifier.height(20.dp))
        AddNoteLabeledField(
            label = stringResource(R.string.note_convert_source_title),
            value = sourceTitle,
            onValueChange = {},
            placeholder = stringResource(R.string.add_note_title_placeholder),
            singleLine = true,
            readOnly = true,
        )
        Spacer(modifier = Modifier.height(16.dp))
        AddNoteLabeledField(
            label = stringResource(R.string.note_convert_snapshot),
            value = snapshot,
            onValueChange = {},
            placeholder = stringResource(R.string.add_note_content_placeholder),
            singleLine = false,
            readOnly = true,
            fillHeight = false,
            fieldModifier = Modifier
                .fillMaxWidth()
                .heightIn(max = ConvertSnapshotMaxHeight),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AddSourceCancelButton(
                onClick = onCancelClick,
                modifier = Modifier.weight(1f),
            )
            AddSourceSubmitButton(
                enabled = canCreate,
                onClick = { onCreateSource(sourceTitle.trim(), snapshot.trim()) },
                labelRes = R.string.note_convert_create,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ConvertSnapshotBanner() {
    val lead = stringResource(R.string.note_convert_banner_lead)
    val rest = stringResource(R.string.note_convert_banner_rest)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(ConvertBannerShape)
            .background(HomeTypeBadgeBackground),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(LoginCopper),
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(lead)
                }
                append(" ")
                append(rest)
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            fontSize = 14.sp,
            color = HomeTextPrimary,
            lineHeight = 20.sp,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun ConvertNoteSheetContentPreview() {
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
                ConvertNoteSheetContent(
                    note = Note(
                        id = "1",
                        title = "Recurring operational problems",
                        content = "The evidence repeatedly identifies fragmented client records, manual compliance reporting, and weak information flow between field and office teams.",
                        project = null,
                        updatedLabel = "Updated 1d ago",
                        isPinned = false,
                        spaceId = "1",
                        origin = NoteOrigin.USER_CREATED,
                    ),
                    onCancelClick = {},
                    onCreateSource = { _, _ -> },
                )
            }
        }
    }
}
