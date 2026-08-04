package com.nus.folio.domain.model

enum class SpaceSort(val apiValue: String) {
    RECENTLY_CREATED("recently-created"),
    RECENTLY_UPDATED("recently-updated"),
    ALPHABETICAL_AZ("alphabetical-az"),
    ALPHABETICAL_ZA("alphabetical-za"),
    ;

    companion object {
        val DEFAULT = RECENTLY_UPDATED
    }
}
