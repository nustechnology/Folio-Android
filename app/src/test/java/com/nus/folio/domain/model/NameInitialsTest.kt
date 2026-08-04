package com.nus.folio.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NameInitialsTest {

    @Test
    fun `uses first and last name initials`() {
        assertEquals("JL", initialsFromDisplayName("Jordan Lee"))
        assertEquals("JD", initialsFromDisplayName("John Michael Doe"))
    }

    @Test
    fun `uses single initial when only one name`() {
        assertEquals("J", initialsFromDisplayName("Jordan"))
    }

    @Test
    fun `falls back to email when name is blank`() {
        assertEquals("U", initialsFromDisplayName("  ", "user@folio.app"))
        assertEquals("", initialsFromDisplayName(""))
    }
}
