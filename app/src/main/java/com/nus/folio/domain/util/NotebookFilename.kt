package com.nus.folio.domain.util

object NotebookFilename {
    fun forSpace(spaceTitle: String): String =
        "${slugify(spaceTitle)}-notebook.md"

    internal fun slugify(title: String): String {
        val slug = title
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return slug.ifEmpty { "notebook" }
    }
}
