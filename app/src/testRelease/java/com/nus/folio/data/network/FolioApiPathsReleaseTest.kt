package com.nus.folio.data.network

import org.junit.Assert.assertEquals
import org.junit.Test

class FolioApiPathsReleaseTest {

    @Test
    fun `release base url is the production Folio host`() {
        assertEquals("https://folio.nustechnology.com", FolioApiPaths.BASE_URL)
        assertEquals(
            "https://folio.nustechnology.com/api/v1/auth/login",
            FolioApiPaths.authLogin(),
        )
    }
}
