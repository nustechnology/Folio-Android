package com.nus.folio.domain.repository

/**
 * Reads file bytes from a content URI string for create-source uploads.
 * Implementation lives in the data layer (ContentResolver).
 */
fun interface SourceFileBytesReader {
    fun read(uriString: String): SourceFileBytes
}

data class SourceFileBytes(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SourceFileBytes) return false
        return fileName == other.fileName &&
            mimeType == other.mimeType &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
