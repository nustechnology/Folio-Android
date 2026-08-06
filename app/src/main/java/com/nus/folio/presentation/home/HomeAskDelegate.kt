package com.nus.folio.presentation.home

import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.model.CreateNoteRequest
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.usecase.CreateNoteUseCase
import com.nus.folio.domain.usecase.GetAskSuggestionsUseCase
import com.nus.folio.domain.usecase.StreamAskAnswerUseCase
import com.nus.folio.domain.util.AskSavedNoteFormatter
import com.nus.folio.domain.util.NoteInputRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * Owns Ask-tab state: submit/stop/stream, scope selection, suggestions,
 * citation previews, and saving an answer as a note.
 */
internal class HomeAskDelegate(
    private val spaceId: String,
    private val state: MutableStateFlow<HomeUiState>,
    private val scope: CoroutineScope,
    private val streamAskAnswerUseCase: StreamAskAnswerUseCase,
    private val getAskSuggestionsUseCase: GetAskSuggestionsUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
) {

    private var askSuggestionsJob: Job? = null
    private var askStreamJob: Job? = null
    private var nextAskMessageId: Long = 0L
    private var streamingAssistantId: String? = null

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
        if (message.isSavedAsNote || message.isStreaming || message.content.isBlank()) return

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
                    content = AskSavedNoteFormatter.body(message.content),
                    citations = message.citations,
                ),
            )
        }
    }

    fun onAskSaveAsNoteDismiss() {
        state.update { it.copy(saveAskNoteDraft = null) }
    }

    fun onAskSaveAsNoteConfirm(title: String) {
        val draft = state.value.saveAskNoteDraft ?: return
        if (state.value.savingAskMessageId != null) return
        if (NoteInputRules.titleValidationError(title) != null) return
        val resolvedTitle = NoteInputRules.resolveTitle(title)
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
                    content = draft.content,
                    origin = NoteOrigin.SAVED_ANSWER,
                    citationCount = draft.citations.size,
                    citations = draft.citations,
                    project = state.value.spaceTitle.ifBlank { null },
                ),
            ).onSuccess { created ->
                state.update { current ->
                    val notes = listOf(created) + current.allNotes
                    val next = current.copy(
                        savingAskMessageId = null,
                        askMessages = current.askMessages.map { msg ->
                            if (msg.id == messageId) msg.copy(isSavedAsNote = true) else msg
                        },
                        allNotes = notes,
                        notesAllCount = notes.size,
                        notesPinnedCount = notes.count { note -> note.isPinned },
                        notesUnfiledCount = notes.count { note -> note.project.isNullOrBlank() },
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
        val feedback = if (useful) AskFeedback.USEFUL else AskFeedback.NOT_USEFUL
        state.update { current ->
            current.copy(
                askMessages = current.askMessages.map { message ->
                    if (message.id == messageId && message.role == AskMessageRole.ASSISTANT) {
                        message.copy(feedback = feedback)
                    } else {
                        message
                    }
                },
                userMessage = HomeUserMessage.ASK_FEEDBACK_RECORDED,
            )
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
                ).collect { event ->
                    coroutineContext.ensureActive()
                    when (event) {
                        is AskStreamEvent.Delta -> {
                            updateAssistantMessage(assistantId) { message ->
                                message.copy(content = message.content + event.text)
                            }
                        }
                        is AskStreamEvent.Completed -> {
                            updateAssistantMessage(assistantId) { message ->
                                message.copy(
                                    isStreaming = false,
                                    citations = event.citations,
                                    limitation = event.limitation,
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
        askStreamJob?.cancel()
        askStreamJob = null
        streamingAssistantId = null
        state.update {
            it.copy(
                askMessages = emptyList(),
                savingAskMessageId = null,
                saveAskNoteDraft = null,
                previewCitation = null,
                pendingUserEnterAnimationId = null,
                askConversationEpoch = it.askConversationEpoch + 1,
            )
        }
        refreshAskSuggestions()
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
        askStreamJob?.cancel()
        askStreamJob = null
        streamingAssistantId = null
        state.update {
            it.copy(
                askScope = newScope,
                askSourceId = sourceId,
                askMessages = emptyList(),
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
        val sourceId = current.askSourceId
        if (current.askScope != AskScope.CURRENT_SOURCE || sourceId == null) {
            state.update { it.copy(askSuggestions = emptyList()) }
            return
        }
        val source = current.allSources.find { it.id == sourceId }
        if (source?.status != SourceStatus.READY) {
            state.update { it.copy(askSuggestions = emptyList()) }
            return
        }
        askSuggestionsJob = scope.launch {
            getAskSuggestionsUseCase(sourceId)
                .onSuccess { suggestions ->
                    state.update { it.copy(askSuggestions = suggestions.take(3)) }
                }
                .onFailure {
                    state.update { it.copy(askSuggestions = emptyList()) }
                }
        }
    }
}
