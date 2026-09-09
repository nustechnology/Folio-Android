package com.nus.folio.domain.util

/**
 * Converts notebook markdown to the HTML subset the Folio notebook API accepts
 * (bold, italic, H1–H3, lists, blockquote, links), and the reverse.
 *
 * [htmlToMarkdownConversion] walks a small tokenizer over the allowed tag set so nested
 * lists and blockquotes keep their structure. Transparent wrappers (e.g. div/span) are
 * unwrapped without marking the conversion lossy. Tags that carry structure this markdown
 * subset cannot express (table, pre, img, h4+, …) are still unwrapped for display and
 * flagged [Conversion.isLossy] so callers can refuse to write the reduced document back.
 */
object NotebookHtml {
    data class Conversion(
        val markdown: String,
        val isLossy: Boolean,
    )

    fun markdownToHtml(markdown: String): String {
        if (markdown.isBlank()) return ""
        val blocks = markdown.replace("\r\n", "\n").split("\n\n")
        return blocks.joinToString("\n") { block -> renderBlock(block) }
    }

    /**
     * Converts the notebook API HTML subset back to markdown for the editor.
     * Blank HTML, or markup that carries no text, becomes an empty string.
     */
    fun htmlToMarkdown(html: String): String = htmlToMarkdownConversion(html).markdown

    fun htmlToMarkdownConversion(html: String): Conversion {
        if (html.isBlank()) return Conversion(markdown = "", isLossy = false)
        val (nodes, parseLossy) = parseFragment(html.trim())
        val converter = MarkdownConverter()
        val markdown = converter.convert(nodes)
        return Conversion(
            markdown = markdown,
            isLossy = parseLossy || converter.isLossy,
        )
    }

    private fun renderBlock(block: String): String {
        val lines = block.split('\n')
        val segments = mutableListOf<String>()
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            when {
                isBlockquoteLine(line) -> {
                    val quoteLines = mutableListOf<String>()
                    while (index < lines.size && isBlockquoteLine(lines[index])) {
                        quoteLines += lines[index]
                        index++
                    }
                    segments += renderBlockquote(quoteLines)
                }
                isListLine(line) ||
                    (line.isBlank() && index + 1 < lines.size && isListLine(lines[index + 1])) -> {
                    val listLines = mutableListOf<String>()
                    while (index < lines.size && (isListLine(lines[index]) || lines[index].isBlank())) {
                        if (lines[index].isNotBlank()) listLines += lines[index]
                        index++
                    }
                    segments += renderMarkdownList(listLines)
                }
                isHeadingLine(line) -> {
                    segments += renderHeading(line)
                    index++
                }
                line.isBlank() -> index++
                else -> {
                    val paragraphLines = mutableListOf<String>()
                    while (index < lines.size && isPlainParagraphLine(lines[index])) {
                        paragraphLines += lines[index]
                        index++
                    }
                    if (paragraphLines.isNotEmpty()) {
                        segments += renderParagraph(paragraphLines)
                    }
                }
            }
        }
        return segments.joinToString("\n")
    }

    private fun renderBlockquote(lines: List<String>): String {
        val quote = lines.joinToString("\n") { blockquoteContent(it).trimEnd() }
        val paragraphs = quote.split("\n\n")
        if (paragraphs.size == 1) {
            return "<blockquote>${inlineMarkdown(escapeHtml(paragraphs[0]).replace("\n", "<br/>"))}</blockquote>"
        }
        val inner = paragraphs.joinToString("") { paragraph ->
            "<p>${inlineMarkdown(escapeHtml(paragraph.trim()).replace("\n", "<br/>"))}</p>"
        }
        return "<blockquote>$inner</blockquote>"
    }

    private fun renderHeading(line: String): String =
        when {
            line.startsWith("### ") -> "<h3>${inlineMarkdown(escapeHtml(line.removePrefix("### ")))}</h3>"
            line.startsWith("## ") -> "<h2>${inlineMarkdown(escapeHtml(line.removePrefix("## ")))}</h2>"
            else -> "<h1>${inlineMarkdown(escapeHtml(line.removePrefix("# ")))}</h1>"
        }

    private fun renderParagraph(lines: List<String>): String {
        val inner = lines.joinToString("<br/>") { line ->
            inlineMarkdown(escapeHtml(line))
        }
        return "<p>$inner</p>"
    }

    private fun isBlockquoteLine(line: String): Boolean = line.startsWith(">")

    private fun isHeadingLine(line: String): Boolean =
        line.startsWith("### ") || line.startsWith("## ") || line.startsWith("# ")

    private fun isPlainParagraphLine(line: String): Boolean =
        line.isNotBlank() &&
            !isHeadingLine(line) &&
            !isListLine(line) &&
            !isBlockquoteLine(line)

    private fun isListLine(line: String): Boolean = parseMarkdownListItem(line) != null

    private fun blockquoteContent(line: String): String = line.removePrefix(">").trimStart()

    private data class MarkdownListItem(
        val indent: Int,
        val ordered: Boolean,
        val text: String,
    )

    private val BULLET_MARKER = Regex("^[-*]\\s+(.*)$")
    private val ORDERED_MARKER = Regex("^\\d+\\.\\s+(.*)$")
    private val BOLD = Regex("\\*\\*(.+?)\\*\\*")
    private val ITALIC = Regex("(?<!\\w)_(.+?)_(?!\\w)")
    private val LINK = Regex("\\[([^\\]]+)]\\(([^)]+)\\)")
    private val ATTRIBUTE = Regex("""([^\s=]+)\s*=\s*("([^"]*)"|'([^']*)'|([^\s>]+))""")

    private val SUPPORTED_TAGS = setOf(
        "h1", "h2", "h3", "p", "ul", "ol", "li", "blockquote",
        "strong", "b", "em", "i", "a", "br",
    )
    /** Pure layout/wrapper tags whose children flatten losslessly into this markdown subset. */
    private val TRANSPARENT_TAGS = setOf(
        "div", "span", "section", "article", "main", "header", "footer", "aside", "nav",
        "body", "html",
    )
    private val BLOCK_TAGS = setOf("h1", "h2", "h3", "p", "ul", "ol", "li", "blockquote")
    private val VOID_TAGS = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta",
        "param", "source", "track", "wbr",
    )

    private fun isStructurallyLossyTag(name: String): Boolean =
        name !in SUPPORTED_TAGS && name !in TRANSPARENT_TAGS

    private fun parseMarkdownListItem(line: String): MarkdownListItem? {
        val indent = line.indexOfFirst { !it.isWhitespace() }
        if (indent < 0) return null
        val rest = line.substring(indent)
        BULLET_MARKER.matchEntire(rest)?.let { match ->
            return MarkdownListItem(indent = indent, ordered = false, text = match.groupValues[1])
        }
        ORDERED_MARKER.matchEntire(rest)?.let { match ->
            return MarkdownListItem(indent = indent, ordered = true, text = match.groupValues[1])
        }
        return null
    }

    private fun renderMarkdownList(lines: List<String>): String {
        val items = lines.mapNotNull { parseMarkdownListItem(it) }
        if (items.isEmpty()) return ""
        val html = StringBuilder()
        var index = 0
        while (index < items.size) {
            val (listHtml, next) = buildMarkdownList(items, index)
            html.append(listHtml)
            index = next
        }
        return html.toString()
    }

    private fun buildMarkdownList(
        items: List<MarkdownListItem>,
        start: Int,
    ): Pair<String, Int> {
        if (start >= items.size) return "" to start
        val indent = items[start].indent
        val ordered = items[start].ordered
        val listItems = StringBuilder()
        var index = start
        while (index < items.size && items[index].indent >= indent) {
            if (items[index].indent > indent) break
            if (items[index].ordered != ordered) break
            val text = items[index].text
            index++
            val nested = StringBuilder()
            while (index < items.size && items[index].indent > indent) {
                val (html, next) = buildMarkdownList(items, index)
                nested.append(html)
                index = next
            }
            listItems.append("<li>${inlineMarkdown(escapeHtml(text))}$nested</li>")
        }
        val tag = if (ordered) "ol" else "ul"
        return "<$tag>$listItems</$tag>" to index
    }

    private fun inlineMarkdown(text: String): String =
        text
            .replace(BOLD) { "<strong>${it.groupValues[1]}</strong>" }
            .replace(ITALIC) { "<em>${it.groupValues[1]}</em>" }
            .replace(LINK) { match ->
                val label = match.groupValues[1]
                val href = match.groupValues[2]
                if (isAllowedHref(href)) {
                    """<a href="$href">${label}</a>"""
                } else {
                    label
                }
            }

    private fun isAllowedHref(href: String): Boolean {
        val trimmed = href.trim()
        if (trimmed.isEmpty()) return false
        if (!hasSchemeDelimiter(trimmed)) return true
        val scheme = hrefScheme(trimmed) ?: return false
        return scheme == "http" || scheme == "https" || scheme == "mailto"
    }

    private fun hasSchemeDelimiter(href: String): Boolean {
        val colon = href.indexOf(':')
        if (colon < 0) return false
        val relativeStart = href.indexOfFirst { it == '/' || it == '?' || it == '#' }
        return relativeStart < 0 || colon < relativeStart
    }

    private fun hrefScheme(href: String): String? {
        val compact = buildString {
            href.forEach { ch ->
                if (!ch.isWhitespace()) append(ch.lowercaseChar())
            }
        }
        val colon = compact.indexOf(':')
        if (colon <= 0) return null
        val scheme = compact.substring(0, colon)
        if (scheme.first().isLetter().not()) return null
        if (scheme.any { !it.isLetterOrDigit() && it != '+' && it != '.' && it != '-' }) return null
        return scheme
    }

    private sealed class HtmlNode {
        data class Text(val value: String) : HtmlNode()
        data class Element(
            val name: String,
            val attributes: Map<String, String>,
            val children: MutableList<HtmlNode> = mutableListOf(),
        ) : HtmlNode()
    }

    private fun parseFragment(html: String): Pair<List<HtmlNode>, Boolean> {
        val root = HtmlNode.Element(name = "#fragment", attributes = emptyMap())
        val stack = ArrayDeque<HtmlNode.Element>().apply { addLast(root) }
        var index = 0
        var lossy = false

        fun current(): HtmlNode.Element = stack.last()

        while (index < html.length) {
            if (html[index] != '<') {
                val next = html.indexOf('<', index).let { if (it < 0) html.length else it }
                current().children += HtmlNode.Text(html.substring(index, next))
                index = next
                continue
            }
            if (html.startsWith("<!--", index)) {
                val end = html.indexOf("-->", index + 4)
                index = if (end < 0) html.length else end + 3
                continue
            }
            val close = html.indexOf('>', index + 1)
            if (close < 0) {
                current().children += HtmlNode.Text(html.substring(index))
                break
            }
            val raw = html.substring(index + 1, close).trim()
            index = close + 1
            if (raw.startsWith("!") || raw.startsWith("?")) continue
            if (raw.startsWith("/")) {
                val name = raw.drop(1).trim().substringBefore(' ').lowercase()
                val matchIndex = stack.indexOfLast { it.name == name }
                if (matchIndex <= 0) continue
                while (stack.lastIndex >= matchIndex && stack.size > 1) {
                    stack.removeLast()
                }
                continue
            }
            val selfClosing = raw.endsWith("/")
            val body = raw.removeSuffix("/").trim()
            val nameEnd = body.indexOfFirst { it.isWhitespace() }
            val name = (if (nameEnd < 0) body else body.substring(0, nameEnd)).lowercase()
            if (name.isEmpty()) continue
            if (isStructurallyLossyTag(name)) lossy = true
            val element = HtmlNode.Element(
                name = name,
                attributes = parseAttributes(body),
            )
            current().children += element
            val void = selfClosing || name in VOID_TAGS
            if (!void) stack.addLast(element)
        }
        return root.children to lossy
    }

    private fun parseAttributes(tagBody: String): Map<String, String> {
        val nameEnd = tagBody.indexOfFirst { it.isWhitespace() }
        if (nameEnd < 0) return emptyMap()
        val rest = tagBody.substring(nameEnd).trimStart()
        if (rest.isBlank()) return emptyMap()
        return ATTRIBUTE.findAll(rest).associate { match ->
            val value = match.groupValues[3]
                .ifEmpty { match.groupValues[4] }
                .ifEmpty { match.groupValues[5] }
            match.groupValues[1].lowercase() to value
        }
    }

    private class MarkdownConverter {
        var isLossy: Boolean = false
            private set

        fun convert(nodes: List<HtmlNode>): String {
            val hasBlock = nodes.any(::containsBlockElement)
            val markdown = if (hasBlock) blocks(nodes) else inlines(nodes)
            return markdown.trim()
        }

        private fun containsBlockElement(node: HtmlNode): Boolean =
            node is HtmlNode.Element && (
                node.name in BLOCK_TAGS || node.children.any(::containsBlockElement)
            )

        private fun blocks(nodes: List<HtmlNode>): String {
            val parts = mutableListOf<String>()
            for (node in nodes) {
                when (node) {
                    is HtmlNode.Text -> {
                        val text = unescape(node.value).trim()
                        if (text.isNotEmpty()) parts += text
                    }
                    is HtmlNode.Element -> when (node.name) {
                        "h1" -> parts += "# ${inlines(node.children)}".trimEnd()
                        "h2" -> parts += "## ${inlines(node.children)}".trimEnd()
                        "h3" -> parts += "### ${inlines(node.children)}".trimEnd()
                        "p" -> parts += inlines(node.children)
                        "ul" -> parts += renderList(node, ordered = false, depth = 0)
                        "ol" -> parts += renderList(node, ordered = true, depth = 0)
                        "blockquote" -> parts += renderQuote(node)
                        "br" -> Unit
                        else -> {
                            if (isStructurallyLossyTag(node.name)) isLossy = true
                            val inner = blocks(node.children).ifBlank { inlines(node.children) }
                            if (inner.isNotBlank()) parts += inner
                        }
                    }
                }
            }
            return parts.filter { it.isNotBlank() }.joinToString("\n\n")
        }

        private fun renderQuote(element: HtmlNode.Element): String {
            val hasBlockChild = element.children.any { child ->
                child is HtmlNode.Element && child.name !in setOf("strong", "b", "em", "i", "a", "br")
            }
            val inner = if (hasBlockChild) {
                blocks(element.children).ifBlank { inlines(element.children) }
            } else {
                inlines(element.children)
            }
            if (inner.isBlank()) return ""
            return inner.lineSequence().joinToString("\n") { line ->
                if (line.isBlank()) ">" else "> $line"
            }
        }

        private fun renderList(
            list: HtmlNode.Element,
            ordered: Boolean,
            depth: Int,
        ): String {
            val indent = "  ".repeat(depth)
            val items = list.children.mapNotNull { child ->
                child as? HtmlNode.Element
            }.filter { it.name == "li" }
            return items.mapIndexed { index, item ->
                val marker = if (ordered) "${index + 1}. " else "- "
                val nestedLists = mutableListOf<HtmlNode.Element>()
                val inlineNodes = mutableListOf<HtmlNode>()
                for (child in item.children) {
                    if (child is HtmlNode.Element && (child.name == "ul" || child.name == "ol")) {
                        nestedLists += child
                    } else {
                        inlineNodes += child
                    }
                }
                val title = inlines(inlineNodes)
                val head = "$indent$marker$title".trimEnd()
                val nested = nestedLists.joinToString("\n") { nested ->
                    renderList(nested, ordered = nested.name == "ol", depth = depth + 1)
                }
                if (nested.isBlank()) head else "$head\n$nested"
            }.filter { it.isNotBlank() }.joinToString("\n")
        }

        private fun inlines(nodes: List<HtmlNode>): String {
            val builder = StringBuilder()
            for (node in nodes) {
                when (node) {
                    is HtmlNode.Text -> builder.append(unescape(node.value))
                    is HtmlNode.Element -> when (node.name) {
                        "strong", "b" -> builder.append("**${inlines(node.children)}**")
                        "em", "i" -> builder.append("_${inlines(node.children)}_")
                        "a" -> {
                            val href = unescape(node.attributes["href"].orEmpty())
                            val label = inlines(node.children)
                            builder.append("[$label]($href)")
                        }
                        "br" -> builder.append('\n')
                        "p" -> {
                            if (builder.isNotEmpty()) builder.append("\n\n")
                            builder.append(inlines(node.children))
                        }
                        "ul", "ol" -> {
                            if (builder.isNotEmpty()) builder.append('\n')
                            builder.append(renderList(node, ordered = node.name == "ol", depth = 0))
                        }
                        else -> {
                            if (isStructurallyLossyTag(node.name)) isLossy = true
                            builder.append(inlines(node.children))
                        }
                    }
                }
            }
            return builder.toString().trim()
        }

        private fun unescape(value: String): String = unescapeHtml(value)
    }

    /**
     * Escapes raw markup characters. Well-formed entity references are passed through only when
     * [decodeEntity] would leave them unchanged on read; decodable entities are escaped so
     * markdown → HTML → markdown stays symmetric (e.g. literal `&amp;` is not decoded to `&`).
     */
    private fun escapeHtml(value: String): String {
        val result = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            when (val ch = value[index]) {
                '&' -> {
                    val match = ENTITY.matchAt(value, index)
                    if (match != null && decodeEntity(match.groupValues[1]) == null) {
                        result.append(match.value)
                        index = match.range.last + 1
                    } else {
                        result.append("&amp;")
                        index++
                    }
                }
                '<' -> {
                    result.append("&lt;")
                    index++
                }
                '>' -> {
                    result.append("&gt;")
                    index++
                }
                '"' -> {
                    result.append("&quot;")
                    index++
                }
                else -> {
                    result.append(ch)
                    index++
                }
            }
        }
        return result.toString()
    }

    private val ENTITY = Regex("""&(#x[0-9a-fA-F]+|#\d+|[a-zA-Z][a-zA-Z0-9]*);""")

    private val NAMED_ENTITIES = mapOf(
        // XML / HTML essentials (+ HTML5 legacy uppercase spellings)
        "amp" to "&",
        "AMP" to "&",
        "lt" to "<",
        "LT" to "<",
        "gt" to ">",
        "GT" to ">",
        "quot" to "\"",
        "QUOT" to "\"",
        "apos" to "'",
        "APOS" to "'",
        "nbsp" to "\u00A0",
        "NBSP" to "\u00A0",
        // Typography rich-text editors commonly emit
        "lsquo" to "\u2018",
        "rsquo" to "\u2019",
        "ldquo" to "\u201C",
        "rdquo" to "\u201D",
        "mdash" to "\u2014",
        "ndash" to "\u2013",
        "hellip" to "\u2026",
        "copy" to "\u00A9",
        "COPY" to "\u00A9",
        "reg" to "\u00AE",
        "trade" to "\u2122",
        // Latin-1 letter entities (ISO-8859-1)
        "Agrave" to "\u00C0", "Aacute" to "\u00C1", "Acirc" to "\u00C2", "Atilde" to "\u00C3",
        "Auml" to "\u00C4", "Aring" to "\u00C5", "AElig" to "\u00C6", "Ccedil" to "\u00C7",
        "Egrave" to "\u00C8", "Eacute" to "\u00C9", "Ecirc" to "\u00CA", "Euml" to "\u00CB",
        "Igrave" to "\u00CC", "Iacute" to "\u00CD", "Icirc" to "\u00CE", "Iuml" to "\u00CF",
        "ETH" to "\u00D0", "Ntilde" to "\u00D1", "Ograve" to "\u00D2", "Oacute" to "\u00D3",
        "Ocirc" to "\u00D4", "Otilde" to "\u00D5", "Ouml" to "\u00D6", "Oslash" to "\u00D8",
        "Ugrave" to "\u00D9", "Uacute" to "\u00DA", "Ucirc" to "\u00DB", "Uuml" to "\u00DC",
        "Yacute" to "\u00DD", "THORN" to "\u00DE", "szlig" to "\u00DF",
        "agrave" to "\u00E0", "aacute" to "\u00E1", "acirc" to "\u00E2", "atilde" to "\u00E3",
        "auml" to "\u00E4", "aring" to "\u00E5", "aelig" to "\u00E6", "ccedil" to "\u00E7",
        "egrave" to "\u00E8", "eacute" to "\u00E9", "ecirc" to "\u00EA", "euml" to "\u00EB",
        "igrave" to "\u00EC", "iacute" to "\u00ED", "icirc" to "\u00EE", "iuml" to "\u00EF",
        "eth" to "\u00F0", "ntilde" to "\u00F1", "ograve" to "\u00F2", "oacute" to "\u00F3",
        "ocirc" to "\u00F4", "otilde" to "\u00F5", "ouml" to "\u00F6", "oslash" to "\u00F8",
        "ugrave" to "\u00F9", "uacute" to "\u00FA", "ucirc" to "\u00FB", "uuml" to "\u00FC",
        "yacute" to "\u00FD", "thorn" to "\u00FE", "yuml" to "\u00FF",
    )

    private fun unescapeHtml(value: String): String =
        ENTITY.replace(value) { match ->
            decodeEntity(match.groupValues[1]) ?: match.value
        }

    private fun decodeEntity(body: String): String? = when {
        body.startsWith("#x", ignoreCase = true) ->
            body.substring(2).toIntOrNull(16)?.let(::codePointToString)
        body.startsWith("#") ->
            body.substring(1).toIntOrNull()?.let(::codePointToString)
        else -> NAMED_ENTITIES[body]
    }

    private fun codePointToString(code: Int): String? {
        if (code <= 0 || code > Character.MAX_CODE_POINT) return null
        if (code in Character.MIN_SURROGATE.code..Character.MAX_SURROGATE.code) return null
        return String(Character.toChars(code))
    }
}
