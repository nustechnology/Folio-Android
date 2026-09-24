package com.nus.folio.presentation.home.notebook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.presentation.home.NotebookSaveStatus
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import com.nus.folio.ui.theme.LoginCopper

private val ToolbarActiveShape = RoundedCornerShape(6.dp)
private val ToolbarActiveBackground = LoginCopper.copy(alpha = 0.16f)
private val ToolbarActiveBorder = LoginCopper.copy(alpha = 0.45f)
private val ToolbarButtonMinHeight = 36.dp

@Composable
internal fun NotebookEditorToolbar(
    saveStatus: NotebookSaveStatus,
    canUndo: Boolean,
    canRedo: Boolean,
    activeMarks: NotebookToolbarMarks = NotebookToolbarMarks(),
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onHeading1Click: () -> Unit,
    onHeading2Click: () -> Unit,
    onHeading3Click: () -> Unit,
    onBulletListClick: () -> Unit,
    onOrderedListClick: () -> Unit,
    onBlockquoteClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onRetrySaveClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val editable = saveStatus != NotebookSaveStatus.READ_ONLY
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(HomeCardBackground)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            NotebookToolbarButton(
                label = stringResource(R.string.notebook_toolbar_undo),
                enabled = editable && canUndo,
                selected = false,
                onClick = onUndoClick,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "↶",
                    color = if (editable && canUndo) HomeTextPrimary else HomeTextSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
            NotebookToolbarButton(
                label = stringResource(R.string.notebook_toolbar_redo),
                enabled = editable && canRedo,
                selected = false,
                onClick = onRedoClick,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "↷",
                    color = if (editable && canRedo) HomeTextPrimary else HomeTextSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
            NotebookToolbarDivider()
            NotebookToolbarButton(
                label = stringResource(R.string.notebook_toolbar_bold),
                enabled = editable,
                selected = editable && activeMarks.bold,
                onClick = onBoldClick,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "B",
                    fontWeight = FontWeight.Bold,
                    color = toolbarContentColor(editable && activeMarks.bold, editable),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
            NotebookToolbarButton(
                label = stringResource(R.string.notebook_toolbar_italic),
                enabled = editable,
                selected = editable && activeMarks.italic,
                onClick = onItalicClick,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "I",
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = toolbarContentColor(editable && activeMarks.italic, editable),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
            NotebookToolbarDivider()
            NotebookToolbarButton(
                label = stringResource(R.string.notebook_toolbar_h1),
                enabled = editable,
                selected = editable && activeMarks.headingLevel == 1,
                onClick = onHeading1Click,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "H1",
                    color = toolbarContentColor(editable && activeMarks.headingLevel == 1, editable),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            NotebookToolbarButton(
                label = stringResource(R.string.notebook_toolbar_h2),
                enabled = editable,
                selected = editable && activeMarks.headingLevel == 2,
                onClick = onHeading2Click,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "H2",
                    color = toolbarContentColor(editable && activeMarks.headingLevel == 2, editable),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            NotebookToolbarButton(
                label = stringResource(R.string.notebook_toolbar_h3),
                enabled = editable,
                selected = editable && activeMarks.headingLevel == 3,
                onClick = onHeading3Click,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "H3",
                    color = toolbarContentColor(editable && activeMarks.headingLevel == 3, editable),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            NotebookToolbarDivider()
            NotebookToolbarButton(
                label = stringResource(R.string.notebook_toolbar_bullet_list),
                enabled = editable,
                selected = editable && activeMarks.bulletList,
                onClick = onBulletListClick,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_format_list_bulleted),
                    contentDescription = null,
                    tint = toolbarContentColor(editable && activeMarks.bulletList, editable),
                    modifier = Modifier.size(18.dp),
                )
            }
            NotebookToolbarButton(
                label = stringResource(R.string.notebook_toolbar_ordered_list),
                enabled = editable,
                selected = editable && activeMarks.orderedList,
                onClick = onOrderedListClick,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_format_list_numbered),
                    contentDescription = null,
                    tint = toolbarContentColor(editable && activeMarks.orderedList, editable),
                    modifier = Modifier.size(18.dp),
                )
            }
            NotebookToolbarButton(
                label = stringResource(R.string.notebook_toolbar_blockquote),
                enabled = editable,
                selected = editable && activeMarks.blockquote,
                onClick = onBlockquoteClick,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "❝",
                    color = toolbarContentColor(editable && activeMarks.blockquote, editable),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (saveStatus != NotebookSaveStatus.IDLE) {
            NotebookToolbarSaveStatus(
                saveStatus = saveStatus,
                onRetrySaveClick = onRetrySaveClick,
                modifier = Modifier.padding(start = 6.dp, end = 4.dp),
            )
        }
    }
}

@Composable
private fun NotebookToolbarSaveStatus(
    saveStatus: NotebookSaveStatus,
    onRetrySaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (saveStatus) {
        NotebookSaveStatus.SAVING -> {
            Text(
                text = stringResource(R.string.notebook_save_status_saving),
                color = HomeTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier,
            )
        }
        NotebookSaveStatus.SAVED -> {
            Text(
                text = stringResource(R.string.notebook_save_status_saved),
                color = HomeHeader,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier,
            )
        }
        NotebookSaveStatus.FAILED -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.notebook_save_status_retry),
                    color = HomeStatusFailedText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRetrySaveClick,
                    ),
                )
            }
        }
        NotebookSaveStatus.STALE -> {
            Text(
                text = stringResource(R.string.notebook_save_status_offline),
                color = HomeTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier,
            )
        }
        NotebookSaveStatus.READ_ONLY -> {
            Text(
                text = stringResource(R.string.notebook_save_status_read_only),
                color = HomeTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier,
            )
        }
        NotebookSaveStatus.IDLE -> Unit
    }
}

private fun toolbarContentColor(selected: Boolean, enabled: Boolean = true): Color =
    when {
        !enabled -> HomeTextSecondary
        selected -> LoginCopper
        else -> HomeTextPrimary
    }

@Composable
private fun NotebookToolbarDivider() {
    Spacer(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .width(1.dp)
            .height(20.dp)
            .background(HomeCardBorder),
    )
}

@Composable
private fun NotebookToolbarButton(
    label: String,
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = ToolbarButtonMinHeight)
            .focusProperties { canFocus = false }
            .semantics {
                contentDescription = label
                this.selected = selected
            }
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 1.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (selected) {
                        Modifier
                            .background(ToolbarActiveBackground, ToolbarActiveShape)
                            .border(1.dp, ToolbarActiveBorder, ToolbarActiveShape)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun NotebookEditorToolbarPreviewHost(
    saveStatus: NotebookSaveStatus = NotebookSaveStatus.IDLE,
    canUndo: Boolean = true,
    canRedo: Boolean = false,
    activeMarks: NotebookToolbarMarks = NotebookToolbarMarks(),
) {
    NotebookEditorToolbar(
        saveStatus = saveStatus,
        canUndo = canUndo,
        canRedo = canRedo,
        activeMarks = activeMarks,
        onBoldClick = {},
        onItalicClick = {},
        onHeading1Click = {},
        onHeading2Click = {},
        onHeading3Click = {},
        onBulletListClick = {},
        onOrderedListClick = {},
        onBlockquoteClick = {},
        onUndoClick = {},
        onRedoClick = {},
        onRetrySaveClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
    )
}


@Preview(showBackground = true, widthDp = 393, backgroundColor = 0xFFF7F1E6, name = "Toolbar - idle")
@Composable
private fun NotebookEditorToolbarIdlePreview() {
    FolioAndroidTheme(dynamicColor = false) {
        NotebookEditorToolbarPreviewHost()
    }
}

@Preview(showBackground = true, widthDp = 393, backgroundColor = 0xFFF7F1E6, name = "Toolbar - active marks")
@Composable
private fun NotebookEditorToolbarActiveMarksPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        NotebookEditorToolbarPreviewHost(
            canUndo = true,
            canRedo = true,
            activeMarks = NotebookToolbarMarks(
                bold = true,
                italic = true,
                headingLevel = 1,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, backgroundColor = 0xFFF7F1E6, name = "Toolbar - saved right")
@Composable
private fun NotebookEditorToolbarSavedPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        NotebookEditorToolbarPreviewHost(
            saveStatus = NotebookSaveStatus.SAVED,
            activeMarks = NotebookToolbarMarks(bulletList = true),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, backgroundColor = 0xFFF7F1E6, name = "Toolbar - all states")
@Composable
private fun NotebookEditorToolbarAllStatesPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NotebookEditorToolbarPreviewHost(saveStatus = NotebookSaveStatus.IDLE)
            NotebookEditorToolbarPreviewHost(
                saveStatus = NotebookSaveStatus.SAVING,
                activeMarks = NotebookToolbarMarks(italic = true, headingLevel = 2),
            )
            NotebookEditorToolbarPreviewHost(
                saveStatus = NotebookSaveStatus.SAVED,
                activeMarks = NotebookToolbarMarks(bold = true, orderedList = true),
            )
            NotebookEditorToolbarPreviewHost(
                saveStatus = NotebookSaveStatus.FAILED,
                canUndo = true,
                canRedo = true,
                activeMarks = NotebookToolbarMarks(blockquote = true),
            )
        }
    }
}
