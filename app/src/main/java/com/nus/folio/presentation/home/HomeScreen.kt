package com.nus.folio.presentation.home

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.nus.folio.components.dismissKeyboardOnTapOutside
import com.nus.folio.components.rememberDismissKeyboardThen
import com.nus.folio.components.rememberFolioToastHostState
import com.nus.folio.di.LocalAppContainer
import com.nus.folio.domain.model.AskConversation
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.domain.model.NoteSort
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceFilter
import com.nus.folio.domain.model.SourceSort
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.presentation.home.bottomsheet.findActivityOrNull
import com.nus.folio.presentation.home.notebook.NotebookClipboardHelper
import com.nus.folio.presentation.home.notebook.NotebookExportHelper
import com.nus.folio.presentation.home.notebook.NotebookPrintHelper
import com.nus.folio.presentation.home.pane.AskPane
import com.nus.folio.presentation.home.pane.NotebookPane
import com.nus.folio.presentation.home.pane.NotesPane
import com.nus.folio.presentation.home.pane.SourcesPane
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeBackground
import com.nus.folio.ui.theme.HomeHeader
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    spaceId: String,
    spaceTitle: String,
    researchObjective: String = "",
    onNavigateBack: () -> Unit,
    onNavigateToSourceDetail: (sourceId: String, highlightText: String?) -> Unit,
    onSignOut: () -> Unit,
    initialTab: HomeTab? = null,
    onInitialTabHandled: () -> Unit = {},
    initialAskSourceId: String? = null,
    onInitialAskSourceHandled: () -> Unit = {},
    initialRefreshSources: Boolean = false,
    onInitialRefreshSourcesHandled: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            spaceId = spaceId,
            spaceTitle = spaceTitle,
            researchObjective = researchObjective,
            getSourcesUseCase = LocalAppContainer.current.getSourcesUseCase,
            createSourceUseCase = LocalAppContainer.current.createSourceUseCase,
            observeSourceProcessingUseCase = LocalAppContainer.current.observeSourceProcessingUseCase,
            updateSourceUseCase = LocalAppContainer.current.updateSourceUseCase,
            deleteSourceUseCase = LocalAppContainer.current.deleteSourceUseCase,
            getAskSuggestionsUseCase = LocalAppContainer.current.getAskSuggestionsUseCase,
            getAskConversationsUseCase = LocalAppContainer.current.getAskConversationsUseCase,
            getAskConversationUseCase = LocalAppContainer.current.getAskConversationUseCase,
            updateAskConversationUseCase = LocalAppContainer.current.updateAskConversationUseCase,
            deleteAskConversationUseCase = LocalAppContainer.current.deleteAskConversationUseCase,
            streamAskAnswerUseCase = LocalAppContainer.current.streamAskAnswerUseCase,
            submitAskFeedbackUseCase = LocalAppContainer.current.submitAskFeedbackUseCase,
            getNotesUseCase = LocalAppContainer.current.getNotesUseCase,
            getNoteDetailUseCase = LocalAppContainer.current.getNoteDetailUseCase,
            createNoteUseCase = LocalAppContainer.current.createNoteUseCase,
            updateNoteUseCase = LocalAppContainer.current.updateNoteUseCase,
            deleteNoteUseCase = LocalAppContainer.current.deleteNoteUseCase,
            convertNoteToSourceUseCase = LocalAppContainer.current.convertNoteToSourceUseCase,
            getNotebookUseCase = LocalAppContainer.current.getNotebookUseCase,
            saveNotebookUseCase = LocalAppContainer.current.saveNotebookUseCase,
            getSpacesUseCase = LocalAppContainer.current.getSpacesUseCase,
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
    var showConversationSheet by remember { mutableStateOf(false) }
    var showAnswerScopeSheet by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    val toastHostState = rememberFolioToastHostState()
    val onAddSourceClick = rememberDismissKeyboardThen(viewModel::onAddSourceClick)
    val onAddNoteClick = rememberDismissKeyboardThen(viewModel::onAddNoteClick)
    val onFilterSortClick = rememberDismissKeyboardThen(viewModel::onFilterSortClick)
    val onNotebookAddClick = rememberDismissKeyboardThen(viewModel::onNotebookAddClick)
    val onExportNotebookClick = rememberDismissKeyboardThen(viewModel::onExportNotebookClick)
    val onOpenConversationSheet = rememberDismissKeyboardThen { showConversationSheet = true }
    val onOpenAnswerScopeSheet = rememberDismissKeyboardThen { showAnswerScopeSheet = true }
    val onOpenSignOutConfirm = rememberDismissKeyboardThen { showSignOutConfirm = true }

    BackHandler(enabled = uiState.isAskChatOpen && uiState.selectedTab == HomeTab.ASK) {
        viewModel.onAskChatBack()
    }

    LaunchedEffect(researchObjective) {
        viewModel.onResearchObjectiveAvailable(researchObjective)
    }

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri ->
        val request = viewModel.uiState.value.pendingNotebookExport
        when {
            uri == null || request == null -> {
                viewModel.onPendingNotebookExportHandled()
            }
            else -> {
                NotebookExportHelper.writeMarkdown(context, uri, request.markdown)
                    .onSuccess { viewModel.onNotebookExportSucceeded() }
                    .onFailure { viewModel.onNotebookExportFailed() }
            }
        }
    }

    LaunchedEffect(uiState.pendingNotebookCopy) {
        val content = uiState.pendingNotebookCopy ?: return@LaunchedEffect
        NotebookClipboardHelper.copy(
            context = context,
            label = context.getString(R.string.home_notebook_title),
            content = content,
        )
        viewModel.onPendingNotebookCopyHandled()
    }

    LaunchedEffect(uiState.pendingNotebookExport, uiState.notebookExportPickerLaunched) {
        val request = uiState.pendingNotebookExport ?: return@LaunchedEffect
        if (uiState.notebookExportPickerLaunched) return@LaunchedEffect
        viewModel.onNotebookExportPickerLaunched()
        exportDocumentLauncher.launch(request.filename)
    }

    // Keep pendingNotebookPrint across async WebView load and activity recreation. Bump request id
    // on configuration change so print relaunches with a new adapter; clear when leaving Home.
    DisposableEffect(Unit) {
        val activity = context.findActivityOrNull()
        onDispose {
            if (activity?.isChangingConfigurations == true) {
                viewModel.onNotebookPrintAdapterInvalidated()
            } else {
                viewModel.onPendingNotebookPrintHandled()
                viewModel.clearSearch()
            }
        }
    }

    LaunchedEffect(uiState.pendingNotebookPrint?.id) {
        val request = uiState.pendingNotebookPrint ?: return@LaunchedEffect
        val session = NotebookPrintHelper.print(
            context = context,
            jobName = uiState.spaceTitle.ifBlank { context.getString(R.string.home_notebook_title) },
            markdown = request.markdown,
            loadId = request.id,
            onSubmitted = { viewModel.onNotebookPrintSubmitted() },
        )
        try {
            awaitCancellation()
        } finally {
            session.cancel()
        }
    }

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

    LaunchedEffect(initialRefreshSources) {
        if (!initialRefreshSources) return@LaunchedEffect
        viewModel.loadSources()
        onInitialRefreshSourcesHandled()
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
            onFilterSortClick = onFilterSortClick,
            onNoteFilterSelected = viewModel::onNoteFilterSelected,
            onTabSelected = viewModel::onTabSelected,
            onAddClick = {
                when (uiState.selectedTab) {
                    HomeTab.NOTES -> onAddNoteClick()
                    HomeTab.ASK -> onOpenConversationSheet()
                    HomeTab.SOURCES -> onAddSourceClick()
                    HomeTab.NOTEBOOK -> onNotebookAddClick()
                }
            },
            onBackClick = {
                if (uiState.isAskChatOpen && uiState.selectedTab == HomeTab.ASK) {
                    viewModel.onAskChatBack()
                } else {
                    onNavigateBack()
                }
            },
            onAskSubmit = viewModel::onAskSubmit,
            onAskStop = viewModel::onAskStop,
            onAskUserEnterAnimationFinished = viewModel::onAskUserEnterAnimationFinished,
            onScopeChipClick = onOpenAnswerScopeSheet,
            onAskAddSourceClick = onAddSourceClick,
            onAskSaveAsNote = viewModel::onAskSaveAsNote,
            onAskFeedback = viewModel::onAskFeedback,
            onAskCitationClick = viewModel::onAskCitationClick,
            onConversationClick = viewModel::onConversationClick,
            onConversationMoreClick = viewModel::onConversationOptionsClick,
            onRefreshConversations = viewModel::onRefreshConversations,
            onRetryConversations = viewModel::onRefreshConversations,
            onNewConversationClick = {
                showConversationSheet = false
                viewModel.onNewConversation()
            },
            onSourceMoreClick = viewModel::onSourceOptionsClick,
            onSourceClick = viewModel::onSourceClick,
            onNoteClick = viewModel::onNoteClick,
            onNoteMoreClick = viewModel::onNoteOptionsClick,
            onLoadMoreSources = viewModel::onLoadMoreSources,
            onLoadMoreNotes = viewModel::onLoadMoreNotes,
            onLoadMoreConversations = viewModel::onLoadMoreConversations,
            onNotebookContentChange = viewModel::onNotebookContentChange,
            onRetryNotebookSave = viewModel::onRetryNotebookSave,
            onRetryNotebookLoad = viewModel::onRetryNotebookLoad,
            onSignOut = onOpenSignOutConfirm,
            modifier = Modifier.fillMaxSize(),
        )

        HomeOverlaySheets(
            uiState = uiState,
            showConversationSheet = showConversationSheet,
            showAnswerScopeSheet = showAnswerScopeSheet,
            showSignOutConfirm = showSignOutConfirm,
            onAddSourceSheetDismiss = viewModel::onAddSourceSheetDismiss,
            onAddSourceSubmit = viewModel::onAddSourceSubmit,
            onAddSourceFileSelected = viewModel::onAddSourceFileSelected,
            onAddSourceFileSelectionFailed = viewModel::onAddSourceFileSelectionFailed,
            onSortSelected = viewModel::onSortSelected,
            onNoteSortSelected = viewModel::onNoteSortSelected,
            onSortSheetDismiss = viewModel::onSortSheetDismiss,
            onSourceProcessingDismiss = viewModel::onSourceProcessingDismiss,
            onSourceProcessingOpenSource = viewModel::onSourceProcessingOpenSource,
            onSourceProcessingAsk = viewModel::onSourceProcessingAsk,
            onSourceProcessingRetry = viewModel::onSourceProcessingRetry,
            onAddNoteSheetDismiss = viewModel::onAddNoteSheetDismiss,
            onAddNoteSubmit = viewModel::onAddNoteSubmit,
            onAskSaveAsNoteDismiss = viewModel::onAskSaveAsNoteDismiss,
            onAskSaveAsNoteConfirm = viewModel::onAskSaveAsNoteConfirm,
            onAskCitationClick = viewModel::onAskCitationClick,
            onConversationSheetDismiss = { showConversationSheet = false },
            onNewConversation = {
                showConversationSheet = false
                viewModel.onNewConversation()
            },
            onConversationOptionsDismiss = viewModel::onConversationOptionsDismiss,
            onRenameConversationClick = viewModel::onRenameConversationClick,
            onRenameConversationDismiss = viewModel::onRenameConversationDismiss,
            onRenameConversationSave = viewModel::onRenameConversationSave,
            onDeleteConversationClick = viewModel::onDeleteConversationClick,
            onDeleteConversationDismiss = viewModel::onDeleteConversationDismiss,
            onDeleteConversationConfirm = viewModel::onDeleteConversationConfirm,
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
            onExportNotebookClick = onExportNotebookClick,
            onNotebookActionsDismiss = viewModel::onNotebookActionsDismiss,
            onNotebookExportDismiss = viewModel::onNotebookExportDismiss,
            onNotebookExportConfirm = viewModel::onNotebookExportConfirm,
        )

        // Drawn last (+ high zIndex) so toasts stay above bottom sheets / scrims.
        FolioToastHost(
            hostState = toastHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp),
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
    onConversationClick: (AskConversation) -> Unit = {},
    onConversationMoreClick: (AskConversation) -> Unit = {},
    onRefreshConversations: () -> Unit = {},
    onRetryConversations: () -> Unit = {},
    onNewConversationClick: () -> Unit = {},
    onSourceMoreClick: (Source) -> Unit,
    onSourceClick: (Source) -> Unit,
    onNoteClick: (Note) -> Unit,
    onNoteMoreClick: (Note) -> Unit,
    onLoadMoreSources: () -> Unit = {},
    onLoadMoreNotes: () -> Unit = {},
    onLoadMoreConversations: () -> Unit = {},
    onNotebookContentChange: (String) -> Unit = {},
    onRetryNotebookSave: () -> Unit = {},
    onRetryNotebookLoad: () -> Unit = {},
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val hideBottomNavForAskInput =
        isImeVisible && uiState.selectedTab == HomeTab.ASK && uiState.isAskChatOpen
    val notebookImeOpen = isImeVisible && uiState.selectedTab == HomeTab.NOTEBOOK
    val hideBottomNav = hideBottomNavForAskInput || notebookImeOpen

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground)
            .dismissKeyboardOnTapOutside(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!notebookImeOpen) {
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
                        titleOverride = if (
                            uiState.isAskChatOpen && uiState.selectedTab == HomeTab.ASK
                        ) {
                            uiState.askConversationTitle
                        } else {
                            ""
                        },
                        onBackClick = onBackClick,
                        onAddClick = onAddClick,
                    )
                    if (
                        uiState.selectedTab == HomeTab.SOURCES ||
                        uiState.selectedTab == HomeTab.NOTES ||
                        (uiState.selectedTab == HomeTab.ASK && !uiState.isAskChatOpen)
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
                                    when (uiState.selectedTab) {
                                        HomeTab.NOTES -> R.string.home_search_notes
                                        HomeTab.ASK -> R.string.home_search_conversations
                                        else -> R.string.home_search_sources
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
                    onLoadMore = onLoadMoreSources,
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .padding(bottom = HomeBottomNavClearance),
                )
                HomeTab.ASK -> AskPane(
                    uiState = uiState,
                    onAskSubmit = onAskSubmit,
                    onAskStop = onAskStop,
                    onScopeChipClick = onScopeChipClick,
                    onAddSourceClick = onAskAddSourceClick,
                    onSaveAsNote = onAskSaveAsNote,
                    onFeedback = onAskFeedback,
                    onCitationClick = onAskCitationClick,
                    onUserEnterAnimationFinished = onAskUserEnterAnimationFinished,
                    onConversationClick = onConversationClick,
                    onConversationMoreClick = onConversationMoreClick,
                    onRefreshConversations = onRefreshConversations,
                    onRetryConversations = onRetryConversations,
                    onNewConversationClick = onNewConversationClick,
                    onLoadMore = onLoadMoreConversations,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isImeVisible && uiState.selectedTab == HomeTab.ASK) {
                                Modifier.imePadding()
                            } else {
                                Modifier
                                    .navigationBarsPadding()
                                    .padding(bottom = HomeBottomNavClearance)
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
                        .navigationBarsPadding()
                        .padding(bottom = HomeBottomNavClearance),
                )
                HomeTab.NOTEBOOK -> NotebookPane(
                    content = uiState.notebookContent,
                    spaceTitle = uiState.spaceTitle,
                    saveStatus = uiState.notebookSaveStatus,
                    isLoadingNotebook = uiState.isLoadingNotebook,
                    isLoadingNotes = uiState.isLoading,
                    notebookError = uiState.notebookError,
                    onContentChange = onNotebookContentChange,
                    onRetrySave = onRetryNotebookSave,
                    onRetryLoad = onRetryNotebookLoad,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (notebookImeOpen) {
                                Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                            } else {
                                Modifier
                                    .navigationBarsPadding()
                                    .padding(bottom = HomeBottomNavClearance)
                            },
                        ),
                )
            }
        }

        if (!hideBottomNav) {
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
                notesUserCreatedCount = 8,
                notesSavedAnswerCount = 4,
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
                notesUserCreatedCount = 0,
                notesSavedAnswerCount = 0,
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
                notesUserCreatedCount = 8,
                notesSavedAnswerCount = 4,
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
    HomeActionError.EXPORT_FAILED -> R.string.notebook_export_failed
}
