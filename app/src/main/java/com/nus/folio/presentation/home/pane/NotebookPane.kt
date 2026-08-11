package com.nus.folio.presentation.home.pane

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nus.folio.presentation.home.NotebookSaveStatus
import com.nus.folio.presentation.home.notebook.NotebookEditor
import com.nus.folio.presentation.home.notebook.NotebookPaneSkeleton
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeBackground

@Composable
internal fun NotebookPane(
    content: String,
    spaceTitle: String,
    saveStatus: NotebookSaveStatus,
    isLoadingNotebook: Boolean,
    isLoadingNotes: Boolean,
    onContentChange: (String) -> Unit,
    onRetrySave: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when {
        isLoadingNotebook || isLoadingNotes -> {
            NotebookPaneSkeleton(
                showNotesSidebar = isLoadingNotes,
                modifier = modifier.fillMaxSize(),
            )
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
                Synthesize key arguments from primary sources and identify evidence gaps.
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
