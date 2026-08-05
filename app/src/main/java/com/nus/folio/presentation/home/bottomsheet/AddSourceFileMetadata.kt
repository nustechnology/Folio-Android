package com.nus.folio.presentation.home.bottomsheet

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.nus.folio.domain.util.AddSourceInputRules
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

internal data class SelectedSourceFile(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long?,
    val pageCount: Int?,
    val characterCount: Int?,
    val author: String?,
    val validationError: AddSourceInputRules.FileValidationError?,
)

internal fun resolveSelectedSourceFile(context: Context, uri: Uri): SelectedSourceFile {
    val resolver = context.contentResolver
    val displayName = queryDisplayName(resolver, uri)
        ?: uri.lastPathSegment
        ?: "source"
    val sizeBytes = querySizeBytes(resolver, uri)
    val extension = AddSourceInputRules.extensionOf(displayName)
    val validationError = AddSourceInputRules.validateFile(displayName, sizeBytes)

    if (validationError != null) {
        return SelectedSourceFile(
            uri = uri,
            displayName = displayName,
            sizeBytes = sizeBytes,
            pageCount = null,
            characterCount = null,
            author = null,
            validationError = validationError,
        )
    }

    val pageCount = if (extension == "pdf") {
        runCatching { pdfPageCount(resolver, uri) }.getOrNull()
    } else {
        null
    }
    val characterCount = if (extension in TEXT_EXTENSIONS && (sizeBytes == null || sizeBytes <= TEXT_META_MAX_BYTES)) {
        runCatching { countCharacters(resolver, uri) }.getOrNull()
    } else {
        null
    }
    val author = runCatching { queryAuthorHint(resolver, uri) }.getOrNull()

    return SelectedSourceFile(
        uri = uri,
        displayName = displayName,
        sizeBytes = sizeBytes,
        pageCount = pageCount,
        characterCount = characterCount,
        author = author,
        validationError = null,
    )
}

internal fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceAtMost(3)
    val unit = arrayOf("KB", "MB", "GB")[exp - 1]
    val value = bytes / 1024.0.pow(exp.toDouble())
    return String.format(Locale.US, "%.1f %s", value, unit)
}

private val TEXT_EXTENSIONS = setOf("txt", "md", "csv")
private const val TEXT_META_MAX_BYTES = 2L * 1024 * 1024

private fun queryDisplayName(resolver: android.content.ContentResolver, uri: Uri): String? {
    return resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index < 0) null else cursor.getString(index)?.takeIf { it.isNotBlank() }
    }
}

private fun querySizeBytes(resolver: android.content.ContentResolver, uri: Uri): Long? {
    return resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val index = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (index < 0 || cursor.isNull(index)) null else cursor.getLong(index).takeIf { it >= 0 }
    }
}

private fun queryAuthorHint(resolver: android.content.ContentResolver, uri: Uri): String? {
    // Best-effort: some providers expose author/creator columns.
    val candidates = arrayOf("author", "creator", "Artist", OpenableColumns.DISPLAY_NAME)
    return resolver.query(uri, null, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        for (name in candidates) {
            if (name == OpenableColumns.DISPLAY_NAME) continue
            val index = cursor.getColumnIndex(name)
            if (index >= 0 && !cursor.isNull(index)) {
                val value = cursor.getString(index)?.trim().orEmpty()
                if (value.isNotBlank()) return@use value
            }
        }
        null
    }
}

private fun pdfPageCount(resolver: android.content.ContentResolver, uri: Uri): Int? {
    val pfd = resolver.openFileDescriptor(uri, "r") ?: return null
    return pfd.use { descriptor ->
        PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
    }
}

private fun countCharacters(resolver: android.content.ContentResolver, uri: Uri): Int? {
    val stream = resolver.openInputStream(uri) ?: return null
    return stream.use { input ->
        BufferedReader(InputStreamReader(input, Charset.defaultCharset())).use { reader ->
            var count = 0
            val buffer = CharArray(8_192)
            while (true) {
                val read = reader.read(buffer)
                if (read < 0) break
                count += read
                if (count > AddSourceInputRules.MAX_CONTENT_LENGTH) break
            }
            count
        }
    }
}
