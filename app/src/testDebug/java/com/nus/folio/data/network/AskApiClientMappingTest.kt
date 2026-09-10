package com.nus.folio.data.network

import com.nus.folio.domain.model.AskConversation
import com.nus.folio.domain.model.AskConversationRole
import com.nus.folio.domain.model.AskFeedbackRating
import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AskApiClientMappingTest {

    @Test
    fun `spaceAsk path encodes space id`() {
        assertEquals(
            "https://example.test/api/v1/spaces/space%2F1/ask",
            FolioApiPaths.spaceAsk("space/1", baseUrl = "https://example.test"),
        )
        assertEquals(
            "https://example.test/api/v1/spaces/space%2F1/ask/suggestions?scope=source&sourceId=src-1",
            FolioApiPaths.spaceAskSuggestions(
                spaceId = "space/1",
                baseUrl = "https://example.test",
                query = "scope=source&sourceId=src-1",
            ),
        )
    }

    @Test
    fun `buildAskRequestJson uses space scope without sourceId`() {
        val json = AskApiClient.buildAskRequestJson(
            question = "What problems appear most often?",
            sourceId = null,
            conversationId = null,
        )
        assertTrue(json.contains("\"question\":\"What problems appear most often?\""))
        assertTrue(json.contains("\"scope\":\"space\""))
        assertFalse(json.contains("sourceId"))
        assertFalse(json.contains("conversationId"))
    }

    @Test
    fun `buildAskFeedbackJson writes rating`() {
        assertEquals(
            """{"rating":"useful"}""",
            AskApiClient.buildAskFeedbackJson("useful"),
        )
        assertEquals(
            """{"rating":"not_useful"}""",
            AskApiClient.buildAskFeedbackJson("not_useful"),
        )
    }

    @Test
    fun `buildUpdateConversationJson writes title`() {
        assertEquals(
            """{"title":"Q4 operating costs"}""",
            AskApiClient.buildUpdateConversationJson("Q4 operating costs"),
        )
        assertEquals(
            """{"title":"Say \"hello\""}""",
            AskApiClient.buildUpdateConversationJson("Say \"hello\""),
        )
    }

    @Test
    fun `parseUpdatedConversation reads conversation title`() {
        val payload = """
            {
              "status": "success",
              "data": {
                "conversation": {
                  "id": "conv-1",
                  "researchSpaceId": "space-1",
                  "title": "Q4 operating costs",
                  "updatedAt": "2026-08-20T07:54:43.859Z"
                }
              }
            }
        """.trimIndent()
        val parsed = AskApiClient.parseUpdatedConversation(
            payload = payload,
            fallback = AskConversation(
                id = "conv-1",
                title = "Old title",
                dateLabel = "Aug 19, 14:12",
                spaceId = "space-1",
            ),
        )
        assertEquals("conv-1", parsed.id)
        assertEquals("Q4 operating costs", parsed.title)
        assertEquals("space-1", parsed.spaceId)
        assertTrue(parsed.dateLabel.isNotBlank())
    }

    @Test
    fun `parseUpdatedConversation uses fallback when body is empty`() {
        val fallback = AskConversation(
            id = "conv-1",
            title = "Q4 operating costs",
            dateLabel = "Aug 20, 07:54",
            spaceId = "space-1",
        )
        assertEquals(fallback, AskApiClient.parseUpdatedConversation("", fallback))
    }

    @Test
    fun `spaceAskMessageFeedback path encodes ids`() {
        assertEquals(
            "https://example.test/api/v1/spaces/space%2F1/conversations/conv%2F2/messages/msg%2F3/feedback",
            FolioApiPaths.spaceAskMessageFeedback(
                spaceId = "space/1",
                conversationId = "conv/2",
                messageId = "msg/3",
                baseUrl = "https://example.test",
            ),
        )
    }

    @Test
    fun `buildAskRequestJson uses source scope and conversationId`() {
        val json = AskApiClient.buildAskRequestJson(
            question = "Summarize this paper",
            sourceId = "src-9",
            conversationId = "conv-1",
        )
        assertTrue(json.contains("\"scope\":\"source\""))
        assertTrue(json.contains("\"sourceId\":\"src-9\""))
        assertTrue(json.contains("\"conversationId\":\"conv-1\""))
    }

    @Test
    fun `parseAskSseEvent start token citations and done`() {
        val start = AskApiClient.parseAskSseEvent(
            "start",
            """{"conversationId":"conv-1","messageId":"msg-1"}""",
        ) as AskSseParseResult.Event
        val started = start.event as AskStreamEvent.Started
        assertEquals("conv-1", started.conversationId)
        assertEquals("msg-1", started.messageId)

        val token = AskApiClient.parseAskSseEvent(
            "token",
            """{"text":"Hello "}""",
        ) as AskSseParseResult.Event
        assertEquals("Hello ", (token.event as AskStreamEvent.Delta).text)

        val citations = AskApiClient.parseAskSseEvent(
            "citations",
            """{"citations":[{"sourceId":"src-1","sourceTitle":"Paper","sourceType":"file","fileExtension":"pdf","page":14,"evidenceText":"imitation game"}]}""",
        ) as AskSseParseResult.Event
        val citation = (citations.event as AskStreamEvent.Citations).citations.single()
        assertEquals(1, citation.index)
        assertEquals("src-1", citation.sourceId)
        assertEquals("Paper", citation.sourceTitle)
        assertEquals(SourceType.FILE, citation.sourceType)
        assertEquals("pdf", citation.fileExtension)
        assertEquals("Page 14", citation.locationLabel)
        assertEquals("imitation game", citation.evidenceText)

        val done = AskApiClient.parseAskSseEvent(
            "done",
            """{"messageId":"msg-1","content":"Canonical [1].","citations":[{"sourceId":"src-1","title":"Paper"}],"limitation":"Sparse evidence","stopped":false}""",
        ) as AskSseParseResult.Event
        val completed = done.event as AskStreamEvent.Completed
        assertEquals("msg-1", completed.messageId)
        assertEquals("Canonical [1].", completed.content)
        assertEquals("Sparse evidence", completed.limitation)
        assertFalse(completed.stopped)
        assertEquals("src-1", completed.citations.single().sourceId)
    }

    @Test
    fun `parseAskSseEvent uses type field when event name blank`() {
        val result = AskApiClient.parseAskSseEvent(
            "",
            """{"type":"token","text":"delta"}""",
        ) as AskSseParseResult.Event
        assertEquals("delta", (result.event as AskStreamEvent.Delta).text)
    }

    @Test
    fun `parseAskSseEvent error becomes failure`() {
        val result = AskApiClient.parseAskSseEvent(
            "error",
            """{"message":"Generation failed","code":"ASK_FAILED"}""",
        ) as AskSseParseResult.Failure
        assertEquals("Generation failed (ASK_FAILED)", result.error.message)
    }

    @Test
    fun `parseAskSseEvent done with stopped true`() {
        val result = AskApiClient.parseAskSseEvent(
            "done",
            """{"messageId":"msg-2","content":"Partial.","citations":[],"limitation":null,"stopped":true}""",
        ) as AskSseParseResult.Event
        val completed = result.event as AskStreamEvent.Completed
        assertTrue(completed.stopped)
        assertEquals("Partial.", completed.content)
        assertTrue(completed.citations.isEmpty())
        assertEquals(null, completed.limitation)
    }

    @Test
    fun `parseAskSseEvent token preserves surrounding spaces`() {
        val result = AskApiClient.parseAskSseEvent(
            "token",
            """{"text":" world"}""",
        ) as AskSseParseResult.Event
        assertEquals(" world", (result.event as AskStreamEvent.Delta).text)
    }

    @Test
    fun `parseAskSseEvent token keeps literal backslash-n`() {
        val result = AskApiClient.parseAskSseEvent(
            "token",
            """{"text":"a\\nb"}""",
        ) as AskSseParseResult.Event
        assertEquals("a\\nb", (result.event as AskStreamEvent.Delta).text)
    }

    @Test
    fun `parseAskSseEvent token decodes json string escapes`() {
        val result = AskApiClient.parseAskSseEvent(
            "token",
            """{"text":"a\n b\r c\t d\/ e\b f\f g\"h \\u0041"}""",
        ) as AskSseParseResult.Event
        assertEquals(
            "a\n b\r c\t d/ e\b f\u000C g\"h \\u0041",
            (result.event as AskStreamEvent.Delta).text,
        )
    }

    @Test
    fun `parseAskSseEvent token decodes unicode escapes`() {
        val result = AskApiClient.parseAskSseEvent(
            "token",
            """{"text":"caf\u00e9"}""",
        ) as AskSseParseResult.Event
        assertEquals("café", (result.event as AskStreamEvent.Delta).text)
    }

    @Test
    fun `parseAskSseEvent done decodes json string escapes in content`() {
        val result = AskApiClient.parseAskSseEvent(
            "done",
            """{"messageId":"msg-1","content":"line\nnext\t\"quote\" \\u0041","citations":[]}""",
        ) as AskSseParseResult.Event
        val completed = result.event as AskStreamEvent.Completed
        assertEquals("line\nnext\t\"quote\" \\u0041", completed.content)
    }

    @Test
    fun `parseCitations numbers by array order when index omitted`() {
        val citations = AskApiClient.parseCitations(
            """{"citations":[{"sourceId":"a","title":"One"},{"sourceId":"b","title":"Two"}]}""",
        )
        assertEquals(listOf(1, 2), citations.map { it.index })
        assertEquals(listOf("a", "b"), citations.map { it.sourceId })
    }

    @Test
    fun `parseAskSuggestions reads questions and isDynamic`() {
        val dynamic = AskApiClient.parseAskSuggestions(
            """{"suggestions":["Q1","Q2","Q3"],"isDynamic":true}""",
        )
        assertEquals(listOf("Q1", "Q2", "Q3"), dynamic.questions)
        assertTrue(dynamic.isDynamic)

        val generics = AskApiClient.parseAskSuggestions(
            """{"questions":["Summarize all the evidence."],"isDynamic":false}""",
        )
        assertEquals(listOf("Summarize all the evidence."), generics.questions)
        assertFalse(generics.isDynamic)
    }

    @Test
    fun `parseAskSuggestions reads object array items`() {
        val parsed = AskApiClient.parseAskSuggestions(
            """{"suggestions":[{"question":"What is the claim?"},{"text":"How does it work?"}]}""",
        )
        assertEquals(
            listOf("What is the claim?", "How does it work?"),
            parsed.questions,
        )
    }

    @Test
    fun `parseAskSuggestions caps at three questions`() {
        val parsed = AskApiClient.parseAskSuggestions(
            """{"suggestions":["A","B","C","D"],"isDynamic":true}""",
        )
        assertEquals(listOf("A", "B", "C"), parsed.questions)
    }

    @Test
    fun `parseAskSuggestions decodes json string escapes`() {
        val parsed = AskApiClient.parseAskSuggestions(
            """{"suggestions":["line\nnext","caf\u00e9","a\\nb"],"isDynamic":true}""",
        )
        assertEquals(listOf("line\nnext", "café", "a\\nb"), parsed.questions)
    }

    @Test
    fun `parseConversations reads list items and dates`() {
        val payload = """
            {
              "status": "success",
              "data": {
                "conversations": [
                  {
                    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                    "title": "What were the operating costs in Q4?",
                    "createdAt": "2026-08-20T07:54:43.859Z",
                    "updatedAt": "2026-08-20T07:54:43.859Z"
                  }
                ]
              }
            }
        """.trimIndent()
        val parsed = AskApiClient.parseConversations(payload)
        assertEquals(1, parsed.size)
        assertEquals("3fa85f64-5717-4562-b3fc-2c963f66afa6", parsed[0].id)
        assertEquals("What were the operating costs in Q4?", parsed[0].title)
        assertTrue(parsed[0].dateLabel.isNotBlank())
    }

    @Test
    fun `buildConversationsQuery includes page limit and search`() {
        assertEquals(
            "page=1&limit=10",
            AskApiClient.buildConversationsQuery(search = null, page = 1, limit = 10),
        )
        assertEquals(
            "page=2&limit=10&search=operating+costs",
            AskApiClient.buildConversationsQuery(search = "operating costs", page = 2, limit = 10),
        )
    }

    @Test
    fun `parseConversationsPage reads pagination hasMore`() {
        val payload = """
            {
              "status": "success",
              "data": {
                "conversations": [
                  {
                    "id": "conv-1",
                    "title": "What were the operating costs in Q4?",
                    "updatedAt": "2026-08-20T07:54:43.859Z"
                  }
                ],
                "pagination": {
                  "page": 1,
                  "limit": 10,
                  "totalCount": 12,
                  "totalPages": 2
                }
              }
            }
        """.trimIndent()
        val parsed = AskApiClient.parseConversationsPage(payload, page = 1, limit = 10)
        assertEquals(1, parsed.conversations.size)
        assertEquals(1, parsed.page)
        assertEquals(10, parsed.limit)
        assertEquals(12, parsed.totalCount)
        assertTrue(parsed.hasMore)
    }

    @Test
    fun `parseConversationDetail maps messages citations and feedback`() {
        val payload = """
            {
              "status": "success",
              "data": {
                "conversation": {
                  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "researchSpaceId": "space-1",
                  "title": "Q4 costs",
                  "messages": [
                    {
                      "id": "user-1",
                      "role": "user",
                      "content": "What were the operating costs in Q4?"
                    },
                    {
                      "id": "msg-1",
                      "role": "assistant",
                      "content": "Staffing drove Q4 costs [1].",
                      "citations": [
                        {
                          "id": "cite-1",
                          "sourceId": "src-1",
                          "sourceTitle": "Onboarding Benchmark Report",
                          "sourceType": "File",
                          "snippet": "Q4 operating costs",
                          "locationLabel": "Page 14"
                        }
                      ],
                      "limitation": "Only one source contains a primary provider interview.",
                      "feedback": "useful",
                      "savedNoteId": "note-1",
                      "stopped": true
                    }
                  ]
                }
              }
            }
        """.trimIndent()
        val parsed = AskApiClient.parseConversationDetail(payload)
        assertEquals("3fa85f64-5717-4562-b3fc-2c963f66afa6", parsed.conversation.id)
        assertEquals("space-1", parsed.conversation.spaceId)
        assertEquals(2, parsed.messages.size)
        assertEquals(AskConversationRole.USER, parsed.messages[0].role)
        val assistant = parsed.messages[1]
        assertEquals(AskConversationRole.ASSISTANT, assistant.role)
        assertEquals("Staffing drove Q4 costs [1].", assistant.content)
        assertEquals(AskFeedbackRating.USEFUL, assistant.feedback)
        assertEquals("note-1", assistant.savedNoteId)
        assertTrue(assistant.stopped)
        assertEquals("Only one source contains a primary provider interview.", assistant.limitation)
        assertEquals(1, assistant.citations.size)
        assertEquals("src-1", assistant.citations[0].sourceId)
        assertEquals("Onboarding Benchmark Report", assistant.citations[0].sourceTitle)
        assertEquals("Page 14", assistant.citations[0].locationLabel)
        assertEquals("Q4 operating costs", assistant.citations[0].evidenceText)
        assertNull(parsed.conversation.sourceId)
    }

    @Test
    fun `parseConversationDetail reads source scope sourceId`() {
        val payload = """
            {
              "status": "success",
              "data": {
                "conversation": {
                  "id": "conv-1",
                  "researchSpaceId": "space-1",
                  "title": "Q4 costs",
                  "scope": {
                    "type": "source",
                    "sourceId": "src-9"
                  },
                  "messages": [
                    {
                      "id": "msg-1",
                      "role": "assistant",
                      "content": "Staffing drove Q4 costs [1].",
                      "citations": [
                        {
                          "sourceId": "cite-src",
                          "sourceTitle": "Other paper"
                        }
                      ]
                    }
                  ]
                }
              }
            }
        """.trimIndent()
        val parsed = AskApiClient.parseConversationDetail(payload)
        assertEquals("src-9", parsed.conversation.sourceId)
        assertEquals("cite-src", parsed.messages.single().citations.single().sourceId)
    }

    @Test
    fun `parseConversationDetail space scope ignores nested sourceId`() {
        val payload = """
            {
              "conversation": {
                "id": "conv-1",
                "researchSpaceId": "space-1",
                "title": "Across sources",
                "scope": {
                  "type": "space",
                  "sourceId": "src-should-ignore"
                },
                "messages": [
                  {
                    "id": "msg-1",
                    "role": "assistant",
                    "content": "Answer [1].",
                    "citations": [{"sourceId": "src-1"}]
                  }
                ]
              }
            }
        """.trimIndent()
        val parsed = AskApiClient.parseConversationDetail(payload)
        assertNull(parsed.conversation.sourceId)
    }

    @Test
    fun `parseCitations uses snippet and pageReference when evidenceText omitted`() {
        val citations = AskApiClient.parseCitations(
            """{"citations":[{"sourceId":"src-1","sourceTitle":"Onboarding Benchmark Report","snippet":"Q4 operating costs","pageReference":"14"}]}""",
        )
        assertEquals(1, citations.size)
        assertEquals("Q4 operating costs", citations[0].evidenceText)
        assertEquals("Page 14", citations[0].locationLabel)
        assertEquals("Onboarding Benchmark Report", citations[0].sourceTitle)
    }
}
