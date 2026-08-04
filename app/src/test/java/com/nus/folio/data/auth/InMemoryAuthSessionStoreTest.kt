package com.nus.folio.data.auth

import com.nus.folio.domain.model.AuthSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryAuthSessionStoreTest {

    @Test
    fun `write and read round trip`() = runTest {
        val store = InMemoryAuthSessionStore()
        val session = AuthSession(
            email = "jordan@folio.app",
            displayName = "Jordan Lee",
            accessToken = "access",
            refreshToken = "refresh",
        )

        store.write(session)

        assertEquals(session, store.read())
    }

    @Test
    fun `clear removes session`() = runTest {
        val store = InMemoryAuthSessionStore()
        store.write(AuthSession(email = "jordan@folio.app"))

        store.clear()

        assertNull(store.read())
    }
}
