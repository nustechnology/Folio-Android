package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.SourceFileLocation
import com.nus.folio.testing.FakeSourceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetSourceOriginalFileUseCaseTest {

    private val repository = FakeSourceRepository()
    private val useCase = GetSourceOriginalFileUseCase(repository)

    @Test
    fun `invoke returns local file for known source`() = runTest {
        val result = useCase("1", "1")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is SourceFileLocation.Local)
        assertEquals("application/pdf", (result.getOrNull() as SourceFileLocation.Local).mimeType)
    }

    @Test
    fun `invoke uses repository override when provided`() = runTest {
        repository.getOriginalFileResult = Result.success(
            SourceFileLocation.Remote("https://example.org/article"),
        )

        val result = useCase("1", "9")

        assertTrue(result.getOrNull() is SourceFileLocation.Remote)
    }
}
