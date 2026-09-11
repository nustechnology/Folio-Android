package com.nus.folio.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotebookHtmlTest {

    @Test
    fun `markdownToHtml returns empty for blank markdown`() {
        assertEquals("", NotebookHtml.markdownToHtml(""))
        assertEquals("", NotebookHtml.markdownToHtml("   \n"))
    }

    @Test
    fun `markdownToHtml renders headings emphasis lists and links`() {
        val html = NotebookHtml.markdownToHtml(
            "# Title\n\n**bold** and _italic_ and [docs](https://example.test)\n\n- one\n- two",
        )
        assertTrue(html.contains("<h1>Title</h1>"))
        assertTrue(html.contains("<strong>bold</strong>"))
        assertTrue(html.contains("<em>italic</em>"))
        assertTrue(html.contains("""<a href="https://example.test">docs</a>"""))
        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<li>one</li>"))
        assertTrue(html.contains("<li>two</li>"))
    }

    @Test
    fun `markdownToHtml keeps http https mailto and relative links`() {
        assertTrue(
            NotebookHtml.markdownToHtml("[docs](http://example.test)").contains(
                """<a href="http://example.test">docs</a>""",
            ),
        )
        assertTrue(
            NotebookHtml.markdownToHtml("[mail](mailto:a@b.test)").contains(
                """<a href="mailto:a@b.test">mail</a>""",
            ),
        )
        assertTrue(
            NotebookHtml.markdownToHtml("[notes](/notebook#top)").contains(
                """<a href="/notebook#top">notes</a>""",
            ),
        )
    }

    @Test
    fun `markdownToHtml renders disallowed link schemes as plain text`() {
        val html = NotebookHtml.markdownToHtml("[x](javascript:alert(1)) and [y](data:text/html,hi)")
        assertTrue(!html.contains("<a ", ignoreCase = true))
        assertTrue(!html.contains("javascript:", ignoreCase = true))
        assertTrue(!html.contains("data:", ignoreCase = true))
        assertTrue(html.contains("x"))
        assertTrue(html.contains("y"))
    }

    @Test
    fun `markdownToHtml rejects malformed schemes with control characters`() {
        val html = NotebookHtml.markdownToHtml("[x](java\u0000script:alert(1))")
        assertTrue(!html.contains("<a ", ignoreCase = true))
        assertTrue(!html.contains("javascript:", ignoreCase = true))
        assertTrue(html.contains("x"))
    }

    @Test
    fun `markdownToHtml keeps relative paths with a colon after slash`() {
        val html = NotebookHtml.markdownToHtml("[notes](folder/id:123)")
        assertTrue(html.contains("""<a href="folder/id:123">notes</a>"""))
    }

    @Test
    fun `markdownToHtml does not italicize underscores inside words`() {
        val markdown = "space_id and user_name and _italic_"
        val html = NotebookHtml.markdownToHtml(markdown)
        assertTrue(html.contains("space_id"))
        assertTrue(html.contains("user_name"))
        assertTrue(!html.contains("space<em>"))
        assertTrue(!html.contains("user</em>"))
        assertTrue(html.contains("<em>italic</em>"))
        assertEquals(markdown, NotebookHtml.htmlToMarkdown(html))
    }

    @Test
    fun `markdownToHtml renders ordered lists`() {
        val html = NotebookHtml.markdownToHtml("1. first\n2. second")
        assertEquals("<ol><li>first</li><li>second</li></ol>", html)
    }

    @Test
    fun `markdownToHtml renders adjacent bullet and ordered lists`() {
        assertEquals(
            "<ul><li>bullet one</li></ul><ol><li>ordered two</li></ol>",
            NotebookHtml.markdownToHtml("- bullet one\n1. ordered two"),
        )
        assertEquals(
            "<ol><li>first</li></ol><ul><li>second</li></ul>",
            NotebookHtml.markdownToHtml("1. first\n- second"),
        )
        assertEquals(
            "<ul><li>a</li></ul><ol><li>b</li></ol><ul><li>c</li></ul>",
            NotebookHtml.markdownToHtml("- a\n1. b\n- c"),
        )
    }

    @Test
    fun `htmlToMarkdown round trips adjacent bullet and ordered lists without losing items`() {
        val cases = listOf(
            "- bullet one\n1. ordered two",
            "1. first\n- second",
            "- a\n1. b\n- c",
        )
        for (markdown in cases) {
            val html = NotebookHtml.markdownToHtml(markdown)
            val roundTripped = NotebookHtml.htmlToMarkdown(html)
            for (item in markdown.lines().mapNotNull { line ->
                line.replace(Regex("""^[-*]\s+"""), "")
                    .replace(Regex("""^\d+\.\s+"""), "")
                    .takeIf { it.isNotBlank() }
            }) {
                assertTrue(
                    "Missing \"$item\" after round-trip of:\n$markdown\n→ $html\n→ $roundTripped",
                    roundTripped.contains(item),
                )
            }
        }
    }

    @Test
    fun `markdownToHtml renders blockquote`() {
        val html = NotebookHtml.markdownToHtml("> A wise note")
        assertEquals("<blockquote>A wise note</blockquote>", html)
    }

    @Test
    fun `htmlToMarkdown returns empty for blank html`() {
        assertEquals("", NotebookHtml.htmlToMarkdown(""))
        assertEquals("", NotebookHtml.htmlToMarkdown("   "))
        assertEquals("", NotebookHtml.htmlToMarkdown("<p></p>"))
    }

    @Test
    fun `htmlToMarkdown converts API example payload`() {
        val markdown = NotebookHtml.htmlToMarkdown(
            "<h1>Aged-Care Operations</h1><p>Providers describe duplicate entry across systems.</p>",
        )
        assertEquals(
            "# Aged-Care Operations\n\nProviders describe duplicate entry across systems.",
            markdown,
        )
    }

    @Test
    fun `htmlToMarkdown round trips headings emphasis lists and links`() {
        val markdown = "# Title\n\n**bold** and _italic_ and [docs](https://example.test)\n\n- one\n- two"
        val roundTripped = NotebookHtml.htmlToMarkdown(NotebookHtml.markdownToHtml(markdown))
        assertEquals(markdown, roundTripped)
    }

    @Test
    fun `htmlToMarkdown round trips ordered lists and blockquotes`() {
        val markdown = "1. first\n2. second\n\n> Line one\n> Line two"
        val roundTripped = NotebookHtml.htmlToMarkdown(NotebookHtml.markdownToHtml(markdown))
        assertEquals(markdown, roundTripped)
    }

    @Test
    fun `htmlToMarkdown preserves nested lists`() {
        val markdown = NotebookHtml.htmlToMarkdown(
            "<ul><li>Parent<ul><li>Child</li></ul></li></ul>",
        )
        assertEquals("- Parent\n  - Child", markdown)
        assertEquals(
            markdown,
            NotebookHtml.htmlToMarkdown(NotebookHtml.markdownToHtml(markdown)),
        )
    }

    @Test
    fun `htmlToMarkdown preserves paragraph breaks inside blockquotes`() {
        val markdown = NotebookHtml.htmlToMarkdown(
            "<blockquote><p>First para.</p><p>Second para.</p></blockquote>",
        )
        assertEquals("> First para.\n>\n> Second para.", markdown)
        assertEquals(
            markdown,
            NotebookHtml.htmlToMarkdown(NotebookHtml.markdownToHtml(markdown)),
        )
    }

    @Test
    fun `htmlToMarkdown keeps unsupported block text and flags conversion as lossy`() {
        val conversion = NotebookHtml.htmlToMarkdownConversion(
            "<h1>T</h1><h4>Sub</h4><pre>code()</pre><p>Body</p>",
        )
        assertEquals("# T\n\nSub\n\ncode()\n\nBody", conversion.markdown)
        assertTrue(conversion.isLossy)
    }

    @Test
    fun `htmlToMarkdown conversion is not lossy for the supported subset`() {
        val conversion = NotebookHtml.htmlToMarkdownConversion(
            "<h1>Title</h1><p><strong>bold</strong></p><ul><li>one</li></ul>",
        )
        assertEquals("# Title\n\n**bold**\n\n- one", conversion.markdown)
        assertEquals(false, conversion.isLossy)
    }

    @Test
    fun `htmlToMarkdown unwraps transparent wrappers without marking lossy`() {
        val div = NotebookHtml.htmlToMarkdownConversion(
            "<div><p>Hello</p><p>World</p></div>",
        )
        assertEquals("Hello\n\nWorld", div.markdown)
        assertEquals(false, div.isLossy)

        val span = NotebookHtml.htmlToMarkdownConversion("<p>a <span>b</span> c</p>")
        assertEquals("a b c", span.markdown)
        assertEquals(false, span.isLossy)

        val nested = NotebookHtml.htmlToMarkdownConversion(
            "<section><div><h1>Title</h1><p>Body</p></div></section>",
        )
        assertEquals("# Title\n\nBody", nested.markdown)
        assertEquals(false, nested.isLossy)
    }

    @Test
    fun `htmlToMarkdown decodes common rich-text named entities without marking lossy`() {
        val conversion = NotebookHtml.htmlToMarkdownConversion(
            "<p>don&rsquo;t &mdash; really &hellip; &ldquo;quoted&rdquo; &ndash; caf&eacute; &copy;</p>",
        )
        assertEquals(
            "don\u2019t \u2014 really \u2026 \u201Cquoted\u201D \u2013 caf\u00E9 \u00A9",
            conversion.markdown,
        )
        assertEquals(false, conversion.isLossy)
    }

    @Test
    fun `markdownToHtml joins adjacent paragraph lines with br`() {
        assertEquals(
            "<p>Line A<br/>Line B</p>",
            NotebookHtml.markdownToHtml("Line A\nLine B"),
        )
    }

    @Test
    fun `htmlToMarkdown round trips soft line breaks inside a paragraph`() {
        val markdown = "Line A\nLine B"
        assertEquals(markdown, NotebookHtml.htmlToMarkdown(NotebookHtml.markdownToHtml(markdown)))
    }

    @Test
    fun `htmlToMarkdown round trips a blank line as a paragraph break`() {
        val markdown = "Line A\n\nLine B"
        assertEquals(
            "<p>Line A</p>\n<p>Line B</p>",
            NotebookHtml.markdownToHtml(markdown),
        )
        assertEquals(markdown, NotebookHtml.htmlToMarkdown(NotebookHtml.markdownToHtml(markdown)))
    }

    @Test
    fun `htmlToMarkdown keeps href when the attribute follows a newline`() {
        val markdown = NotebookHtml.htmlToMarkdown(
            "<p><a\nhref=\"https://example.test\">docs</a></p>",
        )
        assertEquals("[docs](https://example.test)", markdown)
    }

    @Test
    fun `htmlToMarkdown keeps href when the attribute follows a tab`() {
        val markdown = NotebookHtml.htmlToMarkdown(
            "<p><a\thref=\"https://example.test\">docs</a></p>",
        )
        assertEquals("[docs](https://example.test)", markdown)
    }

    @Test
    fun `htmlToMarkdown decodes apostrophe and nbsp entities`() {
        val conversion = NotebookHtml.htmlToMarkdownConversion(
            "<p>It&#39;s &apos;quoted&apos; and a&nbsp;b</p>",
        )
        assertEquals("It's 'quoted' and a\u00A0b", conversion.markdown)
        assertEquals(false, conversion.isLossy)
    }

    @Test
    fun `htmlToMarkdown decodes hex numeric entities`() {
        val conversion = NotebookHtml.htmlToMarkdownConversion("<p>&#x27;</p>")
        assertEquals("'", conversion.markdown)
        assertEquals(false, conversion.isLossy)
    }

    @Test
    fun `htmlToMarkdown keeps well-formed unknown entities and is not lossy`() {
        val conversion = NotebookHtml.htmlToMarkdownConversion("<p>copy&notanentity;right</p>")
        assertEquals("copy&notanentity;right", conversion.markdown)
        assertEquals(false, conversion.isLossy)
    }

    @Test
    fun `htmlToMarkdown keeps Latin-1 symbol entities editable without marking lossy`() {
        val html =
            "<p>&laquo;French&raquo; 25&deg;C &bull; &euro; &times; &middot; &sect; &pound; &frac12;</p>"
        val conversion = NotebookHtml.htmlToMarkdownConversion(html)
        assertEquals(html.removePrefix("<p>").removeSuffix("</p>"), conversion.markdown)
        assertEquals(false, conversion.isLossy)
    }

    @Test
    fun `htmlToMarkdown preserves common Latin-1 symbol entities without marking lossy`() {
        val entities = listOf(
            "laquo", "raquo", "deg", "bull", "euro", "times", "middot",
            "sect", "pound", "frac12", "divide", "sup2", "dagger", "rarr",
        )
        for (name in entities) {
            val html = "<p>&$name;</p>"
            val conversion = NotebookHtml.htmlToMarkdownConversion(html)
            assertEquals("Expected not lossy for &$name;", false, conversion.isLossy)
            assertEquals("Expected preserved entity &$name;", "&$name;", conversion.markdown)
        }
    }

    @Test
    fun `htmlToMarkdown decodes HTML5 legacy uppercase entity spellings`() {
        val conversion = NotebookHtml.htmlToMarkdownConversion(
            "<p>&AMP; &COPY; &NBSP;x &QUOT;hi&QUOT;</p>",
        )
        assertEquals("& \u00A9 \u00A0x \"hi\"", conversion.markdown)
        assertEquals(false, conversion.isLossy)
    }

    @Test
    fun `markdownToHtml preserves undecodable entity references from html conversion`() {
        val html = "<p>25&deg;C and &laquo;quoted&raquo;</p>"
        val markdown = NotebookHtml.htmlToMarkdown(html)
        assertEquals("25&deg;C and &laquo;quoted&raquo;", markdown)
        assertEquals(html, NotebookHtml.markdownToHtml(markdown))
    }

    @Test
    fun `markdownToHtml preserves well-formed entities for round trip`() {
        val markdown = "French &laquo;quotes&raquo; and 25&deg;C"
        val html = NotebookHtml.markdownToHtml(markdown)
        assertTrue(html.contains("&laquo;"))
        assertTrue(html.contains("&raquo;"))
        assertTrue(html.contains("&deg;"))
        assertEquals(markdown, NotebookHtml.htmlToMarkdown(html))
    }

    @Test
    fun `htmlToMarkdown round trips literal decodable entity spellings`() {
        val cases = listOf(
            "Use &amp; to escape",
            "Write &lt;div&gt; here",
            "a&nbsp;b",
            "5 &lt; 7",
            "&copy; 2026",
            "R&D; team",
        )
        for (markdown in cases) {
            assertEquals(
                markdown,
                NotebookHtml.htmlToMarkdown(NotebookHtml.markdownToHtml(markdown)),
            )
        }
    }
}
