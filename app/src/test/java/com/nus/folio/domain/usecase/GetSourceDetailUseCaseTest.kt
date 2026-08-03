package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.testing.FakeSourceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetSourceDetailUseCaseTest {

    private val repository = FakeSourceRepository()
    private val useCase = GetSourceDetailUseCase(repository)

    @Test
    fun `invoke returns source detail for known source`() = runTest {
        val result = useCase("1", "1")

        assertTrue(result.isSuccess)
        assertEquals("Alan Turing: Computing Machinery", result.getOrNull()?.title)
        assertEquals(SourceContentFormat.DOCUMENT, result.getOrNull()?.contentFormat)
        assertEquals("source.pdf", result.getOrNull()?.originalFileName)
    }

    @Test
    fun `invoke returns failure when source is missing`() = runTest {
        val result = useCase("1", "missing")

        assertTrue(result.isFailure)
    }

    @Test
    fun `invoke uses repository override when provided`() = runTest {
        repository.getSourceDetailResult = Result.success(
            SourceDetail(
                id = "custom",
                title = "Custom source",
                author = "Author",
                addedLabel = "Added today",
                type = SourceType.TEXT,
                status = SourceStatus.READY,
                spaceId = "1",
                fileExtension = "md",
                contentFormat = SourceContentFormat.DOCUMENT,
                originalFileName = "custom.md",
                htmlContent = "<p>Custom</p>",
            ),
        )

        val result = useCase("1", "custom")

        assertEquals("Custom source", result.getOrNull()?.title)
    }
}
