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
        // IPv4-mapped IPv6 (dotted and hexadecimal) must hit the same private checks.
        assertFalse(SourceImageUrlRules.isAllowed("http://[::ffff:127.0.0.1]/secret.png", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("http://[::ffff:7f00:1]/secret.png", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("https://[::ffff:c0a8:10a]/cam.png", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("https://[0:0:0:0:0:ffff:7f00:1]/x.png", apiBase))
        assertTrue(SourceImageUrlRules.isAllowed("https://[::ffff:808:808]/public.png", apiBase))
        // Expanded IPv6 loopback / unspecified (prefix 0) must not be treated as public.
        assertFalse(SourceImageUrlRules.isAllowed("http://[0:0:0:0:0:0:0:1]/secret.png", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("http://[0:0:0:0:0:0:0:0]/secret.png", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("http://[::1]/secret.png", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("http://[::]/secret.png", apiBase))
    }

    @Test
    fun `rejects legacy numeric IPv4 loopback and private forms`() {
        // Chromium resolves these to 127.0.0.1 / private ranges; must not pass as hostnames.
        assertFalse(SourceImageUrlRules.isAllowed("http://2130706433/secret.png", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("http://0177.0.0.1/secret.png", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("http://0x7f000001/secret.png", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("http://0x7f.0.0.1/secret.png", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("http://127.1/secret.png", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("http://0x7f.1/secret.png", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("https://3232235876/cam.png", apiBase)) // 192.168.1.100
        assertFalse(SourceImageUrlRules.isAllowed("http://[::ffff:2130706433]/secret.png", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("http://[::ffff:0177.0.0.1]/secret.png", apiBase))
        // Unparseable numeric shapes must fail closed (not treated as public DNS names).
        assertFalse(SourceImageUrlRules.isAllowed("http://256.1.1.1/x.png", apiBase))
        assertFalse(SourceImageUrlRules.isAllowed("http://0xffffffffff/x.png", apiBase))
        // Public dotted / decimal IPv4 remains allowed.
        assertTrue(SourceImageUrlRules.isAllowed("https://8.8.8.8/public.png", apiBase))
        assertTrue(SourceImageUrlRules.isAllowed("https://0x8080808/public.png", apiBase)) // 8.8.8.8
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

    @Test
    fun `prepareSources strips srcset and imagesrcset loopback bypass`() {
        val html =
            """<img src="https://cdn.example/ok.png" srcset="http://127.0.0.1/secret.png 1x, https://cdn.example/ok.png 2x">""" +
                """<source imagesrcset="http://localhost/x.png 1x">"""
        val prepared = SourceImageUrlRules.prepareSources(html, apiBase)
        assertEquals(
            """<img src="https://cdn.example/ok.png"><source>""",
            prepared,
        )
        assertFalse(prepared.contains("127.0.0.1"))
        assertFalse(prepared.contains("localhost"))
        assertFalse(prepared.contains("srcset", ignoreCase = true))
    }
}
