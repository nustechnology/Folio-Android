package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.CreateSourceRequest
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.testing.FakeSourceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateSourceUseCaseTest {

    private val repository = FakeSourceRepository()
    private val useCase = CreateSourceUseCase(repository)

    @Test
    fun `invoke forwards web request and returns source`() = runTest {
        val request = CreateSourceRequest.Web(
            spaceId = "space-1",
            sourceUrl = "https://example.com/article",
            title = "Example Article",
            author = "",
        )
        repository.createSourceResult = Result.success(
            Source(
                id = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                title = "Example Article",
                type = SourceType.WEB,
                author = "",
                addedLabel = "Added just now",
                status = SourceStatus.PROCESSING,
                spaceId = "space-1",
            ),
        )

        val result = useCase(request)

        assertTrue(result.isSuccess)
        assertEquals(request, repository.lastCreateRequest)
        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", result.getOrNull()?.id)
    }

    @Test
    fun `invoke forwards manual request`() = runTest {
        val request = CreateSourceRequest.Manual(
            spaceId = "space-1",
            title = "My Research Notes",
            author = "Alice Johnson",
            content = "This is a manual source with enough content to satisfy validation.",
        )

        val result = useCase(request)

        assertTrue(result.isSuccess)
        assertEquals(request, repository.lastCreateRequest)
        assertEquals(SourceType.TEXT, result.getOrNull()?.type)
    }
}
