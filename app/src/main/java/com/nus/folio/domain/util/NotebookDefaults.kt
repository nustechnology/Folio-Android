package com.nus.folio.domain.util

/**
 * Default markdown seeded into an empty notebook for a research space.
 */
object NotebookDefaults {
    private const val FALLBACK_TITLE = "Untitled Research"
    private const val OBJECTIVE_HEADING = "## Research Objective"

    fun template(
        spaceTitle: String,
        researchObjective: String,
    ): String {
        val resolvedTitle = spaceTitle.trim().ifBlank { FALLBACK_TITLE }
        val resolvedObjective = researchObjective.trim()
        return buildString {
            appendLine("# Title")
            appendLine(resolvedTitle)
            appendLine()
            appendLine(OBJECTIVE_HEADING)
            if (resolvedObjective.isNotEmpty()) {
                appendLine(resolvedObjective)
            }
        }.trimEnd() + "\n"
    }

    /**
     * Seeds a blank notebook, or fills an empty Research Objective section with
     * [researchObjective] when that body is still blank.
     */
    fun applyDefaults(
        content: String,
        spaceTitle: String,
        researchObjective: String,
    ): String {
        val objective = researchObjective.trim()
        if (content.isBlank()) {
            return template(spaceTitle, objective)
        }
        if (objective.isEmpty()) return content
        // Full re-seed only when the stock scaffold has an empty objective body.
        if (shouldSeedTemplate(content, spaceTitle)) {
            val remainder = content.trim()
                .removePrefix(
                    buildString {
                        appendLine("# Title")
                        appendLine(spaceTitle.trim().ifBlank { FALLBACK_TITLE })
                        appendLine()
                        append(OBJECTIVE_HEADING)
                    },
                )
                .trim()
            if (remainder.isEmpty()) {
                return template(spaceTitle, objective)
            }
        }
        return fillEmptyObjectiveBody(content, objective)
    }

    /**
     * True when [content] is blank or only the stock Title / Research Objective scaffold
     * (with an empty or previously seeded objective body), so it is safe to re-seed.
     */
    fun shouldSeedTemplate(content: String, spaceTitle: String): Boolean {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return true
        val resolvedTitle = spaceTitle.trim().ifBlank { FALLBACK_TITLE }
        val headingPrefix = buildString {
            appendLine("# Title")
            appendLine(resolvedTitle)
            appendLine()
            append(OBJECTIVE_HEADING)
        }
        if (!trimmed.startsWith(headingPrefix)) return false
        val remainder = trimmed.removePrefix(headingPrefix).trim()
        return remainder.isEmpty() ||
            (!remainder.contains('\n') && !remainder.startsWith("#"))
    }

    /**
     * Inserts [objective] under `## Research Objective` when that section has no body yet.
     * Leaves content unchanged when the section already has text or is missing.
     */
    internal fun fillEmptyObjectiveBody(content: String, objective: String): String {
        val trimmedObjective = objective.trim()
        if (trimmedObjective.isEmpty()) return content
        if (content.contains(trimmedObjective)) return content

        val headingIndex = content.indexOf(OBJECTIVE_HEADING)
        if (headingIndex < 0) return content

        val bodyStart = headingIndex + OBJECTIVE_HEADING.length
        val afterHeading = content.substring(bodyStart)
        val nextHeading = Regex("\\n##\\s").find(afterHeading)
        val body = if (nextHeading != null) {
            afterHeading.substring(0, nextHeading.range.first)
        } else {
            afterHeading
        }
        if (body.trim().isNotEmpty()) return content

        val leadingNewline = if (afterHeading.startsWith("\n")) "" else "\n"
        val insertion = "$leadingNewline$trimmedObjective"
        val bodyEnd = bodyStart + body.length
        return content.substring(0, bodyStart) + insertion + content.substring(bodyEnd)
    }
}
