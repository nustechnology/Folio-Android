package com.nus.folio.presentation.home

import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.AskConversation
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.NoteSort
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceFilter
import com.nus.folio.domain.model.SourceProcessingState
import com.nus.folio.domain.model.SourceSort
import com.nus.folio.domain.model.SourceStatus

data class HomeUiState(
    val spaceId: String = "",
    val spaceTitle: String = "",
    /** Research objective for this space; used in the empty-notebook default template. */
    val spaceResearchObjective: String = "",
    val isLoading: Boolean = false,
    val isRefreshingSources: Boolean = false,
    /** True while reloading sources after a filter/sort change (list shows skeleton). */
    val isFilteringSources: Boolean = false,
    val isRefreshingNotes: Boolean = false,
    /** True while reloading the Ask conversation list. */
    val isRefreshingAskConversations: Boolean = false,
    val isLoadingMoreAskConversations: Boolean = false,
    val isSearchingAskConversations: Boolean = false,
    /** True while reloading notes after a filter/sort change (list shows skeleton). */
    val isFilteringNotes: Boolean = false,
    val sourcesError: String? = null,
    val notesError: String? = null,
    val askConversationsError: String? = null,
    val notebookError: String? = null,
    val searchQuery: String = "",
    val selectedFilter: SourceFilter = SourceFilter.ALL,
    val selectedSort: SourceSort = SourceSort.DEFAULT,
    val selectedNoteFilter: NoteFilter = NoteFilter.ALL,
    val selectedNoteSort: NoteSort = NoteSort.DEFAULT,
    val selectedTab: HomeTab = HomeTab.SOURCES,
    val allSources: List<Source> = emptyList(),
    val visibleSources: List<Source> = emptyList(),
    val allNotes: List<Note> = emptyList(),
    val visibleNotes: List<Note> = emptyList(),
    val allCount: Int = 0,
    val sourcesCurrentPage: Int = 1,
    val sourcesHasMore: Boolean = false,
    val isLoadingMoreSources: Boolean = false,
    val notesAllCount: Int = 0,
    val notesUserCreatedCount: Int = 0,
    val notesSavedAnswerCount: Int = 0,
    val notesCurrentPage: Int = 1,
    val notesHasMore: Boolean = false,
    val isLoadingMoreNotes: Boolean = false,
    val isSearchingNotes: Boolean = false,
    val optionsNote: Note? = null,
    val viewingNote: Note? = null,
    val isLoadingNoteDetail: Boolean = false,
    val editingNote: Note? = null,
    val convertingNote: Note? = null,
    val deletingNote: Note? = null,
    val optionsSource: Source? = null,
    val optionsConversation: AskConversation? = null,
    val renamingConversation: AskConversation? = null,
    val isRenamingConversation: Boolean = false,
    val deletingConversation: AskConversation? = null,
    val editingSource: Source? = null,
    /** Plain content for TEXT source edits; empty for Web/File. */
    val editingSourceContent: String = "",
    val deletingSource: Source? = null,
    val showNotebookActions: Boolean = false,
    val showNotebookExport: Boolean = false,
    val notebookContent: String = "",
    val isLoadingNotebook: Boolean = false,
    val notebookSaveStatus: NotebookSaveStatus = NotebookSaveStatus.IDLE,
    val pendingNotebookCopy: String? = null,
    val pendingNotebookExport: NotebookExportRequest? = null,
    /** True after CreateDocument was launched; keeps request without re-opening the picker. */
    val notebookExportPickerLaunched: Boolean = false,
    /**
     * Held until print finishes or the host leaves for a non-configuration reason.
     * Survives activity recreation; [NotebookPrintRequest.id] is bumped on configuration change.
     */
    val pendingNotebookPrint: NotebookPrintRequest? = null,
    val showSortSheet: Boolean = false,
    val showNoteSortSheet: Boolean = false,
    val showAddSourceSheet: Boolean = false,
    val isCreatingSource: Boolean = false,
    val showAddNoteSheet: Boolean = false,
    val isCreatingNote: Boolean = false,
    val processingSourceId: String? = null,
    val processingSourceTitle: String? = null,
    val processingProgress: Int = 0,
    val processingState: SourceProcessingState? = null,
    val isOpeningSource: Boolean = false,
    val openSourceDetailId: String? = null,
    /** Passage to highlight when opening Source Reader from a citation. */
    val openSourceDetailHighlight: String? = null,
    val userMessage: HomeUserMessage? = null,
    /** API/action failure shown as an error toast (safe, localized — never raw Throwable text). */
    val actionError: HomeActionError? = null,
    /** One-shot info toast with dynamic text. */
    val infoToast: String? = null,
    /** Citation opened in the Ask citation preview sheet. */
    val previewCitation: AskCitation? = null,
    /** Draft for saving an Ask answer as a note; null when the sheet is closed. */
    val saveAskNoteDraft: SaveAskNoteDraft? = null,
    /** Assistant message id currently being persisted as a note; null when idle. */
    val savingAskMessageId: String? = null,
    val askScope: AskScope = AskScope.ENTIRE_SPACE,
    val askSourceId: String? = null,
    /** Past Ask threads for this space; shown until a conversation is opened. */
    val askConversations: List<AskConversation> = emptyList(),
    val askConversationsCurrentPage: Int = 1,
    val askConversationsHasMore: Boolean = false,
    /** True while the Ask tab is showing a chat thread instead of the list. */
    val isAskChatOpen: Boolean = false,
    /** Header title while a conversation is open; blank falls back to "Ask". */
    val askConversationTitle: String = "",
    /** True while fetching messages for the selected conversation. */
    val isLoadingAskConversation: Boolean = false,
    /** Messages in the active Ask conversation; cleared when scope changes. */
    val askMessages: List<AskMessage> = emptyList(),
    /**
     * Dynamic suggested questions for a ready source (`isDynamic: true`).
     * Empty means the UI should show the static fallback chips.
     */
    val askSuggestions: List<String> = emptyList(),
    /** Bumped when Ask chat context resets so the input field can clear. */
    val askConversationEpoch: Int = 0,
    /**
     * User message id currently playing the send enter animation.
     * Null when idle — returning to Ask must not replay completed animations.
     */
    val pendingUserEnterAnimationId: String? = null,
    /** Signed-in user display name for Ask message avatars. */
    val userDisplayName: String = "",
    /** Signed-in user email fallback for Ask avatar initials. */
    val userEmail: String = "",
    /** True when the auth session was cleared and the user must sign in again. */
    val requiresReauth: Boolean = false,
)

enum class AskScope {
    ENTIRE_SPACE,
    CURRENT_SOURCE,
}

enum class AskFeedback {
    NONE,
    USEFUL,
    NOT_USEFUL,
}

data class AskMessage(
    val id: String,
    val role: AskMessageRole,
    val content: String,
    val isStreaming: Boolean = false,
    val wasStopped: Boolean = false,
    val isFailed: Boolean = false,
    val citations: List<AskCitation> = emptyList(),
    val limitation: String? = null,
    val feedback: AskFeedback = AskFeedback.NONE,
    val isSavedAsNote: Boolean = false,
    /** Backend conversation id from the Ask SSE `start` frame. */
    val conversationId: String? = null,
    /** Backend assistant message id from the Ask SSE `start` / `done` frames. */
    val backendMessageId: String? = null,
)

/** Prefill state for the Save Ask Answer as Note sheet (AC2). */
data class SaveAskNoteDraft(
    val messageId: String,
    val initialTitle: String,
    val content: String,
    val citations: List<AskCitation>,
    val conversationId: String? = null,
    val backendMessageId: String? = null,
)

enum class AskMessageRole {
    USER,
    ASSISTANT,
}

/** Ready sources available in the Ask scope dropdown. */
fun HomeUiState.askScopeSources(): List<Source> =
    allSources.filter { it.status == SourceStatus.READY }

/** Ready sources used when grounding answers across the entire space. */
fun HomeUiState.askReadySourceCount(): Int =
    allSources.count { it.status == SourceStatus.READY }

fun HomeUiState.hasAskEvidence(): Boolean = when (askScope) {
    AskScope.ENTIRE_SPACE -> askReadySourceCount() > 0
    AskScope.CURRENT_SOURCE ->
        askSourceId
            ?.let { id -> allSources.find { it.id == id } }
            ?.status == SourceStatus.READY
}

fun HomeUiState.isAskStreaming(): Boolean =
    askMessages.any { it.role == AskMessageRole.ASSISTANT && it.isStreaming }

fun HomeUiState.askSourceTitle(): String =
    askSourceId
        ?.let { id -> allSources.find { it.id == id }?.title }
        .orEmpty()

/** Selected source title for the scope chip when asking a single document; blank for entire space. */
fun HomeUiState.askScopeSelectedSourceLabel(): String =
    if (askScope == AskScope.CURRENT_SOURCE) askSourceTitle() else ""

enum class NotebookSaveStatus {
    IDLE,
    SAVING,
    SAVED,
    FAILED,
    /** Local cache shown after the remote GET failed. */
    STALE,
    /** HTML contained unsupported tags; editing would overwrite the server document. */
    READ_ONLY,
}

data class NotebookExportRequest(
    val filename: String,
    val markdown: String,
)

/**
 * Each PDF export gets a unique [id] so Compose relaunches print after repeat exports
 * and after configuration changes (new WebView + [android.print.PrintDocumentAdapter]).
 */
data class NotebookPrintRequest(
    val id: Long,
    val markdown: String,
)

enum class HomeUserMessage {
    SOURCE_UPDATED,
    SOURCE_DELETED,
    SOURCE_CREATED,
    SOURCE_FILE_SELECTED,
    NOTE_UPDATED,
    NOTE_DELETED,
    NOTE_SAVED,
    NOTE_SAVED_FROM_ASK,
    ASK_FEEDBACK_RECORDED,
    CONVERSATION_RENAMED,
    CONVERSATION_DELETED,
    NOTEBOOK_COPIED,
    NOTEBOOK_EXPORTED,
}

/** Safe action-failure codes for toasts; resolved to strings in the UI (CWE-209). */
enum class HomeActionError {
    GENERIC,
    NETWORK,
    FILE_REQUIRED,
    FILE_UNSUPPORTED,
    FILE_TOO_LARGE,
    EXPORT_FAILED,
}

enum class HomeTab {
    SOURCES,
    ASK,
    NOTES,
    NOTEBOOK,
}

enum class NotebookExportFormat {
    MARKDOWN,
    PRINT_PDF,
}
