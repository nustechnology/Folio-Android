package com.nus.folio.presentation.home.notebook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.presentation.home.HomeCardShape
import com.nus.folio.presentation.home.NotebookSaveStatus
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary

@Composable
internal fun NotebookEditorToolbar(
    saveStatus: NotebookSaveStatus,
    canUndo: Boolean,
    canRedo: Boolean,
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
            .border(1.dp, HomeCardBorder, HomeCardShape)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        NotebookToolbarIconButton(
            label = stringResource(R.string.notebook_toolbar_bold),
            enabled = editable,
            onClick = onBoldClick,
        ) {
            Text(
                text = stringResource(R.string.notebook_toolbar_bold_symbol),
                fontWeight = FontWeight.Bold,
                color = if (editable) HomeTextPrimary else HomeTextSecondary,
                fontSize = 16.sp,
            )
        }
        NotebookToolbarIconButton(
            label = stringResource(R.string.notebook_toolbar_italic),
            enabled = editable,
            onClick = onItalicClick,
        ) {
            Text(
                text = stringResource(R.string.notebook_toolbar_italic_symbol),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = if (editable) HomeTextPrimary else HomeTextSecondary,
                fontSize = 16.sp,
            )
        }
        NotebookToolbarDivider()
        NotebookToolbarTextButton(
            label = stringResource(R.string.notebook_toolbar_h1),
            enabled = editable,
            onClick = onHeading1Click,
        ) {
            Text(
                text = stringResource(R.string.notebook_toolbar_h1),
                color = if (editable) HomeTextPrimary else HomeTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        NotebookToolbarTextButton(
            label = stringResource(R.string.notebook_toolbar_h2),
            enabled = editable,
            onClick = onHeading2Click,
        ) {
            Text(
                text = stringResource(R.string.notebook_toolbar_h2),
                color = if (editable) HomeTextPrimary else HomeTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        NotebookToolbarTextButton(
            label = stringResource(R.string.notebook_toolbar_h3),
            enabled = editable,
            onClick = onHeading3Click,
        ) {
            Text(
                text = stringResource(R.string.notebook_toolbar_h3),
                color = if (editable) HomeTextPrimary else HomeTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        NotebookToolbarDivider()
        NotebookToolbarIconButton(
            label = stringResource(R.string.notebook_toolbar_bullet_list),
            enabled = editable,
            onClick = onBulletListClick,
        ) {
            Text(
                text = stringResource(R.string.notebook_toolbar_bullet_symbol),
                color = if (editable) HomeTextPrimary else HomeTextSecondary,
                fontSize = 18.sp,
            )
        }
        NotebookToolbarIconButton(
            label = stringResource(R.string.notebook_toolbar_ordered_list),
            enabled = editable,
            onClick = onOrderedListClick,
        ) {
            Text(
                text = stringResource(R.string.notebook_toolbar_ordered_symbol),
                color = if (editable) HomeTextPrimary else HomeTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        NotebookToolbarIconButton(
            label = stringResource(R.string.notebook_toolbar_blockquote),
            enabled = editable,
            onClick = onBlockquoteClick,
        ) {
            Text(
                text = stringResource(R.string.notebook_toolbar_blockquote_symbol),
                color = if (editable) HomeTextPrimary else HomeTextSecondary,
                fontSize = 16.sp,
            )
        }
        NotebookToolbarDivider()
        NotebookToolbarIconButton(
            label = stringResource(R.string.notebook_toolbar_undo),
            enabled = editable && canUndo,
            onClick = onUndoClick,
        ) {
            Text(
                text = stringResource(R.string.notebook_toolbar_undo_symbol),
                color = if (editable && canUndo) HomeTextPrimary else HomeTextSecondary,
                fontSize = 18.sp,
            )
        }
        NotebookToolbarIconButton(
            label = stringResource(R.string.notebook_toolbar_redo),
            enabled = editable && canRedo,
            onClick = onRedoClick,
        ) {
            Text(
                text = stringResource(R.string.notebook_toolbar_redo_symbol),
                color = if (editable && canRedo) HomeTextPrimary else HomeTextSecondary,
                fontSize = 18.sp,
            )
        }
        NotebookToolbarDivider()
        when (saveStatus) {
            NotebookSaveStatus.SAVING -> {
                Text(
                    text = stringResource(R.string.notebook_save_status_saving),
                    color = HomeTextSecondary,
                    fontSize = 12.sp,
                )
            }
            NotebookSaveStatus.SAVED -> {
                Text(
                    text = stringResource(R.string.notebook_save_status_saved),
                    color = HomeHeader,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            NotebookSaveStatus.FAILED -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.notebook_save_status_failed),
                        color = HomeStatusFailedText,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = stringResource(R.string.notebook_save_status_retry),
                        color = HomeStatusFailedText,
                        fontSize = 12.sp,
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
                    fontSize = 12.sp,
                )
            }
            NotebookSaveStatus.READ_ONLY -> {
                Text(
                    text = stringResource(R.string.notebook_save_status_read_only),
                    color = HomeTextSecondary,
                    fontSize = 12.sp,
                )
            }
            NotebookSaveStatus.IDLE -> Unit
        }
    }
}

@Composable
private fun NotebookToolbarDivider() {
    Spacer(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .padding(vertical = 8.dp)
            .background(HomeCardBorder),
    )
}

@Composable
private fun NotebookToolbarIconButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.semantics { contentDescription = label },
    ) {
        content()
    }
}

@Composable
private fun NotebookToolbarTextButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.semantics { contentDescription = label },
    ) {
        content()
    }
}
