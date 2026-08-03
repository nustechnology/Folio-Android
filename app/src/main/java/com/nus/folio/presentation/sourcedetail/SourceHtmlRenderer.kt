package com.nus.folio.presentation.sourcedetail

import android.content.Context
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
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
) {
    val fullHtml = remember(htmlBody, contentFormat) {
        val wrappedBody = if (contentFormat == SourceContentFormat.SHEET) {
            """<div class="table-scroll">$htmlBody</div>"""
        } else {
            htmlBody
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
                webViewClient = WebViewClient()
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

/**
 * WebView that never registers as a text editor, so the soft keyboard and IME
 * cannot insert, delete, or modify the rendered source content.
 */
private class ReadOnlyWebView(context: Context) : WebView(context) {
    override fun onCheckIsTextEditor(): Boolean = false
}
