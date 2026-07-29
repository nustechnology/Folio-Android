package com.nus.folio.presentation.signup

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.usecase.SignInWithAppleUseCase
import com.nus.folio.domain.usecase.SignUpUseCase
import com.nus.folio.testing.FakeAuthRepository
import com.nus.folio.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignUpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()

    private fun createViewModel(isAuthAvailable: Boolean = true): SignUpViewModel = SignUpViewModel(
        signUpUseCase = SignUpUseCase(repository),
        signInWithAppleUseCase = SignInWithAppleUseCase(repository),
        isAuthAvailable = isAuthAvailable,
    )

    @Test
    fun `onSignUpClick with blank name sets NAME_REQUIRED`() {
        val viewModel = createViewModel()

        viewModel.onSignUpClick(name = " ", email = "a@folio.app", password = "secret")

        assertEquals(SignUpError.NAME_REQUIRED, viewModel.uiState.value.error)
        assertEquals(0, repository.signUpCallCount)
    }

    @Test
    fun `onSignUpClick with blank email sets EMAIL_REQUIRED`() {
        val viewModel = createViewModel()

        viewModel.onSignUpClick(name = "Alex", email = "", password = "secret")

        assertEquals(SignUpError.EMAIL_REQUIRED, viewModel.uiState.value.error)
        assertEquals(0, repository.signUpCallCount)
    }

    @Test
    fun `onSignUpClick with blank password sets PASSWORD_REQUIRED`() {
        val viewModel = createViewModel()

        viewModel.onSignUpClick(name = "Alex", email = "a@folio.app", password = " ")

        assertEquals(SignUpError.PASSWORD_REQUIRED, viewModel.uiState.value.error)
        assertEquals(0, repository.signUpCallCount)
    }

    @Test
    fun `onSignUpClick success navigates home`() {
        repository.signUpResult = Result.success(AuthSession("a@folio.app"))
        val viewModel = createViewModel()

        viewModel.onSignUpClick("Alex", "a@folio.app", "secret")

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.shouldNavigateToHome)
        assertNull(viewModel.uiState.value.error)
        assertEquals("Alex", repository.lastSignUpName)
    }

    @Test
    fun `onSignUpClick failure sets SIGN_UP_FAILED`() {
        repository.signUpResult = Result.failure(IllegalStateException("exists"))
        val viewModel = createViewModel()

        viewModel.onSignUpClick("Alex", "a@folio.app", "secret")

        assertEquals(SignUpError.SIGN_UP_FAILED, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.shouldNavigateToHome)
    }

    @Test
    fun `onContinueWithAppleClick success navigates home`() {
        repository.signInWithAppleResult = Result.success(AuthSession("apple.user@folio.app"))
        val viewModel = createViewModel()

        viewModel.onContinueWithAppleClick()

        assertTrue(viewModel.uiState.value.shouldNavigateToHome)
    }

    @Test
    fun `onContinueWithAppleClick failure sets SIGN_UP_FAILED`() {
        repository.signInWithAppleResult = Result.failure(IllegalStateException("apple error"))
        val viewModel = createViewModel()

        viewModel.onContinueWithAppleClick()

        assertEquals(SignUpError.SIGN_UP_FAILED, viewModel.uiState.value.error)
    }

    @Test
    fun `onTogglePasswordVisibility toggles flag`() {
        val viewModel = createViewModel()

        viewModel.onTogglePasswordVisibility()

        assertTrue(viewModel.uiState.value.passwordVisible)
    }

    @Test
    fun `clearError clears error`() {
        val viewModel = createViewModel()
        viewModel.onSignUpClick("", "a@folio.app", "secret")

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `authUnavailable is set when isAuthAvailable is false`() {
        val viewModel = createViewModel(isAuthAvailable = false)

        assertTrue(viewModel.uiState.value.authUnavailable)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `onNavigationHandled clears navigation flag`() {
        repository.signUpResult = Result.success(AuthSession("a@folio.app"))
        val viewModel = createViewModel()
        viewModel.onSignUpClick("Alex", "a@folio.app", "secret")

        viewModel.onNavigationHandled()

        assertFalse(viewModel.uiState.value.shouldNavigateToHome)
    }
}
