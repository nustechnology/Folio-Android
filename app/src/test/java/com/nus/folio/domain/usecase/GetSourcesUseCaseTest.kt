package com.nus.folio.domain.usecase

import com.nus.folio.testing.FakeSourceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetSourcesUseCaseTest {

    private val repository = FakeSourceRepository()
    private val useCase = GetSourcesUseCase(repository)

    @Test
    fun `invoke returns space-scoped library on success`() = runTest {
        val result = useCase("1")

        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull()?.allCount)
        assertEquals(5, result.getOrNull()?.sources?.size)
        assertEquals(0, result.getOrNull()?.textCount)
        assertEquals(1, repository.getSourcesCallCount)
        assertEquals("1", repository.lastSpaceId)
    }

    @Test
    fun `invoke forwards sourceType search sort and paging`() = runTest {
        val result = useCase(
            spaceId = "1",
            sourceType = "File",
            search = "Turing",
            sort = com.nus.folio.domain.model.SourceSort.ALPHABETICAL_AZ,
            page = 1,
            limit = 3,
        )

        assertTrue(result.isSuccess)
        assertEquals("File", repository.lastSourceType)
        assertEquals("Turing", repository.lastSearch)
        assertEquals(com.nus.folio.domain.model.SourceSort.ALPHABETICAL_AZ, repository.lastSort)
        assertEquals(1, repository.lastPage)
        assertEquals(3, repository.lastLimit)
        assertEquals(1, result.getOrNull()?.sources?.size)
        assertFalse(result.getOrNull()!!.hasMore)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.getSourcesResult = Result.failure(IllegalStateException("offline"))

        val result = useCase("1")

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }
}
