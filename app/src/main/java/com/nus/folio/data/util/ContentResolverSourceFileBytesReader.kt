package com.nus.folio.data.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.nus.folio.domain.repository.SourceFileBytes
import com.nus.folio.domain.repository.SourceFileBytesReader
import com.nus.folio.domain.util.AddSourceInputRules
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ContentResolverSourceFileBytesReader(
    context: Context,
) : SourceFileBytesReader {

    private val appContext = context.applicationContext

    override fun read(uriString: String): SourceFileBytes {
        val uri = Uri.parse(uriString)
        val resolver = appContext.contentResolver
        val fileName = queryDisplayName(uri) ?: uri.lastPathSegment ?: "source"
        val reportedSize = querySizeBytes(uri)
        when (AddSourceInputRules.validateFile(fileName, reportedSize)) {
            AddSourceInputRules.FileValidationError.UNSUPPORTED_FORMAT ->
                throw IllegalArgumentException("Unsupported file format")
            AddSourceInputRules.FileValidationError.SIZE_EXCEEDED ->
                throw IllegalArgumentException("File size exceeds 50 MB limit")
            null -> Unit
        }
        val extension = AddSourceInputRules.extensionOf(fileName)
        val mimeType = resolver.getType(uri)?.takeIf { it.isNotBlank() }
            ?: SourceMimeTypes.forExtension(extension)
        val bytes = resolver.openInputStream(uri)?.use { input ->
            readBytesBounded(input, AddSourceInputRules.MAX_FILE_BYTES)
        } ?: throw IllegalStateException("Could not read selected file")
        require(bytes.isNotEmpty()) { "Selected file is empty" }
        return SourceFileBytes(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes,
        )
    }

    private fun queryDisplayName(uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return appContext.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index < 0) null else cursor.getString(index)?.takeIf { it.isNotBlank() }
        }
    }

    private fun querySizeBytes(uri: Uri): Long? {
        val projection = arrayOf(OpenableColumns.SIZE)
        return appContext.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index < 0 || cursor.isNull(index)) null else cursor.getLong(index).takeIf { it >= 0 }
        }
    }
}

/**
 * Reads at most [maxBytes] + 1 bytes from [input]. Throws if more than [maxBytes]
 * are available so callers never allocate an unbounded [ByteArray].
 */
internal fun readBytesBounded(input: InputStream, maxBytes: Long): ByteArray {
    require(maxBytes >= 0) { "maxBytes must be non-negative" }
    val probeLimit = maxBytes + 1
    require(probeLimit <= Int.MAX_VALUE) { "maxBytes too large" }
    val limit = probeLimit.toInt()
    val chunk = ByteArray(DEFAULT_BUFFER_SIZE.coerceAtMost(limit).coerceAtLeast(1))
    val output = ByteArrayOutputStream(chunk.size.coerceAtMost(limit))
    var total = 0
    while (total < limit) {
        val toRead = minOf(chunk.size, limit - total)
        val read = input.read(chunk, 0, toRead)
        if (read < 0) break
        output.write(chunk, 0, read)
        total += read
    }
    if (total > maxBytes) {
        throw IllegalArgumentException("File size exceeds 50 MB limit")
    }
    return output.toByteArray()
}

private const val DEFAULT_BUFFER_SIZE = 8 * 1024
