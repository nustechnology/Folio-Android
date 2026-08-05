package com.nus.folio.data.repository

import com.nus.folio.data.datasource.SpaceDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceRepositoryImplTest {

    private val repository = SpaceRepositoryImpl(SpaceDataSource())

    @Test
    fun `getSpaces returns success page`() = runTest {
        val result = repository.getSpaces()

        assertTrue(result.isSuccess)
        assertEquals(4, result.getOrNull()?.spaces?.size)
        assertEquals("Dissertation Research", result.getOrNull()?.spaces?.first()?.title)
        assertFalse(result.getOrNull()!!.hasMore)
    }

    @Test
    fun `getSpaces forwards search query`() = runTest {
        val result = repository.getSpaces(searchQuery = "Policy")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.spaces?.size)
        assertEquals("Public Policy Insights", result.getOrNull()?.spaces?.first()?.title)
    }

    @Test
    fun `getSpaces pages local sample data`() = runTest {
        val page1 = repository.getSpaces(page = 1, limit = 2)
        val page2 = repository.getSpaces(page = 2, limit = 2)

        assertTrue(page1.getOrNull()!!.hasMore)
        assertEquals(listOf("1", "2"), page1.getOrNull()!!.spaces.map { it.id })
        assertFalse(page2.getOrNull()!!.hasMore)
        assertEquals(listOf("3", "4"), page2.getOrNull()!!.spaces.map { it.id })
    }

    @Test
    fun `createSpace returns created space`() = runTest {
        val result = repository.createSpace(
            name = "New Space",
            researchObjective = "Objective",
        )

        assertTrue(result.isSuccess)
        assertEquals("New Space", result.getOrNull()?.title)
        assertEquals("Objective", result.getOrNull()?.description)
        assertEquals(5, repository.getSpaces(limit = 20).getOrNull()?.spaces?.size)
    }

    @Test
    fun `updateSpace returns updated space`() = runTest {
        val result = repository.updateSpace(
            spaceId = "1",
            name = "Renamed Space",
            researchObjective = "Updated objective",
        )

        assertTrue(result.isSuccess)
        assertEquals("Renamed Space", result.getOrNull()?.title)
        assertEquals("Updated objective", result.getOrNull()?.description)
        assertEquals(
            "Renamed Space",
            repository.getSpaces().getOrNull()?.spaces?.first { it.id == "1" }?.title,
        )
    }

    @Test
    fun `deleteSpace removes space`() = runTest {
        val result = repository.deleteSpace("1")

        assertTrue(result.isSuccess)
        assertEquals(3, repository.getSpaces(limit = 20).getOrNull()?.spaces?.size)
        assertTrue(repository.getSpaces().getOrNull()!!.spaces.none { it.id == "1" })
    }
}
