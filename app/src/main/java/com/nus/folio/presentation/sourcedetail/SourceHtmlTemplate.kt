package com.nus.folio.presentation.sourcedetail

import com.nus.folio.domain.model.SourceContentFormat

internal object SourceHtmlTemplate {

    fun wrap(bodyHtml: String, contentFormat: SourceContentFormat): String {
        val extraStyles = when (contentFormat) {
            SourceContentFormat.DOCUMENT -> documentStyles
            SourceContentFormat.SLIDES -> slidesStyles
            SourceContentFormat.SHEET -> sheetStyles
        }
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <style>
                    $baseStyles
                    $extraStyles
                </style>
            </head>
            <body contenteditable="false">
                <article class="content" contenteditable="false">
                    $bodyHtml
                </article>
            </body>
            </html>
        """.trimIndent()
    }

    private val baseStyles = """
        @font-face {
            font-family: 'Cormorant Garamond';
            src: url('file:///android_res/font/cormorant_garamond_regular.ttf');
            font-weight: 400;
        }
        @font-face {
            font-family: 'Cormorant Garamond';
            src: url('file:///android_res/font/cormorant_garamond_semibold.ttf');
            font-weight: 600;
        }
        @font-face {
            font-family: 'Cormorant Garamond';
            src: url('file:///android_res/font/cormorant_garamond_bold.ttf');
            font-weight: 700;
        }
        * { box-sizing: border-box; }
        html, body, .content, .content * {
            -webkit-user-modify: read-only;
            user-select: text;
            -webkit-user-select: text;
        }
        html, body {
            margin: 0;
            padding: 0;
            background: transparent;
            color: #1A1A1A;
        }
        .content {
            padding: 4px 0 32px;
        }
        h1, h2, h3 {
            font-family: 'Cormorant Garamond', Georgia, serif;
            color: #0B2A24;
            font-weight: 700;
            margin: 0 0 12px;
        }
        h1 { font-size: 32px; line-height: 1.15; margin-bottom: 16px; }
        h2 { font-size: 24px; line-height: 1.2; margin-top: 28px; }
        h3 { font-size: 20px; line-height: 1.25; margin-top: 22px; }
        p, li {
            font-family: Georgia, 'Times New Roman', serif;
            font-size: 17px;
            line-height: 1.65;
            margin: 0 0 14px;
            color: #2E2A24;
        }
        .byline {
            font-size: 14px;
            color: #7A7164;
            margin-bottom: 24px;
        }
        ul, ol {
            margin: 0 0 16px;
            padding-left: 22px;
        }
        li { margin-bottom: 8px; }
        blockquote {
            margin: 20px 0;
            padding: 14px 18px;
            border-left: 3px solid #C4A35A;
            background: #F0E6D6;
            border-radius: 0 8px 8px 0;
            font-family: Georgia, 'Times New Roman', serif;
            font-size: 16px;
            line-height: 1.6;
            color: #3D3429;
        }
        mark.folio-citation-highlight,
        #folio-citation-highlight {
            background: #F6E7A1;
            color: inherit;
            padding: 0 2px;
            border-radius: 2px;
            scroll-margin-top: 35vh;
        }
        img {
            max-width: 100%;
            height: auto;
            display: block;
            margin: 12px 0 16px;
            border-radius: 8px;
        }
        figure {
            margin: 16px 0 20px;
            padding: 0;
        }
        figcaption, caption {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            font-size: 13px;
            line-height: 1.4;
            color: #7A7164;
            margin-top: 8px;
        }
        table {
            border-collapse: collapse;
            width: 100%;
            margin: 16px 0 20px;
            background: #FFFBF5;
        }
        th, td {
            border: 1px solid #E6DCCB;
            padding: 10px 14px;
            text-align: left;
            vertical-align: top;
            font-family: Georgia, 'Times New Roman', serif;
            font-size: 15px;
            line-height: 1.5;
            color: #1A1A1A;
            white-space: normal;
            word-break: break-word;
        }
        th {
            background: #F0E6D6;
            font-weight: 600;
            color: #0B2A24;
        }
        tr:nth-child(even) td {
            background: #FAF6EF;
        }
        pre {
            margin: 16px 0;
            padding: 14px 16px;
            overflow-x: auto;
            border-radius: 8px;
            background: #F0E6D6;
            border: 1px solid #E6DCCB;
            font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
            font-size: 13px;
            line-height: 1.5;
            color: #2E2A24;
        }
        code {
            font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
            font-size: 0.92em;
            background: #F0E6D6;
            padding: 1px 5px;
            border-radius: 4px;
        }
        pre code {
            background: transparent;
            padding: 0;
            border-radius: 0;
            font-size: inherit;
        }
        hr {
            border: none;
            border-top: 1px dashed #C9BBA3;
            margin: 24px 0;
        }
        sub, sup {
            font-size: 0.75em;
            line-height: 0;
        }
    """.trimIndent()

    private val documentStyles = """
        .pdf-page {
            margin: 0 0 32px;
            padding: 0 0 28px;
            border-bottom: 1px dashed #C9BBA3;
        }
        .pdf-page:last-child {
            margin-bottom: 0;
            padding-bottom: 0;
            border-bottom: none;
        }
        .pdf-page table {
            display: block;
            overflow-x: auto;
            -webkit-overflow-scrolling: touch;
            width: 100%;
        }
    """.trimIndent()

    private val slidesStyles = """
        .slide {
            margin: 0 0 20px;
            padding: 16px 18px;
            border: 1px solid #E6DCCB;
            border-radius: 12px;
            background: #FFFBF5;
        }
        .slide:last-child {
            margin-bottom: 0;
        }
        .slide-heading {
            font-size: 22px;
            margin-top: 0;
        }
        .slide-divider {
            display: none;
        }
    """.trimIndent()

    private val sheetStyles = """
        .table-scroll {
            overflow-x: auto;
            -webkit-overflow-scrolling: touch;
            margin: 0;
        }
        table {
            border-collapse: collapse;
            min-width: 100%;
            width: max-content;
            background: #FFFBF5;
        }
        th, td {
            border: 1px solid #E6DCCB;
            padding: 12px 16px;
            text-align: left;
            white-space: nowrap;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            font-size: 14px;
            line-height: 1.4;
            color: #1A1A1A;
        }
        th {
            background: #F0E6D6;
            font-weight: 600;
            color: #0B2A24;
        }
        tr:nth-child(even) td {
            background: #FAF6EF;
        }
    """.trimIndent()
}
