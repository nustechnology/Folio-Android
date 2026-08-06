package com.nus.folio.presentation.sourcedetail

import android.content.Context
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.nus.folio.domain.model.SourceContentFormat

@Composable
internal fun SourceHtmlRenderer(
    htmlBody: String,
    contentFormat: SourceContentFormat,
    modifier: Modifier = Modifier,
    highlightText: String? = null,
) {
    val passage = highlightText?.trim().orEmpty()
    val fullHtml = remember(htmlBody, contentFormat, passage) {
        val highlightedBody = CitationHighlight.apply(htmlBody, passage)
        val wrappedBody = if (contentFormat == SourceContentFormat.SHEET) {
            """<div class="table-scroll">$highlightedBody</div>"""
        } else {
            highlightedBody
        }
        SourceHtmlTemplate.wrap(wrappedBody, contentFormat)
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            ReadOnlyWebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = contentFormat == SourceContentFormat.SHEET
                isFocusable = false
                isFocusableInTouchMode = false
                isLongClickable = false
                setOnLongClickListener { true }
                setBackgroundColor(0x00000000)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                "file:///android_res/",
                fullHtml,
                "text/html",
                "UTF-8",
                null,
            )
        },
    )
}

internal object CitationHighlight {
    private const val MARK_OPEN =
        """<mark id="folio-citation-highlight" class="folio-citation-highlight" tabindex="-1" autofocus>"""
    private const val MARK_CLOSE = "</mark>"

    fun apply(html: String, passage: String): String {
        if (passage.isBlank() || html.isBlank()) return html
        val match = findInTextContent(html, passage) ?: return html
        return html.replaceRange(
            match.range,
            "$MARK_OPEN${match.value}$MARK_CLOSE",
        )
    }

    /** First case-insensitive [passage] match that lies entirely outside HTML tags. */
    private fun findInTextContent(html: String, passage: String): TextMatch? {
        val pattern = Regex(Regex.escape(passage), RegexOption.IGNORE_CASE)
        for (range in textContentRanges(html)) {
            val text = html.substring(range)
            val match = pattern.find(text) ?: continue
            val absoluteFirst = range.first + match.range.first
            val absoluteLast = range.first + match.range.last
            return TextMatch(
                value = match.value,
                range = absoluteFirst..absoluteLast,
            )
        }
        return null
    }

    private fun textContentRanges(html: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var textStart = -1
        var inTag = false
        for (i in html.indices) {
            when (html[i]) {
                '<' -> {
                    if (!inTag && textStart >= 0) {
                        ranges += textStart until i
                        textStart = -1
                    }
                    inTag = true
                }
                '>' -> {
                    if (inTag) {
                        inTag = false
                        textStart = i + 1
                    }
                }
                else -> {
                    if (!inTag && textStart < 0) {
                        textStart = i
                    }
                }
            }
        }
        if (!inTag && textStart >= 0 && textStart < html.length) {
            ranges += textStart until html.length
        }
        return ranges
    }

    private data class TextMatch(val value: String, val range: IntRange)
}

/**
 * WebView that never registers as a text editor, so the soft keyboard and IME
 * cannot insert, delete, or modify the rendered source content.
 */
private class ReadOnlyWebView(context: Context) : WebView(context) {
    override fun onCheckIsTextEditor(): Boolean = false
}
