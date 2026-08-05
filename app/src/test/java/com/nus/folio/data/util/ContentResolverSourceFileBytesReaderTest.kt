package com.nus.folio.data.util

import com.nus.folio.domain.util.AddSourceInputRules
import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ContentResolverSourceFileBytesReaderTest {

    @Test
    fun `readBytesBounded returns full content within limit`() {
        val payload = byteArrayOf(1, 2, 3, 4, 5)
        val result = readBytesBounded(ByteArrayInputStream(payload), maxBytes = 10)
        assertArrayEquals(payload, result)
    }

    @Test
    fun `readBytesBounded rejects content larger than maxBytes`() {
        val payload = ByteArray(8) { it.toByte() }
        val error = assertThrows(IllegalArgumentException::class.java) {
            readBytesBounded(ByteArrayInputStream(payload), maxBytes = 7)
        }
        assertEquals("File size exceeds 50 MB limit", error.message)
    }

    @Test
    fun `readBytesBounded accepts content exactly at maxBytes`() {
        val payload = ByteArray(4) { 9 }
        val result = readBytesBounded(ByteArrayInputStream(payload), maxBytes = 4)
        assertArrayEquals(payload, result)
    }

    @Test
    fun `readBytesBounded stops after probe byte so stream is not fully drained when oversized`() {
        // 3 bytes allowed; stream has 10. Reader may consume at most 4 (max + 1).
        val stream = object : ByteArrayInputStream(ByteArray(10) { 1 }) {
            var bytesRead = 0
                private set

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val n = super.read(b, off, len)
                if (n > 0) bytesRead += n
                return n
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            readBytesBounded(stream, maxBytes = 3)
        }
        assertEquals(4, stream.bytesRead)
    }

    @Test
    fun `validateFile rejects unsupported name before any read would occur`() {
        assertEquals(
            AddSourceInputRules.FileValidationError.UNSUPPORTED_FORMAT,
            AddSourceInputRules.validateFile("notes.exe", sizeBytes = 12),
        )
    }
}
