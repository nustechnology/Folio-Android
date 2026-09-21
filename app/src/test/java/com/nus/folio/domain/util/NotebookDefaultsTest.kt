package com.nus.folio.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotebookDefaultsTest {

    @Test
    fun `template includes space title and research objective`() {
        val markdown = NotebookDefaults.template(
            spaceTitle = "Dissertation Research",
            researchObjective = "Primary research archive for doctoral thesis",
        )
        assertEquals(
            """
            # Title
            Dissertation Research

            ## Research Objective
            Primary research archive for doctoral thesis
            """.trimIndent() + "\n",
            markdown,
        )
    }

    @Test
    fun `template keeps Research Objective heading when objective blank`() {
        val markdown = NotebookDefaults.template(
            spaceTitle = "Teaching Prep",
            researchObjective = "  ",
        )
        assertTrue(markdown.contains("## Research Objective"))
        assertFalse(markdown.contains("Primary research"))
    }

    @Test
    fun `shouldSeedTemplate is true for blank content`() {
        assertTrue(NotebookDefaults.shouldSeedTemplate("", "Dissertation Research"))
        assertTrue(NotebookDefaults.shouldSeedTemplate("   \n", "Dissertation Research"))
    }

    @Test
    fun `shouldSeedTemplate is true for empty objective scaffold`() {
        val scaffold = NotebookDefaults.template(
            spaceTitle = "Dissertation Research",
            researchObjective = "",
        )
        assertTrue(
            NotebookDefaults.shouldSeedTemplate(scaffold, "Dissertation Research"),
        )
    }

    @Test
    fun `shouldSeedTemplate is true for scaffold with only objective body`() {
        val seeded = NotebookDefaults.template(
            spaceTitle = "Dissertation Research",
            researchObjective = "Old objective",
        )
        assertTrue(
            NotebookDefaults.shouldSeedTemplate(seeded, "Dissertation Research"),
        )
    }

    @Test
    fun `shouldSeedTemplate is false for user notes beyond the scaffold`() {
        val content = """
            # Title
            Dissertation Research

            ## Research Objective
            Primary research archive for doctoral thesis

            ## Findings
            Something I wrote
        """.trimIndent()
        assertFalse(
            NotebookDefaults.shouldSeedTemplate(content, "Dissertation Research"),
        )
    }

    @Test
    fun `applyDefaults fills empty Research Objective body in existing scaffold`() {
        val content = """
            # Title
            Dissertation Research

            ## Research Objective

        """.trimIndent()
        val applied = NotebookDefaults.applyDefaults(
            content = content,
            spaceTitle = "Dissertation Research",
            researchObjective = "Primary research archive for doctoral thesis",
        )
        assertTrue(applied.contains("Primary research archive for doctoral thesis"))
        assertTrue(applied.contains("## Research Objective"))
    }

    @Test
    fun `applyDefaults does not overwrite existing Research Objective body`() {
        val content = """
            # Title
            Dissertation Research

            ## Research Objective
            Keep this objective
        """.trimIndent()
        val applied = NotebookDefaults.applyDefaults(
            content = content,
            spaceTitle = "Dissertation Research",
            researchObjective = "Different objective",
        )
        assertTrue(applied.contains("Keep this objective"))
        assertFalse(applied.contains("Different objective"))
    }
}
