package com.nus.folio.domain.util

/**
 * Client-side rules for space create/rename forms.
 */
object SpaceInputRules {
    const val MAX_TITLE_LENGTH = 100
    const val MAX_OBJECTIVE_LENGTH = 500

    fun limitTitle(value: String): String = value.take(MAX_TITLE_LENGTH)

    fun limitObjective(value: String): String = value.take(MAX_OBJECTIVE_LENGTH)
}
