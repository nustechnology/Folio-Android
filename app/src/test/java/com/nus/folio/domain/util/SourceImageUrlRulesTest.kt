package com.nus.folio.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceImageUrlRulesTest {
    private val apiBase = "https://folio.nustechnology.com"

    @Test
    fun `allows Folio external relative and data images`() {
        assertTrue(
            SourceImageUrlRules.isAllowed("https://folio.nustechnology.com/media/a.png", apiBase),
        )
        assertTrue(SourceImageUrlRules.isAllowed("https://cdn.example/track.png", apiBase))
        assertTrue(SourceImageUrlRules.isAllowed("//cdn.example/x.png", apiBase))
        assertTrue(SourceImageUrlRules.isAllowed("/media/a.png", apiBase))
        assertTrue(SourceImageUrlRules.isAllowed("images/a.png", apiBase))
        assertTrue(SourceImageUrlRules.isAllowed("data:image/png;base64,abc", apiBase))
        assertTrue(SourceImageUrlRules.isAllowed("", apiBase))
    }

    @Test
    fun `rejects local network and dangerous image hosts`() {
        assertFalse(SourceImageUrlRules.isAllowed("http://127.0.0.1/secret.png", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("http://192.168.1.10/cam.png", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("http://10.0.0.5/internal.png", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("http://169.254.169.254/latest/meta", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("http://localhost/secret.png", apiBase))
    }

    @Test
    fun `rejects scriptable and non-image schemes`() {
        assertFalse(SourceImageUrlRules.isAllowed("javascript:alert(1)", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("data:text/html,<script>", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("file:///etc/passwd", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("vbscript:msgbox(1)", apiBase))
    }

    @Test
    fun `neutralizeDisallowedSources blanks only disallowed src values`() {
        val html =
            """<p><img src="javascript:alert(1)"><img src="https://cdn.example/a.png"></p>"""
        assertEquals(
            """<p><img src=""><img src="https://cdn.example/a.png"></p>""",
            SourceImageUrlRules.neutralizeDisallowedSources(html, apiBase),
        )
    }

    @Test
    fun `prepareSources absolutizes relative paths and blanks dangerous src`() {
        val html =
            """<p><img src="/media/a.png"><img src="images/b.png"><img src="https://cdn.example/c.png"><img src="javascript:alert(1)"></p>"""
        assertEquals(
            """<p><img src="https://folio.nustechnology.com/media/a.png"><img src="https://folio.nustechnology.com/images/b.png"><img src="https://cdn.example/c.png"><img src=""></p>""",
            SourceImageUrlRules.prepareSources(html, apiBase),
        )
    }
}
