package com.nus.folio.data.datasource

import com.nus.folio.domain.model.SourceType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceDataSourceTest {

    private val dataSource = SourceDataSource()

    @Test
    fun `fetchSources returns space-scoped library`() = runTest {
        val library = dataSource.fetchSources("1")

        assertEquals(4, library.allCount)
        assertEquals(3, library.papersCount)
        assertEquals(0, library.booksCount)
        assertEquals(1, library.webCount)
        assertEquals(0, library.textCount)
        assertEquals(4, library.sources.size)
        assertTrue(library.sources.all { it.spaceId == "1" })
        assertTrue(library.sources.any { it.title.contains("Turing") })
    }

    @Test
    fun `fetchSources returns different content for another space`() = runTest {
        val library = dataSource.fetchSources("4")

        assertEquals(2, library.sources.size)
        assertTrue(library.sources.all { it.spaceId == "4" })
    }

    @Test
    fun `updateSource persists title and author for later fetch`() = runTest {
        val original = dataSource.fetchSources("1").sources.first { it.id == "1" }

        val updated = dataSource.updateSource(
            original.copy(title = "Updated title", author = "Updated author"),
        )
        val library = dataSource.fetchSources("1")

        assertEquals("Updated title", updated.title)
        assertEquals("Updated author", updated.author)
        val reloaded = library.sources.first { it.id == "1" }
        assertEquals("Updated title", reloaded.title)
        assertEquals("Updated author", reloaded.author)
    }

    @Test
    fun `deleteSource removes source from later fetch`() = runTest {
        dataSource.deleteSource("1")
        val library = dataSource.fetchSources("1")

        assertEquals(3, library.allCount)
        assertTrue(library.sources.none { it.id == "1" })
    }
}
