package com.nus.folio.domain.model

/**
 * Converts [StructuredContent] payloads into HTML fragments for the detail WebView.
 */
object StructuredContentHtml {

    fun body(content: StructuredContent, selectedSheetIndex: Int = 0): String =
        when (content) {
            is StructuredContent.Document -> content.html
            is StructuredContent.Sheets -> {
                val sheet = content.sheets.getOrNull(selectedSheetIndex)
                    ?: content.sheets.firstOrNull()
                sheet?.let(::sheetToHtml).orEmpty()
            }
            is StructuredContent.Slides -> slidesToHtml(content.slides)
        }

    fun toSheetTabs(sheets: List<StructuredSheet>): List<SourceSheetTab> =
        sheets.mapIndexed { index, sheet ->
            SourceSheetTab(
                id = "structured-sheet-$index",
                name = sheet.name.ifBlank { "Sheet ${index + 1}" },
                htmlTable = sheetToHtml(sheet),
            )
        }

    fun sheetToHtml(sheet: StructuredSheet): String {
        if (sheet.headers.isEmpty() && sheet.rows.isEmpty()) return ""
        return buildString {
            append("<table>")
            if (sheet.headers.isNotEmpty()) {
                append("<thead><tr>")
                sheet.headers.forEach { header ->
                    append("<th>")
                    append(escapeHtml(header))
                    append("</th>")
                }
                append("</tr></thead>")
            }
            if (sheet.rows.isNotEmpty()) {
                append("<tbody>")
                sheet.rows.forEach { row ->
                    append("<tr>")
                    row.forEach { cell ->
                        append("<td>")
                        append(escapeHtml(cell))
                        append("</td>")
                    }
                    append("</tr>")
                }
                append("</tbody>")
            }
            append("</table>")
        }
    }

    fun slidesToHtml(slides: List<StructuredSlide>): String {
        if (slides.isEmpty()) return ""
        return buildString {
            slides.forEachIndexed { index, slide ->
                if (index > 0) append("""<hr class="slide-divider" />""")
                append("""<section class="slide">""")
                append("""<p class="byline">Slide ${slide.slideNumber}</p>""")
                append("""<h2 class="slide-heading">""")
                append(escapeHtml(slide.title.ifBlank { "Untitled Slide" }))
                append("</h2>")
                if (slide.bullets.isNotEmpty()) {
                    append("<ul>")
                    slide.bullets.forEach { bullet ->
                        append("<li>")
                        append(escapeHtml(bullet))
                        append("</li>")
                    }
                    append("</ul>")
                }
                append("</section>")
            }
        }
    }

    fun contentFormat(content: StructuredContent): SourceContentFormat =
        when (content) {
            is StructuredContent.Document -> SourceContentFormat.DOCUMENT
            is StructuredContent.Sheets -> SourceContentFormat.SHEET
            is StructuredContent.Slides -> SourceContentFormat.SLIDES
        }

    private fun escapeHtml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
