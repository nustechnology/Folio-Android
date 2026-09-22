package com.nus.folio.presentation.sourcedetail

import android.content.Context
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nus.folio.BuildConfig
import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.util.SourceImageUrlRules
import java.io.ByteArrayInputStream

/**
 * Renders source HTML in a read-only WebView that wraps content height and
 * only expands to [maxHeight] (with internal scroll) when the document is taller.
 */
@Composable
internal fun SourceHtmlRenderer(
    htmlBody: String,
    contentFormat: SourceContentFormat,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
    highlightText: String? = null,
) {
    val passage = highlightText?.trim().orEmpty()
    val fullHtml = remember(htmlBody, contentFormat, passage) {
        // Keep document base as file:///android_res/ for bundled fonts; rewrite
        // relative img src to the Folio API origin so PDF/doc images can load.
        val safeBody = SourceImageUrlRules.prepareSources(
            htmlBody,
            BuildConfig.FOLIO_API_BASE_URL,
        )
        val highlightedBody = CitationHighlight.apply(safeBody, passage)
        val wrappedBody = if (contentFormat == SourceContentFormat.SHEET) {
            """<div class="table-scroll">$highlightedBody</div>"""
        } else {
            highlightedBody
        }
        SourceHtmlTemplate.wrap(wrappedBody, contentFormat)
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .wrapContentHeight(),
        factory = { context ->
            ReadOnlyWebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = false
                settings.loadsImagesAutomatically = true
                // Network images allowed for http(s); other schemes gated by [SourceHtmlWebViewClient].
                settings.blockNetworkImage = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = contentFormat == SourceContentFormat.SHEET
                isFocusable = false
                isFocusableInTouchMode = false
                isLongClickable = false
                setOnLongClickListener { true }
                setBackgroundColor(0x00000000)
                webViewClient = SourceHtmlWebViewClient()
            }
        },
        update = { webView ->
            if (webView.tag != fullHtml) {
                webView.tag = fullHtml
                webView.loadDataWithBaseURL(
                    "file:///android_res/",
                    fullHtml,
                    "text/html",
                    "UTF-8",
                    null,
                )
            }
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
 * Allows normal document subresources (http(s), file, data, about) and blocks
 * other schemes that should never load from source HTML.
 */
private class SourceHtmlWebViewClient : WebViewClient() {
    private val initialDelaysMs = longArrayOf(0L, 100L, 300L, 800L)
    private var lastContentHeight = 0
    private var heightCheckRunnable: Runnable? = null

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (view != null) scheduleContentRelayout(view)
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val scheme = request.url.scheme?.lowercase().orEmpty()
        return when (scheme) {
            "file", "data", "about", "http", "https" ->
                super.shouldInterceptRequest(view, request)
            else -> blockedResponse()
        }
    }

    private fun scheduleContentRelayout(view: WebView) {
        initialDelaysMs.forEach { delayMs ->
            view.postDelayed(
                {
                    view.requestLayout()
                    view.invalidate()
                },
                delayMs,
            )
        }
        // Poll for content height changes to catch late-loading resources (e.g., images).
        lastContentHeight = 0
        heightCheckRunnable?.let { view.removeCallbacks(it) }
        heightCheckRunnable = object : Runnable {
            private var attempts = 0
            private var unchangedCount = 0
            override fun run() {
                val currentHeight = view.contentHeight
                if (currentHeight == lastContentHeight && currentHeight > 0) {
                    unchangedCount++
                    if (unchangedCount >= 2) return // Height stable, stop polling
                } else {
                    unchangedCount = 0
                }
                if (currentHeight != lastContentHeight && currentHeight > 0) {
                    lastContentHeight = currentHeight
                    view.requestLayout()
                    view.invalidate()
                }
                if (attempts++ < 10) view.postDelayed(this, 500)
            }
        }
        view.postDelayed(heightCheckRunnable!!, 1000)
    }

    private fun blockedResponse(): WebResourceResponse =
        WebResourceResponse(
            "text/plain",
            "utf-8",
            403,
            "Blocked",
            emptyMap(),
            ByteArrayInputStream(ByteArray(0)),
        )
}

/**
 * WebView that wraps document height for Compose layout, and never registers
 * as a text editor so the soft keyboard and IME cannot modify rendered source content.
 */
private class ReadOnlyWebView(context: Context) : WebView(context) {
    override fun onCheckIsTextEditor(): Boolean = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        // Measure to intrinsic HTML content height first.
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )
        var contentPx = measuredHeight
        if (contentPx <= 0) {
            val cssHeight = contentHeight
            if (cssHeight > 0) {
                contentPx = (cssHeight * resources.displayMetrics.density).toInt()
                setMeasuredDimension(measuredWidth, contentPx)
            }
        }

        when (heightMode) {
            MeasureSpec.EXACTLY -> {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }
            MeasureSpec.AT_MOST -> {
                if (contentPx > heightSize) {
                    // Content overflows — take the full available height and scroll.
                    super.onMeasure(
                        widthMeasureSpec,
                        MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY),
                    )
                }
                // else keep content-wrapped height from the UNSPECIFIED pass
            }
            MeasureSpec.UNSPECIFIED -> Unit
        }
    }
}
