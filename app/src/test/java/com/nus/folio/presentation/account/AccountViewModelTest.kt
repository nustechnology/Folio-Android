package com.nus.folio.presentation.account

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.usecase.GetCurrentSessionUseCase
import com.nus.folio.testing.FakeAuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountViewModelTest {

    private val authRepository = FakeAuthRepository().apply {
        signInResult = Result.success(
            AuthSession(
                email = "alex@folio.app",
                displayName = "Alex Nguyen",
            ),
        )
    }

    private fun createViewModel(): AccountViewModel {
        runBlocking { authRepository.signIn("alex@folio.app", "password") }
        return AccountViewModel(
            getCurrentSessionUseCase = GetCurrentSessionUseCase(authRepository),
        )
    }

    @Test
    fun `init loads session profile`() {
        val viewModel = createViewModel()

        assertEquals("Alex Nguyen", viewModel.uiState.value.displayName)
        assertEquals("alex@folio.app", viewModel.uiState.value.email)
    }

    @Test
    fun `onProfileSettingsClick sets user message`() {
        val viewModel = createViewModel()

        viewModel.onProfileSettingsClick()

        assertEquals(
            AccountUserMessage.PROFILE_SETTINGS_NOT_SUPPORTED,
            viewModel.uiState.value.userMessage,
        )
    }

    @Test
    fun `onUserMessageShown clears user message`() {
        val viewModel = createViewModel()
        viewModel.onSecurityClick()

        viewModel.onUserMessageShown()

        assertNull(viewModel.uiState.value.userMessage)
    }
}
