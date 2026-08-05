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
    fun `updateSpace persists name and objective`() = runTest {
        val updated = dataSource.updateSpace(
            spaceId = "1",
            name = "  Renamed Research  ",
            researchObjective = "  New objective  ",
        )

        assertEquals("1", updated.id)
        assertEquals("Renamed Research", updated.title)
        assertEquals("New objective", updated.description)

        val page = dataSource.fetchSpaces(searchQuery = "Renamed")
        assertEquals(1, page.spaces.size)
        assertEquals("Renamed Research", page.spaces.first().title)
        assertEquals("New objective", page.spaces.first().description)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateSpace rejects blank name`() = runTest {
        dataSource.updateSpace(spaceId = "1", name = " ", researchObjective = "Objective")
    }

    @Test(expected = NoSuchElementException::class)
    fun `updateSpace throws when space is missing`() = runTest {
        dataSource.updateSpace(
            spaceId = "missing",
            name = "Renamed",
            researchObjective = "Objective",
        )
    }

    @Test
    fun `deleteSpace removes space from list`() = runTest {
        dataSource.deleteSpace(spaceId = "1")

        val page = dataSource.fetchSpaces(limit = 20)
        assertEquals(3, page.spaces.size)
        assertTrue(page.spaces.none { it.id == "1" })
    }

    @Test(expected = NoSuchElementException::class)
    fun `deleteSpace throws when space is missing`() = runTest {
        dataSource.deleteSpace(spaceId = "missing")
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
