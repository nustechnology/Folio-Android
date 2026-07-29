package com.nus.folio.presentation.resetpassword

import com.nus.folio.domain.usecase.RequestPasswordResetUseCase
import com.nus.folio.testing.FakeAuthRepository
import com.nus.folio.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ResetPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()

    private fun createViewModel(): ResetPasswordViewModel =
        ResetPasswordViewModel(RequestPasswordResetUseCase(repository))

    @Test
    fun `onSendRecoveryLinkClick with blank email sets EMAIL_REQUIRED`() {
        val viewModel = createViewModel()

        viewModel.onSendRecoveryLinkClick(" ")

        assertEquals(ResetPasswordError.EMAIL_REQUIRED, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.info)
        assertEquals(0, repository.requestPasswordResetCallCount)
    }

    @Test
    fun `onSendRecoveryLinkClick success sets LINK_SENT`() {
        repository.requestPasswordResetResult = Result.success(Unit)
        val viewModel = createViewModel()

        viewModel.onSendRecoveryLinkClick("user@folio.app")

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(ResetPasswordInfo.LINK_SENT, viewModel.uiState.value.info)
        assertNull(viewModel.uiState.value.error)
        assertEquals("user@folio.app", repository.lastPasswordResetEmail)
    }

    @Test
    fun `onSendRecoveryLinkClick failure sets SEND_FAILED`() {
        repository.requestPasswordResetResult = Result.failure(IllegalStateException("send failed"))
        val viewModel = createViewModel()

        viewModel.onSendRecoveryLinkClick("user@folio.app")

        assertEquals(ResetPasswordError.SEND_FAILED, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.info)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `clearFeedback clears error and info`() {
        repository.requestPasswordResetResult = Result.success(Unit)
        val viewModel = createViewModel()
        viewModel.onSendRecoveryLinkClick("user@folio.app")
        assertTrue(viewModel.uiState.value.info != null)

        viewModel.clearFeedback()

        assertNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.info)
    }
}
