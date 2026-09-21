package com.nus.folio.presentation.home

import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.AskConversation
import com.nus.folio.domain.model.AskConversationMessage
import com.nus.folio.domain.model.AskConversationPaging
import com.nus.folio.domain.model.AskConversationRole
import com.nus.folio.domain.model.AskFeedbackRating
import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.model.CreateNoteRequest
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.usecase.CreateNoteUseCase
import com.nus.folio.domain.usecase.DeleteAskConversationUseCase
import com.nus.folio.domain.usecase.GetAskConversationUseCase
import com.nus.folio.domain.usecase.GetAskConversationsUseCase
import com.nus.folio.domain.usecase.GetAskSuggestionsUseCase
import com.nus.folio.domain.usecase.StreamAskAnswerUseCase
import com.nus.folio.domain.usecase.SubmitAskFeedbackUseCase
import com.nus.folio.domain.usecase.UpdateAskConversationUseCase
import com.nus.folio.domain.util.AskSavedNoteFormatter
import com.nus.folio.domain.util.NoteInputRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * Owns Ask-tab state: conversation list, chat thread, submit/stop/stream,
 * scope selection, suggestions, citation previews, and saving an answer as a note.
 */
internal class HomeAskDelegate(
    private val spaceId: String,
    private val state: MutableStateFlow<HomeUiState>,
    private val scope: CoroutineScope,
    private val streamAskAnswerUseCase: StreamAskAnswerUseCase,
    private val getAskSuggestionsUseCase: GetAskSuggestionsUseCase,
    private val getAskConversationsUseCase: GetAskConversationsUseCase,
    private val getAskConversationUseCase: GetAskConversationUseCase,
    private val updateAskConversationUseCase: UpdateAskConversationUseCase,
    private val deleteAskConversationUseCase: DeleteAskConversationUseCase,
    private val submitAskFeedbackUseCase: SubmitAskFeedbackUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val searchDebounceMs: Long,
    private val pageLimit: Int = AskConversationPaging.DEFAULT_LIMIT,
) {

    private var askSuggestionsJob: Job? = null
    private var askStreamJob: Job? = null
    private var conversationsJob: Job? = null
    private var conversationDetailJob: Job? = null
    private var nextAskMessageId: Long = 0L
    private var streamingAssistantId: String? = null
    /** Backend thread id from the last `start` frame; omitted on the first question. */
    private var askConversationId: String? = null

    fun onAskSubmit(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return
        val current = state.value
        if (!current.hasAskEvidence()) return
        if (current.isAskStreaming()) return

        val userMessage = AskMessage(
            id = "ask-msg-${nextAskMessageId++}",
            role = AskMessageRole.USER,
            content = trimmed,
        )
        val assistantId = "ask-msg-${nextAskMessageId++}"
        val assistantMessage = AskMessage(
            id = assistantId,
            role = AskMessageRole.ASSISTANT,
            content = "",
            isStreaming = true,
        )
        streamingAssistantId = assistantId
        state.update {
            it.copy(
                askMessages = it.askMessages + userMessage + assistantMessage,
                pendingUserEnterAnimationId = userMessage.id,
                isAskChatOpen = true,
            )
        }
        startAskStream(
            question = trimmed,
            assistantId = assistantId,
            sourceId = when (current.askScope) {
                AskScope.ENTIRE_SPACE -> null
                AskScope.CURRENT_SOURCE -> current.askSourceId
            },
        )
    }

    fun onAskUserEnterAnimationFinished(messageId: String) {
        state.update { current ->
            if (current.pendingUserEnterAnimationId != messageId) {
                current
            } else {
                current.copy(pendingUserEnterAnimationId = null)
            }
        }
    }

    fun onAskStop() {
        val assistantId = streamingAssistantId ?: return
        askStreamJob?.cancel()
        askStreamJob = null
        streamingAssistantId = null
        state.update { current ->
            current.copy(
                askMessages = current.askMessages.map { message ->
                    if (message.id == assistantId) {
                        message.copy(isStreaming = false, wasStopped = true)
                    } else {
                        message
                    }
                },
            )
        }
    }

    fun onAskSaveAsNote(messageId: String) {
        val current = state.value
        if (current.saveAskNoteDraft != null || current.savingAskMessageId != null) return
        val messageIndex = current.askMessages.indexOfFirst {
            it.id == messageId && it.role == AskMessageRole.ASSISTANT
        }
        if (messageIndex < 0) return
        val message = current.askMessages[messageIndex]
        if (message.isSavedAsNote || message.isStreaming || message.isFailed || message.content.isBlank()) return

        val question = current.askMessages
            .take(messageIndex)
            .lastOrNull { it.role == AskMessageRole.USER }
            ?.content
            .orEmpty()

        state.update {
            it.copy(
                saveAskNoteDraft = SaveAskNoteDraft(
                    messageId = messageId,
                    initialTitle = AskSavedNoteFormatter.titleFromQuestion(question),
                    content = AskSavedNoteFormatter.body(
                        content = message.content,
                        citations = message.citations,
                        limitation = message.limitation,
                    ),
                    citations = message.citations,
                    conversationId = message.conversationId,
                    backendMessageId = message.backendMessageId,
                ),
            )
        }
    }

    fun onAskSaveAsNoteDismiss() {
        state.update { it.copy(saveAskNoteDraft = null) }
    }

    fun onAskSaveAsNoteConfirm(title: String, content: String) {
        val draft = state.value.saveAskNoteDraft ?: return
        if (state.value.savingAskMessageId != null) return
        if (!NoteInputRules.canSave(title, content)) return
        val resolvedTitle = NoteInputRules.resolveTitle(title)
        val resolvedContent = content.trim()
        val messageId = draft.messageId

        state.update {
            it.copy(
                saveAskNoteDraft = null,
                savingAskMessageId = messageId,
            )
        }
        scope.launch {
            createNoteUseCase(
                CreateNoteRequest(
                    spaceId = spaceId,
                    title = resolvedTitle,
                    content = resolvedContent,
                    origin = NoteOrigin.SAVED_ANSWER,
                    citationCount = draft.citations.size,
                    citations = draft.citations,
                    project = state.value.spaceTitle.ifBlank { null },
                    conversationId = draft.conversationId,
                    messageId = draft.backendMessageId,
                ),
            ).onSuccess { created ->
                state.update { current ->
                    val alreadyExists = current.allNotes.any { it.id == created.id }
                    val notes = listOf(created) + current.allNotes.filterNot { it.id == created.id }
                    val next = current.copy(
                        savingAskMessageId = null,
                        askMessages = current.askMessages.map { msg ->
                            if (msg.id == messageId) msg.copy(isSavedAsNote = true) else msg
                        },
                        allNotes = notes,
                        notesAllCount = if (alreadyExists) {
                            current.notesAllCount
                        } else {
                            current.notesAllCount + 1
                        },
                        notesUserCreatedCount = notes.count { note ->
                            note.origin == NoteOrigin.USER_CREATED
                        },
                        notesSavedAnswerCount = notes.count { note ->
                            note.origin == NoteOrigin.SAVED_ANSWER
                        },
                        userMessage = HomeUserMessage.NOTE_SAVED_FROM_ASK,
                    )
                    next.copy(visibleNotes = filterNotes(next))
                }
            }.onFailure { throwable ->
                state.update {
                    it.copy(
                        savingAskMessageId = null,
                        actionError = throwable.toHomeActionError(),
                    )
                }
            }
        }
    }

    fun onAskFeedback(messageId: String, useful: Boolean) {
        val rating = if (useful) AskFeedbackRating.USEFUL else AskFeedbackRating.NOT_USEFUL
        val feedback = if (useful) AskFeedback.USEFUL else AskFeedback.NOT_USEFUL
        val current = state.value
        val message = current.askMessages.firstOrNull {
            it.id == messageId && it.role == AskMessageRole.ASSISTANT
        } ?: return
        if (message.isStreaming || message.isFailed || message.feedback == feedback) return
        val conversationId = message.conversationId ?: askConversationId
        val backendMessageId = message.backendMessageId
        if (conversationId.isNullOrBlank() || backendMessageId.isNullOrBlank()) {
            state.update { it.copy(actionError = HomeActionError.GENERIC) }
            return
        }
        val previousFeedback = message.feedback
        // Optimistic: hide the prompt immediately; revert if the request fails.
        state.update { ui ->
            ui.copy(
                askMessages = ui.askMessages.map { existing ->
                    if (existing.id == messageId && existing.role == AskMessageRole.ASSISTANT) {
                        existing.copy(feedback = feedback)
                    } else {
                        existing
                    }
                },
            )
        }
        scope.launch {
            submitAskFeedbackUseCase(
                spaceId = spaceId,
                conversationId = conversationId,
                messageId = backendMessageId,
                rating = rating,
            ).onSuccess {
                state.update { ui ->
                    ui.copy(userMessage = HomeUserMessage.ASK_FEEDBACK_RECORDED)
                }
            }.onFailure { throwable ->
                state.update { ui ->
                    ui.copy(
                        askMessages = ui.askMessages.map { existing ->
                            if (existing.id == messageId && existing.role == AskMessageRole.ASSISTANT) {
                                existing.copy(feedback = previousFeedback)
                            } else {
                                existing
                            }
                        },
                        actionError = throwable.toHomeActionError(),
                    )
                }
            }
        }
    }

    fun onAskCitationClick(citation: AskCitation) {
        state.update { it.copy(previewCitation = citation) }
    }

    fun onCitationPreviewDismiss() {
        state.update { it.copy(previewCitation = null) }
    }

    fun onCitationOpenInSource() {
        val citation = state.value.previewCitation ?: return
        if (citation.sourceId.isBlank() || citation.sourceId == "unknown") return
        askStreamJob?.cancel()
        state.update {
            it.copy(
                previewCitation = null,
                isOpeningSource = false,
                openSourceDetailId = citation.sourceId,
                openSourceDetailHighlight = citation.evidenceText.takeIf { text -> text.isNotBlank() },
            )
        }
    }

    private fun startAskStream(
        question: String,
        assistantId: String,
        sourceId: String?,
    ) {
        askStreamJob?.cancel()
        askStreamJob = scope.launch {
            try {
                streamAskAnswerUseCase(
                    spaceId = spaceId,
                    question = question,
                    sourceId = sourceId,
                    conversationId = askConversationId,
                ).collect { event ->
                    coroutineContext.ensureActive()
                    when (event) {
                        is AskStreamEvent.Started -> {
                            askConversationId = event.conversationId
                            updateAssistantMessage(assistantId) { message ->
                                message.copy(
                                    conversationId = event.conversationId,
                                    backendMessageId = event.messageId.takeIf { it.isNotBlank() }
                                        ?: message.backendMessageId,
                                )
                            }
                        }
                        is AskStreamEvent.Delta -> {
                            updateAssistantMessage(assistantId) { message ->
                                message.copy(content = message.content + event.text)
                            }
                        }
                        is AskStreamEvent.Citations -> {
                            updateAssistantMessage(assistantId) { message ->
                                message.copy(citations = event.citations)
                            }
                        }
                        is AskStreamEvent.Completed -> {
                            updateAssistantMessage(assistantId) { message ->
                                message.copy(
                                    isStreaming = false,
                                    content = event.content ?: message.content,
                                    citations = event.citations.ifEmpty { message.citations },
                                    limitation = event.limitation,
                                    wasStopped = message.wasStopped || event.stopped,
                                    conversationId = message.conversationId ?: askConversationId,
                                    backendMessageId = event.messageId.takeIf { it.isNotBlank() }
                                        ?: message.backendMessageId,
                                )
                            }
                            streamingAssistantId = null
                        }
                    }
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Stop retains partial text; state already updated in onAskStop.
            } catch (throwable: Throwable) {
                updateAssistantMessage(assistantId) { message ->
                    message.copy(
                        isStreaming = false,
                        isFailed = true,
                        content = message.content.ifBlank {
                            ASK_STREAM_FALLBACK_ERROR
                        },
                    )
                }
                streamingAssistantId = null
                state.update {
                    it.copy(actionError = throwable.toHomeActionError())
                }
            } finally {
                if (streamingAssistantId == assistantId) {
                    streamingAssistantId = null
                }
                updateAssistantMessage(assistantId) { message ->
                    if (message.isStreaming) message.copy(isStreaming = false) else message
                }
            }
        }
    }

    private fun updateAssistantMessage(
        assistantId: String,
        transform: (AskMessage) -> AskMessage,
    ) {
        state.update { current ->
            current.copy(
                askMessages = current.askMessages.map { message ->
                    if (message.id == assistantId) transform(message) else message
                },
            )
        }
    }

    /**
     * Selects Entire Space (`sourceId == null`) or a single Ready source.
     * Switching to a different option clears conversation messages and resets chat context.
     */
    fun onAskScopeOptionSelected(sourceId: String?) {
        applyAskScope(sourceId = sourceId, forceReset = false)
    }

    /** Clears the current Ask thread and returns to the empty conversation state. */
    fun onNewConversation() {
        conversationsJob?.cancel()
        conversationsJob = null
        conversationDetailJob?.cancel()
        askStreamJob?.cancel()
        askStreamJob = null
        streamingAssistantId = null
        askConversationId = null
        state.update {
            it.copy(
                isAskChatOpen = true,
                isLoadingAskConversation = false,
                askMessages = emptyList(),
                askConversationTitle = "",
                savingAskMessageId = null,
                saveAskNoteDraft = null,
                previewCitation = null,
                pendingUserEnterAnimationId = null,
                askConversationEpoch = it.askConversationEpoch + 1,
            )
        }
        refreshAskSuggestions()
    }

    fun onAskChatBack() {
        if (!state.value.isAskChatOpen) return
        conversationDetailJob?.cancel()
        askStreamJob?.cancel()
        askStreamJob = null
        streamingAssistantId = null
        askConversationId = null
        state.update {
            it.copy(
                isAskChatOpen = false,
                isLoadingAskConversation = false,
                askMessages = emptyList(),
                askConversationTitle = "",
                savingAskMessageId = null,
                saveAskNoteDraft = null,
                previewCitation = null,
                pendingUserEnterAnimationId = null,
                askConversationEpoch = it.askConversationEpoch + 1,
            )
        }
        loadConversationsOnly()
    }

    fun cancelSearchJob() {
        conversationsJob?.cancel()
        conversationsJob = null
        state.update {
            it.copy(
                isLoadingMoreAskConversations = false,
                isSearchingAskConversations = false,
            )
        }
    }

    fun scheduleSearchReload() {
        conversationsJob?.cancel()
        state.update { it.copy(isSearchingAskConversations = true) }
        conversationsJob = scope.launch {
            delay(searchDebounceMs)
            loadConversationsInternal(reset = true, refreshing = false)
        }
    }

    fun loadConversationsOnly() {
        conversationsJob?.cancel()
        conversationsJob = scope.launch {
            loadConversationsInternal(reset = true, refreshing = false)
        }
    }

    fun onRefreshConversations() {
        if (state.value.isRefreshingAskConversations) return
        conversationsJob?.cancel()
        conversationsJob = scope.launch {
            state.update {
                it.copy(
                    isRefreshingAskConversations = true,
                    isLoadingMoreAskConversations = false,
                    isSearchingAskConversations = false,
                )
            }
            try {
                loadConversationsInternal(reset = true, refreshing = true)
            } finally {
                state.update { it.copy(isRefreshingAskConversations = false) }
            }
        }
    }

    fun onLoadMore() {
        val current = state.value
        if (
            current.isLoading ||
            current.isRefreshingAskConversations ||
            current.isLoadingMoreAskConversations ||
            current.isSearchingAskConversations ||
            !current.askConversationsHasMore ||
            current.selectedTab != HomeTab.ASK ||
            current.isAskChatOpen
        ) {
            return
        }
        conversationsJob?.cancel()
        conversationsJob = scope.launch {
            loadConversationsInternal(reset = false, refreshing = false)
        }
    }

    private suspend fun loadConversationsInternal(
        reset: Boolean,
        refreshing: Boolean,
    ) {
        val current = state.value
        val page = if (reset) {
            AskConversationPaging.DEFAULT_PAGE
        } else {
            current.askConversationsCurrentPage + 1
        }
        if (reset) {
            state.update {
                it.copy(
                    isLoadingMoreAskConversations = false,
                    askConversationsError = if (refreshing) it.askConversationsError else null,
                )
            }
        } else {
            state.update { it.copy(isLoadingMoreAskConversations = true) }
        }

        getAskConversationsUseCase(
            spaceId = spaceId,
            search = current.searchQuery.trim().takeIf { it.isNotEmpty() },
            page = page,
            limit = pageLimit,
        )
            .onSuccess { library ->
                state.update { ui ->
                    val merged = if (reset) {
                        library.conversations
                    } else {
                        val existingIds = ui.askConversations.mapTo(HashSet()) { it.id }
                        ui.askConversations + library.conversations.filterNot { it.id in existingIds }
                    }
                    ui.copy(
                        askConversations = merged,
                        askConversationsCurrentPage = library.page,
                        askConversationsHasMore = library.hasMore,
                        isLoadingMoreAskConversations = false,
                        isSearchingAskConversations = false,
                        isRefreshingAskConversations = false,
                        askConversationsError = null,
                    )
                }
            }
            .onFailure { throwable ->
                state.update { ui ->
                    if (reset && ui.askConversations.isEmpty()) {
                        ui.copy(
                            isLoadingMoreAskConversations = false,
                            isSearchingAskConversations = false,
                            isRefreshingAskConversations = false,
                            askConversationsError = "",
                        )
                    } else {
                        ui.copy(
                            isLoadingMoreAskConversations = false,
                            isSearchingAskConversations = false,
                            isRefreshingAskConversations = false,
                            actionError = throwable.toHomeActionError(),
                        )
                    }
                }
            }
    }

    fun onConversationClick(conversation: AskConversation) {
        if (state.value.isAskStreaming() || state.value.isLoadingAskConversation) return
        conversationsJob?.cancel()
        conversationsJob = null
        conversationDetailJob?.cancel()
        askStreamJob?.cancel()
        askStreamJob = null
        streamingAssistantId = null
        askConversationId = conversation.id
        state.update {
            it.copy(
                isAskChatOpen = true,
                isLoadingAskConversation = true,
                askMessages = emptyList(),
                askConversationTitle = conversation.title,
                optionsConversation = null,
                renamingConversation = null,
                pendingUserEnterAnimationId = null,
                savingAskMessageId = null,
                saveAskNoteDraft = null,
                previewCitation = null,
                askConversationEpoch = it.askConversationEpoch + 1,
            )
        }
        conversationDetailJob = scope.launch {
            getAskConversationUseCase(spaceId, conversation.id)
                .onSuccess { detail ->
                    askConversationId = detail.conversation.id
                    val availableSourceIds = state.value.allSources
                        .asSequence()
                        .map { it.id }
                        .toSet()
                    val (restoredScope, restoredSourceId) = detail.conversation
                        .copy(sourceId = detail.conversation.sourceId ?: conversation.sourceId)
                        .askScopeSelection(availableSourceIds)
                    val messages = detail.messages.map { message ->
                        message.toAskMessage(conversationId = detail.conversation.id)
                    }
                    state.update {
                        it.copy(
                            isLoadingAskConversation = false,
                            askMessages = messages,
                            askConversationTitle = detail.conversation.title
                                .ifBlank { conversation.title },
                            askScope = restoredScope,
                            askSourceId = restoredSourceId,
                        )
                    }
                    refreshAskSuggestions()
                }
                .onFailure { throwable ->
                    askConversationId = null
                    state.update {
                        it.copy(
                            isAskChatOpen = false,
                            isLoadingAskConversation = false,
                            askMessages = emptyList(),
                            askConversationTitle = "",
                            actionError = throwable.toHomeActionError(),
                        )
                    }
                }
        }
    }

    fun onConversationOptionsClick(conversation: AskConversation) {
        state.update { it.copy(optionsConversation = conversation) }
    }

    fun onConversationOptionsDismiss() {
        state.update { it.copy(optionsConversation = null) }
    }

    fun onRenameConversationClick() {
        val conversation = state.value.optionsConversation ?: return
        state.update { it.copy(optionsConversation = null, renamingConversation = conversation) }
    }

    fun onRenameConversationDismiss() {
        if (state.value.isRenamingConversation) return
        state.update { it.copy(renamingConversation = null) }
    }

    fun onRenameConversationSave(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty() || state.value.isRenamingConversation) return
        val renaming = state.value.renamingConversation ?: return
        if (trimmed == renaming.title) {
            state.update { it.copy(renamingConversation = null) }
            return
        }
        scope.launch {
            state.update { it.copy(isRenamingConversation = true, actionError = null) }
            updateAskConversationUseCase(spaceId, renaming.id, trimmed)
                .onSuccess { updated ->
                    state.update { current ->
                        current.copy(
                            isRenamingConversation = false,
                            renamingConversation = null,
                            askConversationTitle = if (
                                current.isAskChatOpen && askConversationId == renaming.id
                            ) {
                                updated.title
                            } else {
                                current.askConversationTitle
                            },
                            askConversations = current.askConversations.map { conversation ->
                                if (conversation.id == renaming.id) {
                                    conversation.copy(
                                        title = updated.title,
                                        dateLabel = updated.dateLabel.ifBlank { conversation.dateLabel },
                                    )
                                } else {
                                    conversation
                                }
                            },
                            userMessage = HomeUserMessage.CONVERSATION_RENAMED,
                        )
                    }
                }
                .onFailure { throwable ->
                    state.update {
                        it.copy(
                            isRenamingConversation = false,
                            actionError = throwable.toHomeActionError(),
                        )
                    }
                }
        }
    }

    fun onDeleteConversationClick() {
        val conversation = state.value.optionsConversation ?: return
        state.update { it.copy(optionsConversation = null, deletingConversation = conversation) }
    }

    fun onDeleteConversationDismiss() {
        if (state.value.isDeletingConversation) return
        state.update { it.copy(deletingConversation = null) }
    }

    fun onDeleteConversationConfirm() {
        if (state.value.isDeletingConversation) return
        val deleting = state.value.deletingConversation ?: return
        scope.launch {
            state.update { it.copy(isDeletingConversation = true, actionError = null) }
            deleteAskConversationUseCase(spaceId, deleting.id)
                .onSuccess {
                    val closingChat = askConversationId == deleting.id ||
                        state.value.askMessages.any { it.conversationId == deleting.id }
                    if (closingChat) {
                        conversationDetailJob?.cancel()
                        askStreamJob?.cancel()
                        askStreamJob = null
                        streamingAssistantId = null
                        askConversationId = null
                    }
                    state.update { current ->
                        current.copy(
                            askConversations = current.askConversations.filterNot { it.id == deleting.id },
                            deletingConversation = null,
                            isDeletingConversation = false,
                            userMessage = HomeUserMessage.CONVERSATION_DELETED,
                            isAskChatOpen = if (closingChat) false else current.isAskChatOpen,
                            askConversationTitle = if (closingChat) {
                                ""
                            } else {
                                current.askConversationTitle
                            },
                            isLoadingAskConversation = if (closingChat) {
                                false
                            } else {
                                current.isLoadingAskConversation
                            },
                            askMessages = if (closingChat) emptyList() else current.askMessages,
                            pendingUserEnterAnimationId = if (closingChat) {
                                null
                            } else {
                                current.pendingUserEnterAnimationId
                            },
                            askConversationEpoch = if (closingChat) {
                                current.askConversationEpoch + 1
                            } else {
                                current.askConversationEpoch
                            },
                        )
                    }
                }
                .onFailure { throwable ->
                    state.update {
                        it.copy(
                            isDeletingConversation = false,
                            actionError = throwable.toHomeActionError(),
                        )
                    }
                }
        }
    }

    /** Auto-selects a source when redirecting from Source Reader (always resets chat). */
    fun onAskSourceSelected(sourceId: String) {
        applyAskScope(sourceId = sourceId, forceReset = true)
    }

    fun applyAskScope(sourceId: String?, forceReset: Boolean) {
        val newScope = if (sourceId == null) AskScope.ENTIRE_SPACE else AskScope.CURRENT_SOURCE
        val current = state.value
        val unchanged = current.askScope == newScope && current.askSourceId == sourceId
        if (unchanged && !forceReset) return
        conversationDetailJob?.cancel()
        askStreamJob?.cancel()
        askStreamJob = null
        streamingAssistantId = null
        askConversationId = null
        state.update {
            it.copy(
                askScope = newScope,
                askSourceId = sourceId,
                isAskChatOpen = true,
                isLoadingAskConversation = false,
                askMessages = emptyList(),
                askConversationTitle = "",
                askSuggestions = emptyList(),
                pendingUserEnterAnimationId = null,
                askConversationEpoch = it.askConversationEpoch + 1,
            )
        }
        refreshAskSuggestions()
    }

    private fun refreshAskSuggestions() {
        askSuggestionsJob?.cancel()
        val current = state.value
        if (!current.hasAskEvidence()) {
            state.update { it.copy(askSuggestions = emptyList()) }
            return
        }
        val sourceId = when (current.askScope) {
            AskScope.ENTIRE_SPACE -> null
            AskScope.CURRENT_SOURCE -> {
                val selectedId = current.askSourceId
                val source = selectedId?.let { id -> current.allSources.find { it.id == id } }
                if (selectedId == null || source?.status != SourceStatus.READY) {
                    state.update { it.copy(askSuggestions = emptyList()) }
                    return
                }
                selectedId
            }
        }
        askSuggestionsJob = scope.launch {
            getAskSuggestionsUseCase(spaceId = spaceId, sourceId = sourceId)
                .onSuccess { suggestions ->
                    val questions = suggestions.questions.take(3)
                    state.update {
                        it.copy(
                            askSuggestions = if (suggestions.isDynamic) {
                                questions
                            } else {
                                emptyList()
                            },
                        )
                    }
                }
                .onFailure {
                    state.update { it.copy(askSuggestions = emptyList()) }
                }
        }
    }

    private fun AskConversation.askScopeSelection(
        availableSourceIds: Set<String>,
    ): Pair<AskScope, String?> {
        val scopedSourceId = sourceId?.trim()?.takeIf { it.isNotEmpty() }
        return if (scopedSourceId == null || scopedSourceId !in availableSourceIds) {
            AskScope.ENTIRE_SPACE to null
        } else {
            AskScope.CURRENT_SOURCE to scopedSourceId
        }
    }

    private fun AskConversationMessage.toAskMessage(conversationId: String): AskMessage {
        val localId = id.takeIf { it.isNotBlank() } ?: "ask-msg-${nextAskMessageId++}"
        return AskMessage(
            id = localId,
            role = when (role) {
                AskConversationRole.USER -> AskMessageRole.USER
                AskConversationRole.ASSISTANT -> AskMessageRole.ASSISTANT
            },
            content = content,
            wasStopped = stopped,
            citations = citations,
            limitation = limitation,
            feedback = when (feedback) {
                AskFeedbackRating.USEFUL -> AskFeedback.USEFUL
                AskFeedbackRating.NOT_USEFUL -> AskFeedback.NOT_USEFUL
                null -> AskFeedback.NONE
            },
            isSavedAsNote = !savedNoteId.isNullOrBlank(),
            conversationId = conversationId,
            backendMessageId = id.takeIf { it.isNotBlank() },
        )
    }
}
