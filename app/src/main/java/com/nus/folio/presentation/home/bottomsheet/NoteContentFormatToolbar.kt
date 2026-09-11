package com.nus.folio.presentation.home.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.presentation.home.notebook.NotebookToolbarMarks
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.LoginCopper

private val ToolbarActiveShape = RoundedCornerShape(6.dp)
private val ToolbarActiveBackground = LoginCopper.copy(alpha = 0.16f)
private val ToolbarActiveBorder = LoginCopper.copy(alpha = 0.45f)
private val ToolbarButtonSize = 32.dp

@Composable
internal fun NoteContentFormatToolbar(
    activeMarks: NotebookToolbarMarks,
    enabled: Boolean = true,
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onBulletListClick: () -> Unit,
    onOrderedListClick: () -> Unit,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        NoteFormatToolbarButton(
            label = stringResource(R.string.notebook_toolbar_bold),
            enabled = enabled,
            selected = activeMarks.bold,
            onClick = onBoldClick,
        ) {
            Text(
                text = "B",
                fontWeight = FontWeight.Bold,
                color = formatToolbarContentColor(activeMarks.bold, enabled),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
        NoteFormatToolbarButton(
            label = stringResource(R.string.notebook_toolbar_italic),
            enabled = enabled,
            selected = activeMarks.italic,
            onClick = onItalicClick,
        ) {
            Text(
                text = "I",
                fontStyle = FontStyle.Italic,
                color = formatToolbarContentColor(activeMarks.italic, enabled),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
        NoteFormatToolbarDivider()
        NoteFormatToolbarButton(
            label = stringResource(R.string.notebook_toolbar_bullet_list),
            enabled = enabled,
            selected = activeMarks.bulletList,
            onClick = onBulletListClick,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_format_list_bulleted),
                contentDescription = null,
                tint = formatToolbarContentColor(activeMarks.bulletList, enabled),
                modifier = Modifier.size(18.dp),
            )
        }
        NoteFormatToolbarButton(
            label = stringResource(R.string.notebook_toolbar_ordered_list),
            enabled = enabled,
            selected = activeMarks.orderedList,
            onClick = onOrderedListClick,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_format_list_numbered),
                contentDescription = null,
                tint = formatToolbarContentColor(activeMarks.orderedList, enabled),
                modifier = Modifier.size(18.dp),
            )
        }
        NoteFormatToolbarDivider()
        NoteFormatToolbarButton(
            label = stringResource(R.string.notebook_toolbar_link),
            enabled = enabled,
            selected = activeMarks.link,
            onClick = onLinkClick,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_format_link),
                contentDescription = null,
                tint = formatToolbarContentColor(activeMarks.link, enabled),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun formatToolbarContentColor(selected: Boolean, enabled: Boolean): Color =
    when {
        !enabled -> HomeTextPrimary.copy(alpha = 0.38f)
        selected -> LoginCopper
        else -> HomeTextPrimary
    }

@Composable
private fun NoteFormatToolbarDivider() {
    Spacer(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .width(1.dp)
            .height(20.dp)
            .background(HomeCardBorder),
    )
}

@Composable
private fun NoteFormatToolbarButton(
    label: String,
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(ToolbarButtonSize)
            .focusProperties { canFocus = false }
            .semantics {
                contentDescription = label
                this.selected = selected
            }
            .then(
                if (selected) {
                    Modifier
                        .background(ToolbarActiveBackground, ToolbarActiveShape)
                        .border(1.dp, ToolbarActiveBorder, ToolbarActiveShape)
                } else {
                    Modifier
                },
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Preview(showBackground = true, widthDp = 360, backgroundColor = 0xFFF7F1E6)
@Composable
private fun NoteContentFormatToolbarPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        NoteContentFormatToolbar(
            activeMarks = NotebookToolbarMarks(bold = true, link = true),
            onBoldClick = {},
            onItalicClick = {},
            onBulletListClick = {},
            onOrderedListClick = {},
            onLinkClick = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}
