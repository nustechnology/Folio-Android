package com.nus.folio.presentation.home.pane

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.presentation.home.NotebookSaveStatus
import com.nus.folio.presentation.home.notebook.NotebookEditor
import com.nus.folio.presentation.home.notebook.NotebookPaneSkeleton
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeBackground
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeTextSecondary

@Composable
internal fun NotebookPane(
    content: String,
    spaceTitle: String,
    saveStatus: NotebookSaveStatus,
    isLoadingNotebook: Boolean,
    isLoadingNotes: Boolean,
    notebookError: String? = null,
    onContentChange: (String) -> Unit,
    onRetrySave: () -> Unit = {},
    onRetryLoad: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when {
        isLoadingNotebook || isLoadingNotes -> {
            NotebookPaneSkeleton(
                showNotesSidebar = isLoadingNotes,
                modifier = modifier.fillMaxSize(),
            )
        }
        notebookError != null -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = notebookError.ifBlank {
                            stringResource(R.string.home_error_generic)
                        },
                        color = HomeTextSecondary,
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.home_retry),
                        modifier = Modifier.clickable(onClick = onRetryLoad),
                        color = HomeHeader,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        else -> {
            NotebookEditor(
                content = content,
                spaceTitle = spaceTitle,
                saveStatus = saveStatus,
                onContentChange = onContentChange,
                onRetrySave = onRetrySave,
                modifier = modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Notebook — editor")
@Composable
private fun NotebookPaneEditorPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        NotebookPane(
            content = """
                # Title
                Dissertation Research

                ## Research Objective
                Primary research archive for doctoral thesis
            """.trimIndent(),
            spaceTitle = "Dissertation Research",
            saveStatus = NotebookSaveStatus.SAVED,
            isLoadingNotebook = false,
            isLoadingNotes = false,
            onContentChange = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Notebook — loading")
@Composable
private fun NotebookPaneLoadingPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        NotebookPane(
            content = "",
            spaceTitle = "Dissertation Research",
            saveStatus = NotebookSaveStatus.IDLE,
            isLoadingNotebook = true,
            isLoadingNotes = false,
            onContentChange = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Notebook — load error")
@Composable
private fun NotebookPaneErrorPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        NotebookPane(
            content = "",
            spaceTitle = "Dissertation Research",
            saveStatus = NotebookSaveStatus.IDLE,
            isLoadingNotebook = false,
            isLoadingNotes = false,
            notebookError = "",
            onContentChange = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}
