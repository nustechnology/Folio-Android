package com.nus.folio.data.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SafeRelativePathTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `requireSourceId accepts alphanumeric ids`() {
        assertEquals("1", SafeRelativePath.requireSourceId("1"))
        assertEquals("source_10", SafeRelativePath.requireSourceId("source_10"))
        assertEquals("abc-DEF", SafeRelativePath.requireSourceId("abc-DEF"))
    }

    @Test
    fun `requireSourceId rejects path traversal and separators`() {
        assertThrows(IllegalArgumentException::class.java) {
            SafeRelativePath.requireSourceId("../escape")
        }
        assertThrows(IllegalArgumentException::class.java) {
            SafeRelativePath.requireSourceId("a/b")
        }
        assertThrows(IllegalArgumentException::class.java) {
            SafeRelativePath.requireSourceId("a\\b")
        }
        assertThrows(IllegalArgumentException::class.java) {
            SafeRelativePath.requireSourceId("")
        }
        assertThrows(IllegalArgumentException::class.java) {
            SafeRelativePath.requireSourceId("..")
        }
    }

    @Test
    fun `requireFileName accepts simple file names`() {
        assertEquals(
            "alan-turing.pdf",
            SafeRelativePath.requireFileName("alan-turing.pdf"),
        )
    }

    @Test
    fun `requireFileName rejects traversal segments`() {
        assertThrows(IllegalArgumentException::class.java) {
            SafeRelativePath.requireFileName("../evil.pdf")
        }
        assertThrows(IllegalArgumentException::class.java) {
            SafeRelativePath.requireFileName("..")
        }
        assertThrows(IllegalArgumentException::class.java) {
            SafeRelativePath.requireFileName("sub/dir.pdf")
        }
    }

    @Test
    fun `resolveUnder returns path under base`() {
        val base = temporaryFolder.newFolder("source_originals")
        val resolved = SafeRelativePath.resolveUnder(base, "1", "doc.pdf")

        assertTrue(resolved.canonicalPath.startsWith(base.canonicalPath + File.separator))
        assertEquals(File(File(base, "1"), "doc.pdf").canonicalFile, resolved)
    }

    @Test
    fun `resolveUnder rejects escape via parent segments`() {
        val base = temporaryFolder.newFolder("source_originals")

        assertThrows(IllegalArgumentException::class.java) {
            SafeRelativePath.resolveUnder(base, "..", "escape.txt")
        }
    }
}
