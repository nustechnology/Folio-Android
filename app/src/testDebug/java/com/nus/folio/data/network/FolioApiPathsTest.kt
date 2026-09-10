package com.nus.folio.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolioApiPathsTest {

    @Test
    fun `debug base url is the production Folio host`() {
        assertEquals("https://folio.nustechnology.com", FolioApiPaths.BASE_URL)
    }

    @Test
    fun `auth paths resolve against base url`() {
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/auth/login",
            FolioApiPaths.authLogin(),
        )
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/auth/sign-up",
            FolioApiPaths.authSignUp(),
        )
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/auth/refresh",
            FolioApiPaths.authRefresh(),
        )
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/auth/logout",
            FolioApiPaths.authLogout(),
        )
    }

    @Test
    fun `user path encodes id`() {
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/users/user%2Fid",
            FolioApiPaths.user("user/id"),
        )
    }

    @Test
    fun `spaces and sources support optional query`() {
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/spaces",
            FolioApiPaths.spaces(),
        )
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/spaces?sort=recent",
            FolioApiPaths.spaces(query = "sort=recent"),
        )
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/spaces/space%2Fid",
            FolioApiPaths.space("space/id"),
        )
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/sources?spaceId=1",
            FolioApiPaths.sources(query = "spaceId=1"),
        )
    }

    @Test
    fun `source nested paths`() {
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/sources/abc",
            FolioApiPaths.source("abc"),
        )
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/sources/abc/retry",
            FolioApiPaths.sourceRetry("abc"),
        )
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/sources/abc/preview",
            FolioApiPaths.sourcePreview("abc"),
        )
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/sources/status",
            FolioApiPaths.sourcesStatus(),
        )
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/spaces/space%2Fid/ask",
            FolioApiPaths.spaceAsk("space/id"),
        )
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/spaces/space%2Fid/ask/suggestions",
            FolioApiPaths.spaceAskSuggestions("space/id"),
        )
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/spaces/abc/ask/suggestions?scope=source&sourceId=1",
            FolioApiPaths.spaceAskSuggestions("abc", query = "scope=source&sourceId=1"),
        )
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/spaces/space%2Fid/notebook",
            FolioApiPaths.spaceNotebook("space/id"),
        )
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/spaces/space%2Fid/conversations",
            FolioApiPaths.spaceConversations("space/id"),
        )
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/spaces/space%2Fid/conversations?page=2&limit=10&search=q4",
            FolioApiPaths.spaceConversations("space/id", query = "page=2&limit=10&search=q4"),
        )
        assertEquals(
            "${FolioApiPaths.BASE_URL}/api/v1/spaces/space%2Fid/conversations/conv%2F1",
            FolioApiPaths.spaceConversation("space/id", "conv/1"),
        )
    }

    @Test
    fun `custom base url is respected`() {
        val custom = "http://localhost:8080"
        assertTrue(FolioApiPaths.authLogin(custom).startsWith(custom))
        assertEquals("$custom/api/v1/sources/1", FolioApiPaths.source("1", custom))
    }
}
