package com.nus.folio.data.util

import java.io.File

/**
 * Guards against path traversal when building paths under a trusted base directory.
 */
internal object SafeRelativePath {

    private val SOURCE_ID = Regex("^[A-Za-z0-9_-]+$")
    private val FILE_NAME = Regex("^[A-Za-z0-9][A-Za-z0-9._-]*$")

    fun requireSourceId(sourceId: String): String {
        require(SOURCE_ID.matches(sourceId)) {
            "Invalid sourceId for file path: $sourceId"
        }
        return sourceId
    }

    fun requireFileName(fileName: String): String {
        require(fileName != "." && fileName != ".." && FILE_NAME.matches(fileName)) {
            "Invalid file name for file path: $fileName"
        }
        return fileName
    }

    fun resolveUnder(base: File, vararg segments: String): File {
        val baseCanonical = base.canonicalFile
        val resolved = segments.fold(baseCanonical) { parent, segment ->
            File(parent, segment)
        }.canonicalFile
        val basePath = baseCanonical.path
        require(
            resolved == baseCanonical ||
                resolved.path.startsWith(basePath + File.separator),
        ) {
            "Resolved path escapes base directory: ${resolved.path}"
        }
        return resolved
    }
}
