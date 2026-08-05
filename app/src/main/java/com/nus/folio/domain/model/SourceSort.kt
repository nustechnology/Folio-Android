package com.nus.folio.domain.model

enum class SourceSort(val apiValue: String) {
    RECENTLY_ADDED("recently-added"),
    ALPHABETICAL_AZ("alphabetical-az"),
    ALPHABETICAL_ZA("alphabetical-za"),
    ;

    companion object {
        val DEFAULT = RECENTLY_ADDED
    }
}

/** API `sourceType` query value for list sources; null means omit (All). */
fun SourceFilter.toApiSourceType(): String? = when (this) {
    SourceFilter.ALL -> null
    SourceFilter.FILE -> "File"
    SourceFilter.WEB -> "Web"
    SourceFilter.TEXT -> "Manual"
}
