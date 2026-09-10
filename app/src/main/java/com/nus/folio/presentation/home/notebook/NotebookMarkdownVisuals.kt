package com.nus.folio.presentation.home.notebook

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeTextSecondary
import com.nus.folio.ui.theme.LoginCopper

/**
 * Renders notebook markdown in the editor box: markers are hidden (not faded)
 * and the remaining text carries the format from the toolbar.
 *
 * Keep one instance per editor (via [remember]) so last-hit caching stays
 * scoped to that field and cannot leak across split panes / previews.
 */
internal class NotebookMarkdownVisualTransformation : VisualTransformation {
    // Same markdown is visualized multiple times per keystroke (transformation,
    // marker-aware delete, cursor scroll). Cache the last snapshot to avoid
    // redundant full scans on long documents.
    private var cachedSource: String? = null
    private var cachedVisualized: VisualizedMarkdown? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val visualized = visualizeCached(text.text)
        return TransformedText(visualized.text, visualized.mapping)
    }

    fun visualizeCached(markdown: String): VisualizedMarkdown {
        cachedVisualized?.let { cached ->
            if (cachedSource == markdown) return cached
        }
        return NotebookMarkdownVisuals.visualize(markdown).also {
            cachedSource = markdown
            cachedVisualized = it
        }
    }
}

internal data class VisualizedMarkdown(
    val text: AnnotatedString,
    val mapping: OffsetMapping,
)

internal object NotebookMarkdownVisuals {
    private val BoldStyle = SpanStyle(fontWeight = FontWeight.Bold)
    private val ItalicStyle = SpanStyle(fontStyle = FontStyle.Italic)
    private val LinkStyle = SpanStyle(
        color = LoginCopper,
        textDecoration = TextDecoration.Underline,
    )
    private val Heading1Style = SpanStyle(
        fontFamily = CormorantGaramond,
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        color = HomeHeader,
    )
    private val Heading2Style = SpanStyle(
        fontFamily = CormorantGaramond,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        color = HomeHeader,
    )
    private val Heading3Style = SpanStyle(
        fontFamily = CormorantGaramond,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = HomeHeader,
    )
    private val QuoteStyle = SpanStyle(
        fontStyle = FontStyle.Italic,
        color = HomeTextSecondary,
    )

    private val LinkPattern = Regex("\\[([^\\]]+)]\\(([^)]+)\\)")
    private val OrderedPrefix = Regex("^\\d+\\.\\s+")

    /** Pure markdown → visual mapping; safe to call from any editor instance. */
    fun visualize(markdown: String): VisualizedMarkdown {
        val builder = VisualBuilder(markdown)
        builder.parse()
        return builder.build()
    }

    private class VisualBuilder(private val original: String) {
        private val out = AnnotatedString.Builder()
        private val origToTrans = IntArray(original.length + 1)
        private val transToOrigChars = mutableListOf<Int>()
        private var o = 0

        fun parse() {
            while (o < original.length) {
                val newline = original.indexOf('\n', o)
                val lineEnd = if (newline == -1) original.length else newline
                parseLine(lineEnd)
                if (newline != -1) {
                    showUntil(newline + 1, style = null)
                }
            }
        }

        fun build(): VisualizedMarkdown {
            hideUntil(original.length)
            origToTrans[original.length] = out.length
            val transformedLength = out.length
            val originalLength = original.length
            check(transToOrigChars.size == transformedLength) {
                "Each shown character must have a transformed→original entry"
            }
            // Caret offsets 0..length-1 map to the original index of each shown
            // character. The end caret (length) must sit immediately after the last
            // shown character — before trailing hidden closers (** / _) — so
            // toolbar toggles and marker-aware delete do not land inside markers.
            val transToOrig = IntArray(transformedLength + 1) { index ->
                if (index < transformedLength) {
                    transToOrigChars[index]
                } else if (transToOrigChars.isEmpty()) {
                    0
                } else {
                    (transToOrigChars.last() + 1).coerceIn(0, originalLength)
                }
            }
            return VisualizedMarkdown(
                text = out.toAnnotatedString(),
                mapping = object : OffsetMapping {
                    override fun originalToTransformed(offset: Int): Int =
                        origToTrans[offset.coerceIn(0, origToTrans.lastIndex)]
                            .coerceIn(0, transformedLength)

                    override fun transformedToOriginal(offset: Int): Int =
                        transToOrig[offset.coerceIn(0, transToOrig.lastIndex)]
                            .coerceIn(0, originalLength)
                },
            )
        }

        private fun parseLine(lineEnd: Int) {
            val line = original.substring(o, lineEnd)
            when {
                line.startsWith("### ") -> {
                    hideUntil(o + 4)
                    parseInline(lineEnd, Heading3Style)
                }
                line.startsWith("## ") -> {
                    hideUntil(o + 3)
                    parseInline(lineEnd, Heading2Style)
                }
                line.startsWith("# ") -> {
                    hideUntil(o + 2)
                    parseInline(lineEnd, Heading1Style)
                }
                line.startsWith("> ") -> {
                    showUntil(o + 2, style = QuoteStyle)
                    parseInline(lineEnd, QuoteStyle)
                }
                line.startsWith(">") -> {
                    showUntil(o + 1, style = QuoteStyle)
                    if (o < lineEnd && original[o] == ' ') showUntil(o + 1, style = QuoteStyle)
                    parseInline(lineEnd, QuoteStyle)
                }
                line.startsWith("- ") -> {
                    showUntil(o + 2, style = null)
                    parseInline(lineEnd, style = null)
                }
                else -> {
                    val ordered = OrderedPrefix.find(line)
                    if (ordered != null) {
                        showUntil(o + ordered.value.length, style = null)
                        parseInline(lineEnd, style = null)
                    } else {
                        parseInline(lineEnd, style = null)
                    }
                }
            }
        }

        private fun parseInline(limit: Int, style: SpanStyle?) {
            while (o < limit) {
                val markup = nextMarkup(o, limit)
                if (markup == null) {
                    showUntil(limit, style)
                    return
                }
                if (markup.start > o) showUntil(markup.start, style)
                consumeMarkup(markup, style)
            }
        }

        private fun consumeMarkup(markup: Markup, base: SpanStyle?) {
            when (markup) {
                is Markup.Bold -> {
                    hideUntil(markup.start + 2)
                    parseInline(markup.close, base.merge(BoldStyle))
                    hideUntil(markup.close + 2)
                }
                is Markup.Italic -> {
                    hideUntil(markup.start + 1)
                    parseInline(markup.close, base.merge(ItalicStyle))
                    hideUntil(markup.close + 1)
                }
                is Markup.Link -> {
                    hideUntil(markup.start + 1)
                    showUntil(markup.labelEnd, base.merge(LinkStyle))
                    hideUntil(markup.end)
                }
            }
        }

        private fun nextMarkup(from: Int, limit: Int): Markup? {
            var i = from
            while (i < limit) {
                when {
                    original.startsWith("**", i) -> {
                        val close = original.indexOf("**", i + 2)
                        if (close in (i + 2) until limit) return Markup.Bold(i, close)
                        i += 2
                    }
                    original[i] == '_' && isItalicOpenMarker(original, i, limit) -> {
                        val close = findItalicCloseMarker(original, i, limit)
                        if (close != null) return Markup.Italic(i, close)
                        i++
                    }
                    original[i] == '[' -> {
                        val link = matchLink(i, limit)
                        if (link != null) return link
                        i++
                    }
                    else -> i++
                }
            }
            return null
        }

        private fun matchLink(index: Int, limit: Int): Markup.Link? {
            val match = LinkPattern.find(original, index) ?: return null
            if (match.range.first != index) return null
            if (match.range.last + 1 > limit) return null
            val label = match.groups[1] ?: return null
            return Markup.Link(
                start = index,
                labelEnd = index + 1 + label.value.length,
                end = match.range.last + 1,
            )
        }

        private fun hideUntil(end: Int) {
            val t = out.length
            val bound = end.coerceAtMost(original.length)
            while (o < bound) {
                origToTrans[o] = t
                o++
            }
        }

        private fun showUntil(end: Int, style: SpanStyle?) {
            val bound = end.coerceAtMost(original.length)
            if (bound <= o) return
            val startT = out.length
            val startO = o
            out.append(original.substring(o, bound))
            if (style != null) out.addStyle(style, startT, out.length)
            while (o < bound) {
                origToTrans[o] = startT + (o - startO)
                transToOrigChars.add(o)
                o++
            }
        }
    }

    private sealed class Markup {
        abstract val start: Int

        data class Bold(override val start: Int, val close: Int) : Markup()
        data class Italic(override val start: Int, val close: Int) : Markup()
        data class Link(override val start: Int, val labelEnd: Int, val end: Int) : Markup()
    }

    private fun SpanStyle?.merge(other: SpanStyle): SpanStyle =
        this?.merge(other) ?: other
}

/** Letter, digit, or `_` — same word-char rule CommonMark uses for emphasis. */
internal fun Char.isMdWord(): Boolean = isLetterOrDigit() || this == '_'

/**
 * Underscore italic opener: not after a word char, and a valid closer exists
 * before [limit] (typically end of line).
 */
internal fun isItalicOpenMarker(text: String, index: Int, limit: Int): Boolean {
    if (index >= limit || text[index] != '_') return false
    if (index > 0 && text[index - 1].isMdWord()) return false
    return findItalicCloseMarker(text, index, limit) != null
}

/** First `_` after [open] that is not followed by a word char. */
internal fun findItalicCloseMarker(text: String, open: Int, limit: Int): Int? {
    var i = open + 1
    while (i < limit) {
        if (text[i] == '_') {
            val next = i + 1
            val nextIsWord = next < limit && text[next].isMdWord()
            if (!nextIsWord) return i
        }
        i++
    }
    return null
}
