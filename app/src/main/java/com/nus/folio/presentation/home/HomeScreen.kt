package com.nus.folio.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nus.folio.R
import com.nus.folio.components.FolioSearchField
import com.nus.folio.components.FolioToastHost
import com.nus.folio.components.FolioToastStyle
import com.nus.folio.components.FolioToastVisuals
import com.nus.folio.components.rememberFolioToastHostState
import com.nus.folio.di.LocalAppContainer
import com.nus.folio.domain.model.AskTopic
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.domain.model.NoteSort
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceFilter
import com.nus.folio.domain.model.SourceSort
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
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
    onNavigateToSourceDetail: (sourceId: String, highlightText: String?) -> Unit,
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
            createSourceUseCase = LocalAppContainer.current.createSourceUseCase,
            observeSourceProcessingUseCase = LocalAppContainer.current.observeSourceProcessingUseCase,
            updateSourceUseCase = LocalAppContainer.current.updateSourceUseCase,
            deleteSourceUseCase = LocalAppContainer.current.deleteSourceUseCase,
            getSourceDetailUseCase = LocalAppContainer.current.getSourceDetailUseCase,
            getAskTopicsUseCase = LocalAppContainer.current.getAskTopicsUseCase,
            getAskSuggestionsUseCase = LocalAppContainer.current.getAskSuggestionsUseCase,
            streamAskAnswerUseCase = LocalAppContainer.current.streamAskAnswerUseCase,
            getNotesUseCase = LocalAppContainer.current.getNotesUseCase,
            getNoteDetailUseCase = LocalAppContainer.current.getNoteDetailUseCase,
            createNoteUseCase = LocalAppContainer.current.createNoteUseCase,
            updateNoteUseCase = LocalAppContainer.current.updateNoteUseCase,
            deleteNoteUseCase = LocalAppContainer.current.deleteNoteUseCase,
            sourceFileBytesReader = LocalAppContainer.current.sourceFileBytesReader,
            refreshAuthSessionUseCase = LocalAppContainer.current.refreshAuthSessionUseCase,
            getCurrentSessionUseCase = LocalAppContainer.current.getCurrentSessionUseCase,
            retrySourceUseCase = LocalAppContainer.current.retrySourceUseCase,
        ),
    ),
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddNoteSheet by remember { mutableStateOf(false) }
    var showConversationSheet by remember { mutableStateOf(false) }
    var showAnswerScopeSheet by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    val toastHostState = rememberFolioToastHostState()

    LaunchedEffect(uiState.requiresReauth) {
        if (uiState.requiresReauth) onSignOut()
    }

    LaunchedEffect(uiState.userMessage) {
        val message = uiState.userMessage ?: return@LaunchedEffect
        toastHostState.showToast(message.toHomeToastVisuals(context))
        viewModel.onUserMessageShown()
    }

    LaunchedEffect(uiState.infoToast) {
        val message = uiState.infoToast ?: return@LaunchedEffect
        toastHostState.showToast(
            FolioToastVisuals(
                title = message,
                style = FolioToastStyle.Info,
            ),
        )
        viewModel.onInfoToastShown()
    }

    LaunchedEffect(uiState.actionError) {
        val error = uiState.actionError ?: return@LaunchedEffect
        toastHostState.showToast(
            FolioToastVisuals(
                title = context.getString(error.toStringRes()),
                style = FolioToastStyle.Error,
            ),
        )
        viewModel.onActionErrorShown()
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
        onNavigateToSourceDetail(sourceId, uiState.openSourceDetailHighlight)
        viewModel.onOpenSourceDetailHandled()
    }

    Box(modifier = modifier.fillMaxSize()) {
        HomeContent(
            uiState = uiState,
            onRetry = viewModel::onRetry,
            onRefreshSources = viewModel::onRefreshSources,
            onRefreshNotes = viewModel::onRefreshNotes,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onFilterSelected = viewModel::onFilterSelected,
            onFilterSortClick = viewModel::onFilterSortClick,
            onNoteFilterSelected = viewModel::onNoteFilterSelected,
            onTabSelected = viewModel::onTabSelected,
            onAddClick = {
                when (uiState.selectedTab) {
                    HomeTab.NOTES -> showAddNoteSheet = true
                    HomeTab.ASK -> showConversationSheet = true
                    HomeTab.SOURCES -> viewModel.onAddSourceClick()
                    HomeTab.NOTEBOOK -> viewModel.onNotebookAddClick()
                }
            },
            onBackClick = onNavigateBack,
            onAskSubmit = viewModel::onAskSubmit,
            onAskStop = viewModel::onAskStop,
            onAskUserEnterAnimationFinished = viewModel::onAskUserEnterAnimationFinished,
            onScopeChipClick = { showAnswerScopeSheet = true },
            onAskAddSourceClick = viewModel::onAddSourceClick,
            onAskSaveAsNote = viewModel::onAskSaveAsNote,
            onAskFeedback = viewModel::onAskFeedback,
            onAskCitationClick = viewModel::onAskCitationClick,
            onSourceMoreClick = viewModel::onSourceOptionsClick,
            onSourceClick = viewModel::onSourceClick,
            onNoteClick = viewModel::onNoteClick,
            onNoteMoreClick = viewModel::onNoteOptionsClick,
            onLoadMoreNotes = viewModel::onLoadMoreNotes,
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

        HomeOverlaySheets(
            uiState = uiState,
            showAddNoteSheet = showAddNoteSheet,
            showConversationSheet = showConversationSheet,
            showAnswerScopeSheet = showAnswerScopeSheet,
            showSignOutConfirm = showSignOutConfirm,
            onAddSourceSheetDismiss = viewModel::onAddSourceSheetDismiss,
            onAddSourceSubmit = viewModel::onAddSourceSubmit,
            onSortSelected = viewModel::onSortSelected,
            onNoteSortSelected = viewModel::onNoteSortSelected,
            onSortSheetDismiss = viewModel::onSortSheetDismiss,
            onSourceProcessingDismiss = viewModel::onSourceProcessingDismiss,
            onSourceProcessingOpenSource = viewModel::onSourceProcessingOpenSource,
            onSourceProcessingAsk = viewModel::onSourceProcessingAsk,
            onSourceProcessingRetry = viewModel::onSourceProcessingRetry,
            onAddNoteSheetDismiss = { showAddNoteSheet = false },
            onAddNoteSubmit = { title, content ->
                viewModel.onAddNoteSubmit(title, content)
            },
            onAskSaveAsNoteDismiss = viewModel::onAskSaveAsNoteDismiss,
            onAskSaveAsNoteConfirm = viewModel::onAskSaveAsNoteConfirm,
            onAskCitationClick = viewModel::onAskCitationClick,
            onConversationSheetDismiss = { showConversationSheet = false },
            onNewConversation = viewModel::onNewConversation,
            onAskScopeOptionSelected = viewModel::onAskScopeOptionSelected,
            onAnswerScopeSheetDismiss = { showAnswerScopeSheet = false },
            onCitationPreviewDismiss = viewModel::onCitationPreviewDismiss,
            onCitationOpenInSource = viewModel::onCitationOpenInSource,
            onEditSourceDismiss = viewModel::onEditSourceDismiss,
            onEditSourceSave = viewModel::onEditSourceSave,
            onDeleteSourceDismiss = viewModel::onDeleteSourceDismiss,
            onDeleteSourceConfirm = viewModel::onDeleteSourceConfirm,
            onViewNoteDismiss = viewModel::onViewNoteDismiss,
            onConvertNoteClick = viewModel::onConvertNoteClick,
            onEditNoteClick = viewModel::onEditNoteClick,
            onEditNoteDismiss = viewModel::onEditNoteDismiss,
            onEditNoteSave = viewModel::onEditNoteSave,
            onDeleteNoteClick = viewModel::onDeleteNoteClick,
            onConvertNoteDismiss = viewModel::onConvertNoteDismiss,
            onConvertNoteCreate = viewModel::onConvertNoteCreate,
            onDeleteNoteDismiss = viewModel::onDeleteNoteDismiss,
            onDeleteNoteConfirm = viewModel::onDeleteNoteConfirm,
            onSignOutConfirmDismiss = { showSignOutConfirm = false },
            onSignOutConfirm = {
                scope.launch {
                    container.clearAuthSessionUseCase()
                    onSignOut()
                }
            },
            onEditSourceClick = viewModel::onEditSourceClick,
            onDeleteSourceClick = viewModel::onDeleteSourceClick,
            onSourceOptionsDismiss = viewModel::onSourceOptionsDismiss,
            onViewNoteClick = viewModel::onViewNoteClick,
            onNoteOptionsDismiss = viewModel::onNoteOptionsDismiss,
            onCopyNotebookClick = viewModel::onCopyNotebookClick,
            onExportNotebookClick = viewModel::onExportNotebookClick,
            onNotebookActionsDismiss = viewModel::onNotebookActionsDismiss,
            onNotebookExportDismiss = viewModel::onNotebookExportDismiss,
            onNotebookExportConfirm = viewModel::onNotebookExportConfirm,
        )
    }
}

@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    onRefreshSources: () -> Unit = {},
    onRefreshNotes: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (SourceFilter) -> Unit,
    onFilterSortClick: () -> Unit = {},
    onNoteFilterSelected: (NoteFilter) -> Unit,
    onTabSelected: (HomeTab) -> Unit,
    onAddClick: () -> Unit,
    onBackClick: () -> Unit,
    onAskSubmit: (String) -> Unit,
    onAskStop: () -> Unit = {},
    onAskUserEnterAnimationFinished: (String) -> Unit = {},
    onScopeChipClick: () -> Unit,
    onAskAddSourceClick: () -> Unit = {},
    onAskSaveAsNote: (String) -> Unit = {},
    onAskFeedback: (String, Boolean) -> Unit = { _, _ -> },
    onAskCitationClick: (com.nus.folio.domain.model.AskCitation) -> Unit = {},
    onSourceMoreClick: (Source) -> Unit,
    onSourceClick: (Source) -> Unit,
    onNoteClick: (Note) -> Unit,
    onNoteMoreClick: (Note) -> Unit,
    onLoadMoreNotes: () -> Unit = {},
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val hideBottomNavForAskInput = isImeVisible && uiState.selectedTab == HomeTab.ASK

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
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
                            modifier = Modifier.weight(1f),
                        )
                        if (uiState.selectedTab == HomeTab.SOURCES) {
                            HomeFilterSortButton(
                                onClick = onFilterSortClick,
                                showActiveIndicator = uiState.selectedSort != SourceSort.DEFAULT,
                            )
                        } else if (uiState.selectedTab == HomeTab.NOTES) {
                            HomeFilterSortButton(
                                onClick = onFilterSortClick,
                                showActiveIndicator = uiState.selectedNoteSort != NoteSort.DEFAULT,
                            )
                        }
                    }
                }
            }

            when (uiState.selectedTab) {
                HomeTab.SOURCES -> SourcesPane(
                    uiState = uiState,
                    onRetry = onRetry,
                    onRefresh = onRefreshSources,
                    onAddClick = onAddClick,
                    onFilterSelected = onFilterSelected,
                    onSourceMoreClick = onSourceMoreClick,
                    onSourceClick = onSourceClick,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = HomeBottomNavClearance),
                )
                HomeTab.ASK -> AskPane(
                    uiState = uiState,
                    onRetry = onRetry,
                    onAskSubmit = onAskSubmit,
                    onAskStop = onAskStop,
                    onScopeChipClick = onScopeChipClick,
                    onAddSourceClick = onAskAddSourceClick,
                    onSaveAsNote = onAskSaveAsNote,
                    onFeedback = onAskFeedback,
                    onCitationClick = onAskCitationClick,
                    onUserEnterAnimationFinished = onAskUserEnterAnimationFinished,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (hideBottomNavForAskInput) {
                                Modifier.padding(bottom = HomeBottomNavClearance + 110.dp )
                            } else {
                                Modifier.padding(bottom = HomeBottomNavClearance )
                            },
                        ),
                )
                HomeTab.NOTES -> NotesPane(
                    uiState = uiState,
                    onRetry = onRetry,
                    onRefresh = onRefreshNotes,
                    onAddClick = onAddClick,
                    onFilterSelected = onNoteFilterSelected,
                    onNoteClick = onNoteClick,
                    onNoteMoreClick = onNoteMoreClick,
                    onLoadMore = onLoadMoreNotes,
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

        if (!hideBottomNavForAskInput) {
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
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Sources — populated")
@Composable
private fun HomeContentPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        HomeContent(
            uiState = HomeUiState(
                visibleSources = listOf(
                    Source("1", "Alan Turing: Computing Machinery", SourceType.FILE, "Alan Turing", "Added 2d ago", SourceStatus.READY, "1", "pdf"),
                    Source("2", "The Origins of Totalitarianism", SourceType.FILE, "Hannah Arendt", "Added 2d ago", SourceStatus.READY, "1", "pdf"),
                    Source("3", "Weapons of Math Destruction", SourceType.BOOK, "Cathy O'Neil", "Added 2d ago", SourceStatus.PROCESSING, "1", "epub"),
                    Source("4", "The Age of Surveillance Capitalism", SourceType.FILE, "Shoshana Zuboff", "Added 2d ago", SourceStatus.FAILED, "1", "pdf"),
                    Source("5", "Attention Is All You Need", SourceType.FILE, "Vaswani et al.", "Added 2d ago", SourceStatus.READY, "1", "pdf"),
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
            onSourceMoreClick = {},
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
            onSourceMoreClick = {},
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
            onSourceMoreClick = {},
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
            onSourceMoreClick = {},
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
            onSourceMoreClick = {},
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
            onSourceMoreClick = {},
            onSourceClick = {},
            onNoteClick = {},
            onNoteMoreClick = {},
            onSignOut = {},
        )
    }
}

private fun HomeActionError.toStringRes(): Int = when (this) {
    HomeActionError.GENERIC -> R.string.home_error_generic
    HomeActionError.NETWORK -> R.string.home_error_network
    HomeActionError.FILE_REQUIRED -> R.string.add_source_file_required
    HomeActionError.FILE_UNSUPPORTED -> R.string.add_source_file_unsupported_format
    HomeActionError.FILE_TOO_LARGE -> R.string.add_source_file_size_exceeded
}
