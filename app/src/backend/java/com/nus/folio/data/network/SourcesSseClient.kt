package com.nus.folio.data.network

import com.nus.folio.domain.model.SourceProcessingEvent
import com.nus.folio.domain.model.SourceProcessingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.util.concurrent.atomic.AtomicReference

/**
 * SSE client for source processing status events ([SourcesApi.observeSourceStatus]).
 */
internal object SourcesSseClient {

    const val MAX_SSE_RECONNECT_ATTEMPTS = 5
    private const val SSE_BACKOFF_BASE_MS = 1_000L
    private const val SSE_BACKOFF_MAX_MS = 16_000L
    private const val TIMEOUT_MS = FolioHttp.LONG_TIMEOUT_MS

    fun observeSourceStatus(
        accessToken: String,
        baseUrl: String,
        refreshAccessToken: suspend () -> String? = { null },
        delayMillis: suspend (Long) -> Unit = { delay(it) },
    ): Flow<SourceProcessingEvent> = callbackFlow {
        val url = FolioApiPaths.sourcesStatus(baseUrl)
        val activeConnection = AtomicReference<HttpURLConnection?>(null)

        val readerJob = launch(Dispatchers.IO) {
            var token = accessToken
            var didRefreshForUnauthorized = false
            var reconnectAttempt = 0

            suspend fun tryRefreshAfterUnauthorized(): Boolean {
                if (didRefreshForUnauthorized) return false
                val refreshed = refreshAccessToken()?.takeIf { it.isNotBlank() } ?: return false
                didRefreshForUnauthorized = true
                token = refreshed
                HttpDebugLogger.logEvent("SSE 401: refreshed access token; reconnecting")
                return true
            }

            while (isActive) {
                var responseLogged = false
                val connection = try {
                    FolioHttp.open(
                        method = "GET",
                        url = url,
                        headers = FolioHttp.jsonAcceptHeaders(token) + mapOf(
                            "Accept" to "text/event-stream",
                            "Cache-Control" to "no-cache",
                        ),
                        connectTimeoutMs = TIMEOUT_MS,
                        readTimeoutMs = 0, // keep SSE connection open
                    )
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    HttpDebugLogger.logError(method = "GET", url = url, error = error)
                    if (!scheduleSseReconnect(
                            delayMillis = delayMillis,
                            reconnectAttempt = ++reconnectAttempt,
                            reason = error.message ?: error.javaClass.simpleName,
                        )
                    ) {
                        close(error)
                        return@launch
                    }
                    continue
                }
                activeConnection.set(connection)
                try {
                    HttpDebugLogger.logRequest(
                        method = "GET",
                        url = url,
                        contentType = "text/event-stream",
                    )
                    val code = connection.responseCode
                    if (code !in 200..299) {
                        val responseBody = FolioHttp.readBody(connection.errorStream)
                        HttpDebugLogger.logResponse(
                            method = "GET",
                            url = url,
                            code = code,
                            body = responseBody,
                        )
                        responseLogged = true
                        val error = FolioHttp.unauthorizedOrIo(
                            responseBody,
                            code,
                            "Source status stream",
                        )
                        if (error is UnauthorizedException) {
                            if (tryRefreshAfterUnauthorized()) continue
                            close(error)
                            return@launch
                        }
                        if (!scheduleSseReconnect(
                                delayMillis = delayMillis,
                                reconnectAttempt = ++reconnectAttempt,
                                reason = "HTTP $code",
                            )
                        ) {
                            close(error)
                            return@launch
                        }
                        continue
                    }
                    HttpDebugLogger.logResponse(
                        method = "GET",
                        url = url,
                        code = code,
                        body = "SSE stream opened",
                    )
                    responseLogged = true
                    BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                        val dataLines = mutableListOf<String>()
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            when {
                                line.startsWith("data:") -> {
                                    dataLines += line.removePrefix("data:").trimStart()
                                }
                                line.startsWith(":") ||
                                    line.startsWith("event:") ||
                                    line.startsWith("id:") ||
                                    line.startsWith("retry:") -> {
                                    // ignore SSE metadata / comments
                                }
                                line.isEmpty() -> {
                                    val payload = dataLines.joinToString("\n").trim()
                                    dataLines.clear()
                                    if (emitSsePayload(payload)) {
                                        // Only a delivered event proves the stream is healthy.
                                        reconnectAttempt = 0
                                    }
                                }
                            }
                        }
                        if (dataLines.isNotEmpty() && emitSsePayload(dataLines.joinToString("\n").trim())) {
                            reconnectAttempt = 0
                        }
                    }
                    if (!isActive) return@launch
                    if (!scheduleSseReconnect(
                            delayMillis = delayMillis,
                            reconnectAttempt = ++reconnectAttempt,
                            reason = "stream ended",
                        )
                    ) {
                        close()
                        return@launch
                    }
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    if (!responseLogged) {
                        HttpDebugLogger.logError(method = "GET", url = url, error = error)
                    }
                    if (error is UnauthorizedException) {
                        if (tryRefreshAfterUnauthorized()) continue
                        close(error)
                        return@launch
                    }
                    if (!isActive) return@launch
                    if (!scheduleSseReconnect(
                            delayMillis = delayMillis,
                            reconnectAttempt = ++reconnectAttempt,
                            reason = error.message ?: error.javaClass.simpleName,
                        )
                    ) {
                        close(error)
                        return@launch
                    }
                } finally {
                    connection.disconnect()
                    activeConnection.compareAndSet(connection, null)
                }
            }
        }

        awaitClose {
            readerJob.cancel()
            activeConnection.getAndSet(null)?.disconnect()
        }
    }

    private suspend fun SendChannel<SourceProcessingEvent>.emitSsePayload(payload: String): Boolean {
        val event = parseSseDataPayload(payload) ?: return false
        HttpDebugLogger.logEvent(
            "SSE source status: sourceId=${event.sourceId} " +
                "state=${event.state} progress=${event.progress}",
        )
        send(event)
        return true
    }

    private suspend fun scheduleSseReconnect(
        delayMillis: suspend (Long) -> Unit,
        reconnectAttempt: Int,
        reason: String,
    ): Boolean {
        if (reconnectAttempt > MAX_SSE_RECONNECT_ATTEMPTS) {
            HttpDebugLogger.logEvent(
                "SSE reconnect exhausted after $MAX_SSE_RECONNECT_ATTEMPTS attempts ($reason)",
            )
            return false
        }
        val waitMs = sseBackoffMillis(reconnectAttempt)
        HttpDebugLogger.logEvent(
            "SSE reconnect attempt=$reconnectAttempt/$MAX_SSE_RECONNECT_ATTEMPTS " +
                "delayMs=$waitMs ($reason)",
        )
        delayMillis(waitMs)
        return true
    }

    /** Exponential backoff for SSE reconnect attempts (1-based). Caps at 16s. */
    fun sseBackoffMillis(attempt: Int): Long {
        require(attempt >= 1) { "attempt must be >= 1" }
        val shift = (attempt - 1).coerceAtMost(4)
        val delay = SSE_BACKOFF_BASE_MS shl shift
        return delay.coerceAtMost(SSE_BACKOFF_MAX_MS)
    }

    fun parseSseDataPayload(payload: String): SourceProcessingEvent? {
        if (payload.isBlank()) return null
        val sourceId = SourcesJsonParsers.matchJsonString(payload, "sourceId") ?: return null
        val stateRaw = SourcesJsonParsers.matchJsonString(payload, "state").orEmpty()
        val state = SourceProcessingEvent.parseState(stateRaw)
        val progress = SourcesJsonParsers.matchJsonInt(payload, "progress")
            ?: defaultProgressFor(state)
        return SourceProcessingEvent(
            sourceId = sourceId,
            state = state,
            progress = progress.coerceIn(0, 100),
        )
    }

    fun defaultProgressFor(state: SourceProcessingState): Int = when (state) {
        SourceProcessingState.ADDED -> 0
        SourceProcessingState.EXTRACTING_TEXT -> 25
        SourceProcessingState.INDEXING_EVIDENCE -> 50
        SourceProcessingState.READY,
        SourceProcessingState.FAILED,
        -> 100
    }
}
