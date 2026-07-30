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
}
