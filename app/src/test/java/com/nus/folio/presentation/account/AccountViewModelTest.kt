package com.nus.folio.presentation.account

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.usecase.GetCurrentSessionUseCase
import com.nus.folio.domain.usecase.SyncCurrentUserUseCase
import com.nus.folio.testing.FakeAuthRepository
import com.nus.folio.testing.MainDispatcherRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository().apply {
        signInResult = Result.success(
            AuthSession(
                email = "jordan@folio.app",
                displayName = "Jordan Lee",
                userId = "user-1",
            ),
        )
    }

    private fun createViewModel(): AccountViewModel {
        runBlocking { authRepository.signIn("jordan@folio.app", "password") }
        return AccountViewModel(
            getCurrentSessionUseCase = GetCurrentSessionUseCase(authRepository),
            syncCurrentUserUseCase = SyncCurrentUserUseCase(authRepository),
        )
    }

    @Test
    fun `init loads session profile`() {
        val viewModel = createViewModel()

        assertEquals("Jordan Lee", viewModel.uiState.value.displayName)
        assertEquals("jordan@folio.app", viewModel.uiState.value.email)
        assertEquals(1, authRepository.syncCurrentUserCallCount)
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
