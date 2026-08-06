package com.nus.folio.presentation.home.bottomsheet

import android.net.Uri

enum class AddSourceTab {
    FILE,
    WEB,
    TEXT,
}

sealed interface AddSourceDraft {
    data class File(
        val displayName: String,
        val uri: Uri?,
        val author: String = "",
    ) : AddSourceDraft

    data class Web(
        val url: String,
        val title: String,
        val author: String,
    ) : AddSourceDraft

    data class Text(
        val title: String,
        val author: String,
        val content: String,
    ) : AddSourceDraft
}
