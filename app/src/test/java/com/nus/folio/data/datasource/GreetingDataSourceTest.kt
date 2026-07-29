package com.nus.folio.data.datasource

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingDataSourceTest {

    private val dataSource = GreetingDataSource()

    @Test
    fun `fetchGreeting returns Hello Folio`() = runTest {
        val greeting = dataSource.fetchGreeting()

        assertEquals("Hello Folio!", greeting.message)
    }
}
