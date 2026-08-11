package com.nus.folio.presentation.home.notebook

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

    fun markdownToHtml(markdown: String): String {
        if (markdown.isBlank()) return "<p></p>"
        val blocks = markdown.replace("\r\n", "\n").split("\n\n")
        return blocks.joinToString("\n") { block -> renderBlock(block) }
    }

    private fun renderBlock(block: String): String {
        val lines = block.split('\n')
        if (lines.isEmpty()) return ""

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
                line.matches(BULLET_ITEM) || (line.isBlank() && index + 1 < lines.size && lines[index + 1].matches(BULLET_ITEM)) -> {
                    val listLines = mutableListOf<String>()
                    while (index < lines.size && (lines[index].matches(BULLET_ITEM) || lines[index].isBlank())) {
                        if (lines[index].isNotBlank()) listLines += lines[index]
                        index++
                    }
                    val items = listLines.joinToString("") { itemLine ->
                        val item = itemLine.replace(BULLET_ITEM, "")
                        "<li>${inlineMarkdown(escapeHtml(item))}</li>"
                    }
                    segments += "<ul>$items</ul>"
                }
                line.matches(ORDERED_ITEM) || (line.isBlank() && index + 1 < lines.size && lines[index + 1].matches(ORDERED_ITEM)) -> {
                    val listLines = mutableListOf<String>()
                    while (index < lines.size && (lines[index].matches(ORDERED_ITEM) || lines[index].isBlank())) {
                        if (lines[index].isNotBlank()) listLines += lines[index]
                        index++
                    }
                    val items = listLines.joinToString("") { itemLine ->
                        val item = itemLine.replace(ORDERED_ITEM, "")
                        "<li>${inlineMarkdown(escapeHtml(item))}</li>"
                    }
                    segments += "<ol>$items</ol>"
                }
                else -> {
                    segments += renderTextLine(line)
                    index++
                }
            }
        }
        return segments.joinToString("\n")
    }

    private fun renderBlockquote(lines: List<String>): String {
        val quote = lines.joinToString("\n") { blockquoteContent(it).trimEnd() }
        return "<blockquote>${inlineMarkdown(escapeHtml(quote).replace("\n", "<br/>"))}</blockquote>"
    }

    private fun renderTextLine(line: String): String =
        when {
            line.startsWith("### ") -> "<h3>${inlineMarkdown(escapeHtml(line.removePrefix("### ")))}</h3>"
            line.startsWith("## ") -> "<h2>${inlineMarkdown(escapeHtml(line.removePrefix("## ")))}</h2>"
            line.startsWith("# ") -> "<h1>${inlineMarkdown(escapeHtml(line.removePrefix("# ")))}</h1>"
            line.isBlank() -> ""
            else -> "<p>${inlineMarkdown(escapeHtml(line))}</p>"
        }

    private fun isBlockquoteLine(line: String): Boolean =
        line.startsWith(">")

    private fun blockquoteContent(line: String): String =
        line.removePrefix(">").trimStart()

    private val BULLET_ITEM = Regex("^[-*]\\s+")
    private val ORDERED_ITEM = Regex("^\\d+\\.\\s+")

    private fun inlineMarkdown(text: String): String =
        text
            .replace(Regex("\\*\\*(.+?)\\*\\*")) { "<strong>${it.groupValues[1]}</strong>" }
            .replace(Regex("_(.+?)_")) { "<em>${it.groupValues[1]}</em>" }

    private fun escapeHtml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
