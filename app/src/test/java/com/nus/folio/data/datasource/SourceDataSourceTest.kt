package com.nus.folio.data.datasource

import com.nus.folio.domain.model.SourceType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceDataSourceTest {

    private val dataSource = SourceDataSource()

    @Test
    fun `fetchSources returns sample library`() = runTest {
        val library = dataSource.fetchSources()

        assertEquals(128, library.allCount)
        assertEquals(80, library.papersCount)
        assertEquals(24, library.booksCount)
        assertEquals(18, library.webCount)
        assertEquals(6, library.textCount)
        assertEquals(6, library.sources.size)
        assertTrue(library.sources.any { it.title.contains("Turing") })
        assertTrue(library.sources.any { it.type == SourceType.TEXT })
    }
}
