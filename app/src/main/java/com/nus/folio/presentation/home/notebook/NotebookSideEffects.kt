package com.nus.folio.presentation.home.notebook

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.OutputStream

internal object NotebookClipboardHelper {
    fun copy(context: Context, label: String, content: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, content))
    }
}

internal object NotebookExportHelper {
    fun writeMarkdown(output: OutputStream, markdown: String) {
        output.write(markdown.toByteArray(Charsets.UTF_8))
    }

    fun writeMarkdown(context: Context, uri: Uri, markdown: String): Result<Unit> = runCatching {
        val stream = context.contentResolver.openOutputStream(uri)
            ?: error("openOutputStream returned null")
        stream.use { writeMarkdown(it, markdown) }
    }
}

/**
 * Starts an off-screen WebView print job. [PrintManager.print] runs only from
 * [WebViewClient.onPageFinished]; cancel before that to avoid submitting after the host is gone.
 */
internal object NotebookPrintHelper {
    fun print(
        context: Context,
        jobName: String,
        markdown: String,
        loadId: Long,
        onSubmitted: () -> Unit = {},
    ): NotebookPrintSession {
        var finished = false
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (finished) return
                finished = true
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val adapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, adapter, null)
                onSubmitted()
            }
        }
        val html = NotebookPrintTemplate.wrap(
            title = jobName,
            bodyHtml = NotebookPrintTemplate.markdownToHtml(markdown),
        )
        webView.loadDataWithBaseURL(
            "https://folio.local/print/$loadId",
            html,
            "text/html",
            "UTF-8",
            null,
        )
        return NotebookPrintSession {
            if (finished) return@NotebookPrintSession
            finished = true
            webView.stopLoading()
            webView.destroy()
        }
    }
}

internal fun interface NotebookPrintSession {
    fun cancel()
}
