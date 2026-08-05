package com.nus.folio.domain.model

sealed interface CreateSourceRequest {
    val spaceId: String

    data class Web(
        override val spaceId: String,
        val sourceUrl: String,
        val title: String,
        val author: String,
    ) : CreateSourceRequest

    data class Manual(
        override val spaceId: String,
        val title: String,
        val author: String,
        val content: String,
    ) : CreateSourceRequest

    data class File(
        override val spaceId: String,
        val title: String,
        val author: String,
        val fileName: String,
        val mimeType: String,
        val bytes: ByteArray,
    ) : CreateSourceRequest {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is File) return false
            return spaceId == other.spaceId &&
                title == other.title &&
                author == other.author &&
                fileName == other.fileName &&
                mimeType == other.mimeType &&
                bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            var result = spaceId.hashCode()
            result = 31 * result + title.hashCode()
            result = 31 * result + author.hashCode()
            result = 31 * result + fileName.hashCode()
            result = 31 * result + mimeType.hashCode()
            result = 31 * result + bytes.contentHashCode()
            return result
        }
    }
}
