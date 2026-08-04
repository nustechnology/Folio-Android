package com.nus.folio.data.datasource

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceDataSourceTest {

    private val dataSource = SpaceDataSource()

    @Test
    fun `fetchSpaces returns sample spaces`() = runTest {
        val page = dataSource.fetchSpaces()

        assertEquals(4, page.spaces.size)
        assertEquals("Dissertation Research", page.spaces.first().title)
        assertTrue(page.spaces.any { it.id == "4" && it.title == "Teaching Prep" })
        assertFalse(page.hasMore)
    }

    @Test
    fun `fetchSpaces filters by search query locally`() = runTest {
        val page = dataSource.fetchSpaces(searchQuery = "Policy")

        assertEquals(1, page.spaces.size)
        assertEquals("Public Policy Insights", page.spaces.first().title)
        assertFalse(page.hasMore)
    }

    @Test
    fun `fetchSpaces slices by page and limit`() = runTest {
        val page1 = dataSource.fetchSpaces(page = 1, limit = 2)
        val page2 = dataSource.fetchSpaces(page = 2, limit = 2)

        assertEquals(2, page1.spaces.size)
        assertTrue(page1.hasMore)
        assertEquals(listOf("1", "2"), page1.spaces.map { it.id })

        assertEquals(2, page2.spaces.size)
        assertFalse(page2.hasMore)
        assertEquals(listOf("3", "4"), page2.spaces.map { it.id })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createSpace rejects blank name before persisting`() = runTest {
        dataSource.createSpace(name = " ", researchObjective = "Objective")
    }

    @Test
    fun `concurrent createSpace calls keep both spaces`() = runTest {
        val first = async {
            dataSource.createSpace(name = "Alpha Lab", researchObjective = "A")
        }
        val second = async {
            dataSource.createSpace(name = "Beta Lab", researchObjective = "B")
        }

        val created = listOf(first.await(), second.await())
        val page = dataSource.fetchSpaces(limit = 20)

        assertEquals(6, page.spaces.size)
        assertTrue(page.spaces.any { it.id == created[0].id && it.title == "Alpha Lab" })
        assertTrue(page.spaces.any { it.id == created[1].id && it.title == "Beta Lab" })
    }
}
