package com.nus.folio.data.datasource

import com.nus.folio.domain.model.AskFeedbackRating
import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.model.AskSuggestions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Release stub — Ask streaming is not wired to HTTP yet; uses local sample answers.
 */
class AskDataSource(
    @Suppress("UNUSED_PARAMETER")
    accessTokenProvider: () -> String? = { null },
    @Suppress("UNUSED_PARAMETER")
    refreshAccessToken: suspend () -> String? = { null },
) {

    /**
     * Returns up to 3 context-aware questions for a source that has sample metadata.
     * Empty when suggestions are unavailable (UI falls back to static chips).
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun fetchSuggestedQuestions(
        spaceId: String,
        sourceId: String?,
    ): AskSuggestions {
        delay(100)
        val questions = sourceId?.let { AskSampleData.suggestionsFor(it) }.orEmpty()
        return AskSuggestions(
            questions = questions,
            isDynamic = questions.isNotEmpty(),
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun streamAnswer(
        spaceId: String,
        question: String,
        sourceId: String?,
        conversationId: String? = null,
    ): Flow<AskStreamEvent> = flow {
        delay(THINKING_DELAY_MS)
        val answer = AskSampleData.mockAnswer(
            spaceId = spaceId,
            question = question,
            sourceId = sourceId,
        )
        emit(
            AskStreamEvent.Started(
                conversationId = conversationId?.takeIf { it.isNotBlank() } ?: LOCAL_CONVERSATION_ID,
                messageId = LOCAL_MESSAGE_ID,
            ),
        )
        val chunks = chunkText(answer.body)
        for (chunk in chunks) {
            emit(AskStreamEvent.Delta(chunk))
            delay(CHUNK_DELAY_MS)
        }
        emit(
            AskStreamEvent.Completed(
                messageId = LOCAL_MESSAGE_ID,
                content = answer.body,
                citations = answer.citations,
                limitation = answer.limitation,
            ),
        )
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun submitFeedback(
        spaceId: String,
        conversationId: String,
        messageId: String,
        rating: AskFeedbackRating,
    ) {
        delay(100)
    }

    private fun chunkText(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val chunks = mutableListOf<String>()
        var index = 0
        while (index < text.length) {
            val end = (index + CHUNK_SIZE).coerceAtMost(text.length)
            chunks += text.substring(index, end)
            index = end
        }
        return chunks
    }

    companion object {
        private const val THINKING_DELAY_MS = 400L
        private const val CHUNK_DELAY_MS = 45L
        private const val CHUNK_SIZE = 28
        private const val LOCAL_CONVERSATION_ID = "local-conversation"
        private const val LOCAL_MESSAGE_ID = "local-message"
    }
}
