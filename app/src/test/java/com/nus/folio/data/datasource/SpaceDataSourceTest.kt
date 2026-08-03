package com.nus.folio.data.datasource

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceDataSourceTest {

    private val dataSource = SpaceDataSource()

    @Test
    fun `fetchSpaces returns sample spaces`() = runTest {
        val spaces = dataSource.fetchSpaces()

        assertEquals(4, spaces.size)
        assertEquals("Dissertation Research", spaces.first().title)
        assertTrue(spaces.any { it.id == "4" && it.title == "Teaching Prep" })
    }
}
