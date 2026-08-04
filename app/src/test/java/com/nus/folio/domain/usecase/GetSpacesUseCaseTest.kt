package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.SpacePaging
import com.nus.folio.domain.model.SpaceSort
import com.nus.folio.testing.FakeSpaceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetSpacesUseCaseTest {

    private val repository = FakeSpaceRepository()
    private val useCase = GetSpacesUseCase(repository)

    @Test
    fun `invoke returns spaces on success`() = runTest {
        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(4, result.getOrNull()?.spaces?.size)
        assertNull(repository.lastSearchQuery)
        assertEquals(SpaceSort.DEFAULT, repository.lastSort)
        assertEquals(SpacePaging.DEFAULT_PAGE, repository.lastPage)
        assertEquals(SpacePaging.DEFAULT_LIMIT, repository.lastLimit)
    }

    @Test
    fun `invoke forwards search query`() = runTest {
        val result = useCase(searchQuery = "Policy")

        assertTrue(result.isSuccess)
        assertEquals("Policy", repository.lastSearchQuery)
    }

    @Test
    fun `invoke forwards sort`() = runTest {
        val result = useCase(sort = SpaceSort.ALPHABETICAL_AZ)

        assertTrue(result.isSuccess)
        assertEquals(SpaceSort.ALPHABETICAL_AZ, repository.lastSort)
    }

    @Test
    fun `invoke forwards page and limit`() = runTest {
        val result = useCase(page = 2, limit = 10)

        assertTrue(result.isSuccess)
        assertEquals(2, repository.lastPage)
        assertEquals(10, repository.lastLimit)
        assertFalse(result.getOrNull()!!.hasMore)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.spacesResult = Result.failure(IllegalStateException("offline"))

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }
}
