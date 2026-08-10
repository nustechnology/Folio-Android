package com.nus.folio.domain.model

enum class NoteSort(val apiValue: String) {
    RECENTLY_UPDATED("recently-updated"),
    RECENTLY_CREATED("recently-created"),
    ALPHABETICAL_AZ("alphabetical-az"),
    ALPHABETICAL_ZA("alphabetical-za"),
    ;

    companion object {
        val DEFAULT = RECENTLY_UPDATED
    }
}
