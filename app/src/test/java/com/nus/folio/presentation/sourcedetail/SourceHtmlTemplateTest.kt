package com.nus.folio.presentation.sourcedetail

import com.nus.folio.domain.model.SourceContentFormat
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceHtmlTemplateTest {

    @Test
    fun `wrap marks content as read-only`() {
        val html = SourceHtmlTemplate.wrap("<p>Sample</p>", SourceContentFormat.DOCUMENT)

        assertTrue(html.contains("""contenteditable="false""""))
        assertTrue(html.contains("-webkit-user-modify: read-only"))
    }
}
