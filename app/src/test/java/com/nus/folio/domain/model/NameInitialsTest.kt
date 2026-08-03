package com.nus.folio.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NameInitialsTest {

    @Test
    fun `uses first and last name initials`() {
        assertEquals("AN", initialsFromDisplayName("Alex Nguyen"))
        assertEquals("JD", initialsFromDisplayName("John Michael Doe"))
    }

    @Test
    fun `uses single initial when only one name`() {
        assertEquals("A", initialsFromDisplayName("Alex"))
    }

    @Test
    fun `falls back to email when name is blank`() {
        assertEquals("U", initialsFromDisplayName("  ", "user@folio.app"))
        assertEquals("", initialsFromDisplayName(""))
    }

    @Test
    fun `AuthSession avatarInitials uses display name`() {
        val session = AuthSession(email = "alex@folio.app", displayName = "Alex Nguyen")
        assertEquals("AN", session.avatarInitials)
    }
}
