package com.nus.folio.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nus.folio.R
import com.nus.folio.components.BouncingDotsIndicator
import com.nus.folio.components.FolioSearchField
import com.nus.folio.components.FolioToastHost
import com.nus.folio.components.FolioToastStyle
import com.nus.folio.components.FolioToastVisuals
import com.nus.folio.components.ItemOptionAction
import com.nus.folio.components.ItemOptionStyle
import com.nus.folio.components.ItemOptionsBottomSheet
import com.nus.folio.components.rememberFolioToastHostState
import com.nus.folio.di.LocalAppContainer
import com.nus.folio.domain.model.AskTopic
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceFilter
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.presentation.home.bottomsheet.AddNoteBottomSheet
import com.nus.folio.presentation.home.bottomsheet.AddSourceBottomSheet
import com.nus.folio.presentation.home.bottomsheet.AnswerScopeBottomSheet
import com.nus.folio.presentation.home.bottomsheet.ConversationBottomSheet
import com.nus.folio.presentation.home.bottomsheet.ConvertNoteBottomSheet
import com.nus.folio.presentation.home.bottomsheet.DeleteConfirmationBottomSheet
import com.nus.folio.presentation.home.bottomsheet.EditNoteBottomSheet
import com.nus.folio.presentation.home.bottomsheet.EditSourceBottomSheet
import com.nus.folio.presentation.home.bottomsheet.ExportNotebookBottomSheet
import com.nus.folio.presentation.home.bottomsheet.SourceProcessingBottomSheet
import com.nus.folio.presentation.home.bottomsheet.ViewNoteBottomSheet
import com.nus.folio.presentation.home.pane.AskPane
import com.nus.folio.presentation.home.pane.NotebookPane
import com.nus.folio.presentation.home.pane.NotesPane
import com.nus.folio.presentation.home.pane.SourcesPane
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeBackground
import com.nus.folio.ui.theme.HomeHeader
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    spaceId: String,
    spaceTitle: String,
    onNavigateBack: () -> Unit,
    onNavigateToSourceDetail: (sourceId: String) -> Unit,
    onSignOut: () -> Unit,
    initialTab: HomeTab? = null,
    onInitialTabHandled: () -> Unit = {},
    initialAskSourceId: String? = null,
    onInitialAskSourceHandled: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            spaceId = spaceId,
            spaceTitle = spaceTitle,
            getSourcesUseCase = LocalAppContainer.current.getSourcesUseCase,
            updateSourceUseCase = LocalAppContainer.current.updateSourceUseCase,
            deleteSourceUseCase = LocalAppContainer.current.deleteSourceUseCase,
            getAskTopicsUseCase = LocalAppContainer.current.getAskTopicsUseCase,
            getNotesUseCase = LocalAppContainer.current.getNotesUseCase,
            updateNoteUseCase = LocalAppContainer.current.updateNoteUseCase,
            deleteNoteUseCase = LocalAppContainer.current.deleteNoteUseCase,
        ),
    ),
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSourceSheet by remember { mutableStateOf(false) }
    var showAddNoteSheet by remember { mutableStateOf(false) }
    var showConversationSheet by remember { mutableStateOf(false) }
    var showAnswerScopeSheet by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    val toastHostState = rememberFolioToastHostState()

    LaunchedEffect(uiState.userMessage) {
        val message = uiState.userMessage ?: return@LaunchedEffect
        toastHostState.showToast(message.toHomeToastVisuals(context))
        viewModel.onUserMessageShown()
    }

    LaunchedEffect(initialTab) {
        val tab = initialTab ?: return@LaunchedEffect
        viewModel.onTabSelected(tab)
        onInitialTabHandled()
    }

    LaunchedEffect(initialAskSourceId) {
        val sourceId = initialAskSourceId ?: return@LaunchedEffect
        viewModel.onAskSourceSelected(sourceId)
        onInitialAskSourceHandled()
    }

    LaunchedEffect(uiState.openSourceDetailId) {
        val sourceId = uiState.openSourceDetailId ?: return@LaunchedEffect
        onNavigateToSourceDetail(sourceId)
        viewModel.onOpenSourceDetailHandled()
    }

    Box(modifier = modifier.fillMaxSize()) {
        HomeContent(
            uiState = uiState,
            onRetry = viewModel::loadSources,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onFilterSelected = viewModel::onFilterSelected,
            onNoteFilterSelected = viewModel::onNoteFilterSelected,
            onTabSelected = viewModel::onTabSelected,
            onAddClick = {
                when (uiState.selectedTab) {
                    HomeTab.NOTES -> showAddNoteSheet = true
                    HomeTab.ASK -> showConversationSheet = true
                    HomeTab.SOURCES -> showAddSourceSheet = true
                    HomeTab.NOTEBOOK -> viewModel.onNotebookAddClick()
                }
            },
            onBackClick = onNavigateBack,
            onAskSubmit = viewModel::onAskSubmit,
            onScopeChipClick = { showAnswerScopeSheet = true },
            onSourceEditClick = viewModel::onEditSourceClick,
            onSourceDeleteClick = viewModel::onDeleteSourceClick,
            onSourceClick = viewModel::onSourceClick,
            onNoteClick = viewModel::onNoteClick,
            onNoteMoreClick = viewModel::onNoteOptionsClick,
            onSignOut = { showSignOutConfirm = true },
            modifier = Modifier.fillMaxSize(),
        )

        FolioToastHost(
            hostState = toastHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp),
        )

        if (uiState.isOpeningSource) {
            SourceOpeningScreen()
        }

        if (showAddSourceSheet) {
            AddSourceBottomSheet(
                onDismiss = { showAddSourceSheet = false },
                onSubmit = { draft ->
                    showAddSourceSheet = false
                    viewModel.onAddSourceSubmit(draft)
                },
            )
        }

        uiState.processingSourceTitle?.let { title ->
            SourceProcessingBottomSheet(
                sourceTitle = title,
                onDismiss = viewModel::onSourceProcessingDismiss,
                onOpenSource = viewModel::onSourceProcessingOpenSource,
                onAsk = viewModel::onSourceProcessingAsk,
            )
        }

        if (showAddNoteSheet) {
            AddNoteBottomSheet(
                onDismiss = { showAddNoteSheet = false },
                onSubmit = { title, content ->
                    viewModel.onAddNoteSubmit(title, content)
                },
            )
        }

        if (showConversationSheet) {
            ConversationBottomSheet(
                onDismiss = { showConversationSheet = false },
            )
        }

        if (showAnswerScopeSheet) {
            AnswerScopeBottomSheet(
                selectedScope = uiState.askScope,
                sourceCount = uiState.allCount,
                currentSourceTitle = uiState.askSourceTitle(),
                onScopeSelected = viewModel::onAskScopeSelected,
                onDismiss = { showAnswerScopeSheet = false },
            )
        }

        uiState.editingSource?.let { source ->
            EditSourceBottomSheet(
                source = source,
                onDismiss = viewModel::onEditSourceDismiss,
                onSave = viewModel::onEditSourceSave,
            )
        }

        uiState.deletingSource?.let {
            DeleteConfirmationBottomSheet(
                onDismiss = viewModel::onDeleteSourceDismiss,
                onConfirm = viewModel::onDeleteSourceConfirm,
            )
        }

        if (uiState.editingNote == null &&
            uiState.deletingNote == null &&
            uiState.convertingNote == null
        ) {
            uiState.viewingNote?.let { note ->
                ViewNoteBottomSheet(
                    note = note,
                    onDismiss = viewModel::onViewNoteDismiss,
                    onConvertClick = viewModel::onConvertNoteClick,
                    onEditClick = viewModel::onEditNoteClick,
                )
            }
        }

        uiState.editingNote?.let { note ->
            EditNoteBottomSheet(
                note = note,
                onDismiss = viewModel::onEditNoteDismiss,
                onSave = viewModel::onEditNoteSave,
                onDelete = viewModel::onDeleteNoteClick,
            )
        }

        uiState.convertingNote?.let { note ->
            ConvertNoteBottomSheet(
                note = note,
                onDismiss = viewModel::onConvertNoteDismiss,
                onCreateSource = viewModel::onConvertNoteCreate,
            )
        }

        uiState.deletingNote?.let {
            DeleteConfirmationBottomSheet(
                titleRes = R.string.note_delete_title,
                messageRes = R.string.note_delete_message,
                onDismiss = viewModel::onDeleteNoteDismiss,
                onConfirm = viewModel::onDeleteNoteConfirm,
            )
        }

        if (showSignOutConfirm) {
            DeleteConfirmationBottomSheet(
                titleRes = R.string.account_sign_out_title,
                messageRes = R.string.account_sign_out_message,
                confirmLabelRes = R.string.account_sign_out,
                onDismiss = { showSignOutConfirm = false },
                onConfirm = {
                    scope.launch {
                        container.clearAuthSessionUseCase()
                        onSignOut()
                    }
                },
            )
        }

        uiState.optionsNote?.let { note ->
            ItemOptionsBottomSheet(
                title = note.title,
                actions = listOf(
                    ItemOptionAction(
                        label = stringResource(R.string.note_options_view),
                        onClick = viewModel::onViewNoteClick,
                    ),
                    ItemOptionAction(
                        label = stringResource(R.string.note_options_edit),
                        onClick = viewModel::onEditNoteClick,
                    ),
                    ItemOptionAction(
                        label = stringResource(R.string.note_options_convert),
                        onClick = viewModel::onConvertNoteClick,
                    ),
                    ItemOptionAction(
                        label = stringResource(R.string.note_options_delete),
                        style = ItemOptionStyle.Destructive,
                        onClick = viewModel::onDeleteNoteClick,
                    ),
                ),
                onDismiss = viewModel::onNoteOptionsDismiss,
            )
        }

        if (uiState.showNotebookActions) {
            ItemOptionsBottomSheet(
                title = stringResource(R.string.notebook_actions_title),
                actions = listOf(
                    ItemOptionAction(
                        label = stringResource(R.string.notebook_actions_copy),
                        onClick = viewModel::onCopyNotebookClick,
                    ),
                    ItemOptionAction(
                        label = stringResource(R.string.notebook_actions_export),
                        onClick = viewModel::onExportNotebookClick,
                    ),
                ),
                onDismiss = viewModel::onNotebookActionsDismiss,
            )
        }

        if (uiState.showNotebookExport) {
            ExportNotebookBottomSheet(
                onDismiss = viewModel::onNotebookExportDismiss,
                onExport = viewModel::onNotebookExportConfirm,
            )
        }
    }
}

@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (SourceFilter) -> Unit,
    onNoteFilterSelected: (NoteFilter) -> Unit,
    onTabSelected: (HomeTab) -> Unit,
    onAddClick: () -> Unit,
    onBackClick: () -> Unit,
    onAskSubmit: () -> Unit,
    onScopeChipClick: () -> Unit,
    onSourceEditClick: (Source) -> Unit,
    onSourceDeleteClick: (Source) -> Unit,
    onSourceClick: (Source) -> Unit,
    onNoteClick: (Note) -> Unit,
    onNoteMoreClick: (Note) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HomeHeader)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 16.dp),
            ) {
                HomeHeaderRow(
                    selectedTab = uiState.selectedTab,
                    spaceTitle = uiState.spaceTitle,
                    onBackClick = onBackClick,
                    onAddClick = onAddClick,
                )
                if (
                    uiState.selectedTab == HomeTab.SOURCES ||
                    uiState.selectedTab == HomeTab.NOTES
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    FolioSearchField(
                        query = uiState.searchQuery,
                        onQueryChange = onSearchQueryChange,
                        placeholder = stringResource(
                            if (uiState.selectedTab == HomeTab.NOTES) {
                                R.string.home_search_notes
                            } else {
                                R.string.home_search_sources
                            },
                        ),
                    )
                }
            }

            when (uiState.selectedTab) {
                HomeTab.SOURCES -> SourcesPane(
                    uiState = uiState,
                    onRetry = onRetry,
                    onAddClick = onAddClick,
                    onFilterSelected = onFilterSelected,
                    onSourceEditClick = onSourceEditClick,
                    onSourceDeleteClick = onSourceDeleteClick,
                    onSourceClick = onSourceClick,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = HomeBottomNavClearance),
                )
                HomeTab.ASK -> AskPane(
                    uiState = uiState,
                    onRetry = onRetry,
                    onAskSubmit = onAskSubmit,
                    onScopeChipClick = onScopeChipClick,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = HomeBottomNavClearance),
                )
                HomeTab.NOTES -> NotesPane(
                    uiState = uiState,
                    onRetry = onRetry,
                    onAddClick = onAddClick,
                    onFilterSelected = onNoteFilterSelected,
                    onNoteClick = onNoteClick,
                    onNoteMoreClick = onNoteMoreClick,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = HomeBottomNavClearance),
                )
                HomeTab.NOTEBOOK -> NotebookPane(
                    onSignOut = onSignOut,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = HomeBottomNavClearance),
                )
            }
        }

        HomeBottomNav(
            selectedTab = uiState.selectedTab,
            onTabSelected = onTabSelected,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SourceOpeningScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        BouncingDotsIndicator()
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Sources — populated")
@Composable
private fun HomeContentPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        HomeContent(
            uiState = HomeUiState(
                visibleSources = listOf(
                    Source("1", "Alan Turing: Computing Machinery", SourceType.PDF, "Alan Turing", "Added 2d ago", SourceStatus.READY, "1"),
                    Source("2", "The Origins of Totalitarianism", SourceType.PDF, "Hannah Arendt", "Added 2d ago", SourceStatus.READY, "1"),
                    Source("3", "Weapons of Math Destruction", SourceType.BOOK, "Cathy O'Neil", "Added 2d ago", SourceStatus.PROCESSING, "1"),
                    Source("4", "The Age of Surveillance Capitalism", SourceType.PDF, "Shoshana Zuboff", "Added 2d ago", SourceStatus.FAILED, "1"),
                    Source("5", "Attention Is All You Need", SourceType.PDF, "Vaswani et al.", "Added 2d ago", SourceStatus.READY, "1"),
                ),
                spaceTitle = "Dissertation Research",
                allCount = 128,
            ),
            onRetry = {},
            onSearchQueryChange = {},
            onFilterSelected = {},
            onNoteFilterSelected = {},
            onTabSelected = {},
            onAddClick = {},
            onBackClick = {},
            onAskSubmit = {},
            onScopeChipClick = {},
            onSourceEditClick = {},
            onSourceDeleteClick = {},
            onSourceClick = {},
            onNoteClick = {},
            onNoteMoreClick = {},
            onSignOut = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Sources — empty")
@Composable
private fun HomeContentEmptyPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        HomeContent(
            uiState = HomeUiState(
                selectedTab = HomeTab.SOURCES,
                visibleSources = emptyList(),
            ),
            onRetry = {},
            onSearchQueryChange = {},
            onFilterSelected = {},
            onNoteFilterSelected = {},
            onTabSelected = {},
            onAddClick = {},
            onBackClick = {},
            onAskSubmit = {},
            onScopeChipClick = {},
            onSourceEditClick = {},
            onSourceDeleteClick = {},
            onSourceClick = {},
            onNoteClick = {},
            onNoteMoreClick = {},
            onSignOut = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Ask — question pane")
@Composable
private fun HomeAskPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        HomeContent(
            uiState = HomeUiState(
                selectedTab = HomeTab.ASK,
                visibleAskTopics = listOf(
                    AskTopic("1a", "Core dissertation arguments", 4, 2, "1"),
                    AskTopic("1b", "Turing and modern AI", 3, 1, "1"),
                ),
                spaceTitle = "Dissertation Research",
            ),
            onRetry = {},
            onSearchQueryChange = {},
            onFilterSelected = {},
            onNoteFilterSelected = {},
            onTabSelected = {},
            onAddClick = {},
            onBackClick = {},
            onAskSubmit = {},
            onScopeChipClick = {},
            onSourceEditClick = {},
            onSourceDeleteClick = {},
            onSourceClick = {},
            onNoteClick = {},
            onNoteMoreClick = {},
            onSignOut = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Notes — list")
@Composable
private fun HomeNotesPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        HomeContent(
            uiState = HomeUiState(
                selectedTab = HomeTab.NOTES,
                visibleNotes = listOf(
                    Note(
                        id = "1",
                        title = "Research Question Draft",
                        content = "How do informal transit networks reshape access in mid-sized cities?",
                        project = "Urban Mobility",
                        updatedLabel = "Updated 1d ago",
                        isPinned = true,
                        spaceId = "1",
                        origin = NoteOrigin.USER_CREATED,
                    ),
                    Note(
                        id = "2",
                        title = "Literature Review Outline",
                        content = "Map debates on machine intelligence, imitation games, and measurement.",
                        project = "Dissertation Research",
                        updatedLabel = "Updated 2d ago",
                        isPinned = true,
                        spaceId = "1",
                        origin = NoteOrigin.USER_CREATED,
                    ),
                    Note(
                        id = "3",
                        title = "Turing Test — Key Takeaways",
                        content = "The imitation game reframes intelligence as observable linguistic behavior.",
                        project = "Dissertation Research",
                        updatedLabel = "Updated 3d ago",
                        isPinned = false,
                        spaceId = "1",
                        origin = NoteOrigin.SAVED_ANSWER,
                        citationCount = 4,
                    ),
                    Note(
                        id = "4",
                        title = "Policy Implications",
                        content = "Zoning reform alone underestimates last-mile coordination costs.",
                        project = "Urban Mobility",
                        updatedLabel = "Updated 4d ago",
                        isPinned = false,
                        spaceId = "1",
                        origin = NoteOrigin.SAVED_ANSWER,
                        citationCount = 2,
                    ),
                    Note(
                        id = "5",
                        title = "Teaching Prep — Week 7",
                        content = "Seminar prompts on archival silence and source criticism.",
                        project = "Urban Mobility",
                        updatedLabel = "Updated 5d ago",
                        isPinned = false,
                        spaceId = "1",
                        origin = NoteOrigin.USER_CREATED,
                    ),
                ),
                spaceTitle = "Dissertation Research",
                notesAllCount = 32,
                notesPinnedCount = 8,
                notesUnfiledCount = 4,
            ),
            onRetry = {},
            onSearchQueryChange = {},
            onFilterSelected = {},
            onNoteFilterSelected = {},
            onTabSelected = {},
            onAddClick = {},
            onBackClick = {},
            onAskSubmit = {},
            onScopeChipClick = {},
            onSourceEditClick = {},
            onSourceDeleteClick = {},
            onSourceClick = {},
            onNoteClick = {},
            onNoteMoreClick = {},
            onSignOut = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Notes — empty")
@Composable
private fun HomeNotesEmptyPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        HomeContent(
            uiState = HomeUiState(
                selectedTab = HomeTab.NOTES,
                visibleNotes = emptyList(),
                spaceTitle = "Dissertation Research",
                notesAllCount = 0,
                notesPinnedCount = 0,
                notesUnfiledCount = 0,
            ),
            onRetry = {},
            onSearchQueryChange = {},
            onFilterSelected = {},
            onNoteFilterSelected = {},
            onTabSelected = {},
            onAddClick = {},
            onBackClick = {},
            onAskSubmit = {},
            onScopeChipClick = {},
            onSourceEditClick = {},
            onSourceDeleteClick = {},
            onSourceClick = {},
            onNoteClick = {},
            onNoteMoreClick = {},
            onSignOut = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Notes — search empty")
@Composable
private fun HomeNotesSearchEmptyPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        HomeContent(
            uiState = HomeUiState(
                selectedTab = HomeTab.NOTES,
                searchQuery = "xyz",
                visibleNotes = emptyList(),
                spaceTitle = "Dissertation Research",
                notesAllCount = 32,
                notesPinnedCount = 8,
                notesUnfiledCount = 4,
            ),
            onRetry = {},
            onSearchQueryChange = {},
            onFilterSelected = {},
            onNoteFilterSelected = {},
            onTabSelected = {},
            onAddClick = {},
            onBackClick = {},
            onAskSubmit = {},
            onScopeChipClick = {},
            onSourceEditClick = {},
            onSourceDeleteClick = {},
            onSourceClick = {},
            onNoteClick = {},
            onNoteMoreClick = {},
            onSignOut = {},
        )
    }
}

private fun HomeUserMessage.toHomeToastVisuals(context: android.content.Context): FolioToastVisuals {
    val messageRes = when (this) {
        HomeUserMessage.SOURCE_UPDATED -> R.string.toast_source_updated
        HomeUserMessage.SOURCE_DELETED -> R.string.toast_source_deleted
        HomeUserMessage.SOURCE_UPDATE_FAILED -> R.string.toast_source_update_failed
        HomeUserMessage.SOURCE_DELETE_FAILED -> R.string.toast_source_delete_failed
        HomeUserMessage.NOTE_UPDATED -> R.string.toast_note_updated
        HomeUserMessage.NOTE_DELETED -> R.string.toast_note_deleted
        HomeUserMessage.ASK_NOT_SUPPORTED -> R.string.home_ask_not_supported
        HomeUserMessage.ADD_NOTE_NOT_SUPPORTED -> R.string.add_note_not_supported
        HomeUserMessage.COPY_NOTEBOOK_NOT_SUPPORTED -> R.string.notebook_copy_not_supported
        HomeUserMessage.EXPORT_NOTEBOOK_NOT_SUPPORTED -> R.string.notebook_export_not_supported
        HomeUserMessage.EDIT_NOTE_NOT_SUPPORTED -> R.string.note_edit_not_supported
        HomeUserMessage.CONVERT_NOTE_NOT_SUPPORTED -> R.string.note_convert_not_supported
        HomeUserMessage.DELETE_NOTE_NOT_SUPPORTED -> R.string.note_delete_not_supported
    }
    val style = when (this) {
        HomeUserMessage.SOURCE_UPDATED,
        HomeUserMessage.SOURCE_DELETED,
        HomeUserMessage.NOTE_UPDATED,
        HomeUserMessage.NOTE_DELETED,
        -> FolioToastStyle.Success
        HomeUserMessage.ASK_NOT_SUPPORTED,
        -> FolioToastStyle.Info
        HomeUserMessage.ADD_NOTE_NOT_SUPPORTED,
        HomeUserMessage.COPY_NOTEBOOK_NOT_SUPPORTED,
        HomeUserMessage.EXPORT_NOTEBOOK_NOT_SUPPORTED,
        HomeUserMessage.EDIT_NOTE_NOT_SUPPORTED,
        HomeUserMessage.CONVERT_NOTE_NOT_SUPPORTED,
        -> FolioToastStyle.Warning
        HomeUserMessage.SOURCE_UPDATE_FAILED,
        HomeUserMessage.SOURCE_DELETE_FAILED,
        HomeUserMessage.DELETE_NOTE_NOT_SUPPORTED,
        -> FolioToastStyle.Error
    }
    return FolioToastVisuals(
        message = context.getString(messageRes),
        style = style,
    )
}
