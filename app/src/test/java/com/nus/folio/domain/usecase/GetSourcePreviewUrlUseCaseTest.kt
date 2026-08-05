package com.nus.folio.domain.usecase

import com.nus.folio.testing.FakeSourceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetSourcePreviewUrlUseCaseTest {

    private val repository = FakeSourceRepository()
    private val useCase = GetSourcePreviewUrlUseCase(repository)

    @Test
    fun `invoke returns preview url for file source`() = runTest {
        val result = useCase("1")

        assertTrue(result.isSuccess)
        assertEquals("https://example.org/preview/1.pdf", result.getOrNull())
        assertEquals(1, repository.getSourcePreviewUrlCallCount)
    }

    @Test
    fun `invoke returns null when unavailable`() = runTest {
        repository.getSourcePreviewUrlResult = Result.success(null)

        val result = useCase("6")

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
}
