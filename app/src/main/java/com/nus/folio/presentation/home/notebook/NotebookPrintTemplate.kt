package com.nus.folio.presentation.home.notebook

import com.nus.folio.domain.util.NotebookHtml

internal object NotebookPrintTemplate {
    fun wrap(title: String, bodyHtml: String): String = """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8"/>
          <title>${escapeHtml(title)}</title>
          <style>
            @page { margin: 2cm; }
            body {
              font-family: Georgia, "Times New Roman", serif;
              color: #1a1a1a;
              line-height: 1.6;
              font-size: 12pt;
            }
            h1 { font-size: 24pt; margin: 0 0 12pt; }
            h2 { font-size: 18pt; margin: 18pt 0 8pt; }
            h3 { font-size: 14pt; margin: 14pt 0 6pt; }
            p { margin: 0 0 10pt; }
            ul, ol { margin: 0 0 10pt 20pt; padding: 0; }
            blockquote {
              margin: 0 0 10pt;
              padding-left: 12pt;
              border-left: 3px solid #c9b896;
              color: #5a5348;
            }
          </style>
        </head>
        <body>
          $bodyHtml
        </body>
        </html>
    """.trimIndent()

    fun markdownToHtml(markdown: String): String =
        NotebookHtml.markdownToHtml(markdown).ifBlank { "<p></p>" }

    private fun escapeHtml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
