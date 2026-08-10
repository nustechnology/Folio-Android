package com.nus.folio.presentation.login

import com.nus.folio.domain.model.AuthApiException
import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.usecase.SignInUseCase
import com.nus.folio.testing.FakeAuthRepository
import com.nus.folio.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.net.UnknownHostException

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()

    private fun createViewModel(isAuthAvailable: Boolean = true): LoginViewModel = LoginViewModel(
        signInUseCase = SignInUseCase(repository),
        isAuthAvailable = isAuthAvailable,
        signInLoadingDelayMs = 0L,
    )

    @Test
    fun `onSignInClick with blank email sets EMAIL_REQUIRED`() {
        val viewModel = createViewModel()

        viewModel.onSignInClick(email = " ", password = "secret")

        assertEquals(LoginError.EMAIL_REQUIRED, viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.passwordError)
        assertEquals(0, repository.signInCallCount)
    }

    @Test
    fun `onSignInClick with blank email and password sets both field errors`() {
        val viewModel = createViewModel()

        viewModel.onSignInClick(email = "", password = "")

        assertEquals(LoginError.EMAIL_REQUIRED, viewModel.uiState.value.emailError)
        assertEquals(LoginError.PASSWORD_REQUIRED, viewModel.uiState.value.passwordError)
        assertEquals(0, repository.signInCallCount)
    }

    @Test
    fun `onSignInClick with invalid email sets EMAIL_INVALID`() {
        val viewModel = createViewModel()

        viewModel.onSignInClick(email = "not-an-email", password = "secret")

        assertEquals(LoginError.EMAIL_INVALID, viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.passwordError)
        assertEquals(0, repository.signInCallCount)
    }

    @Test
    fun `onSignInClick with blank password sets PASSWORD_REQUIRED`() {
        val viewModel = createViewModel()

        viewModel.onSignInClick(email = "user@folio.app", password = "")

        assertNull(viewModel.uiState.value.emailError)
        assertEquals(LoginError.PASSWORD_REQUIRED, viewModel.uiState.value.passwordError)
        assertEquals(0, repository.signInCallCount)
    }

    @Test
    fun `onSignInClick success navigates home`() {
        repository.signInResult = Result.success(AuthSession("user@folio.app"))
        val viewModel = createViewModel()

        viewModel.onSignInClick("user@folio.app", "secret")

        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.shouldNavigateToHome)
        assertNull(viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.passwordError)
        assertEquals("user@folio.app", repository.lastSignInEmail)
    }

    @Test
    fun `onSignInClick 401 sets INVALID_CREDENTIALS toastError`() {
        repository.signInResult = Result.failure(
            AuthApiException(message = "Invalid credentials", statusCode = 401),
        )
        val viewModel = createViewModel()

        viewModel.onSignInClick("user@folio.app", "secret")

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(LoginError.INVALID_CREDENTIALS, viewModel.uiState.value.toastError)
        assertNull(viewModel.uiState.value.toastMessage)
        assertNull(viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.passwordError)
        assertFalse(viewModel.uiState.value.shouldNavigateToHome)
    }

    @Test
    fun `onSignInClick 429 toasts API message`() {
        repository.signInResult = Result.failure(
            AuthApiException(message = "Too many requests", statusCode = 429),
        )
        val viewModel = createViewModel()

        viewModel.onSignInClick("user@folio.app", "secret")

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Too many requests", viewModel.uiState.value.toastMessage)
        assertNull(viewModel.uiState.value.toastError)
        assertNull(viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.passwordError)
        assertFalse(viewModel.uiState.value.shouldNavigateToHome)
    }

    @Test
    fun `onSignInClick 5xx toasts API message`() {
        repository.signInResult = Result.failure(
            AuthApiException(message = "Server error", statusCode = 503),
        )
        val viewModel = createViewModel()

        viewModel.onSignInClick("user@folio.app", "secret")

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Server error", viewModel.uiState.value.toastMessage)
        assertNull(viewModel.uiState.value.toastError)
        assertNull(viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.passwordError)
        assertFalse(viewModel.uiState.value.shouldNavigateToHome)
    }

    @Test
    fun `onSignInClick AuthApiException without message sets SIGN_IN_FAILED toastError`() {
        repository.signInResult = Result.failure(
            AuthApiException(message = "  ", statusCode = 500),
        )
        val viewModel = createViewModel()

        viewModel.onSignInClick("user@folio.app", "secret")

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(LoginError.SIGN_IN_FAILED, viewModel.uiState.value.toastError)
        assertNull(viewModel.uiState.value.toastMessage)
        assertNull(viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.passwordError)
        assertFalse(viewModel.uiState.value.shouldNavigateToHome)
    }

    @Test
    fun `onSignInClick failure without message sets INVALID_CREDENTIALS toastError`() {
        repository.signInResult = Result.failure(IllegalStateException())
        val viewModel = createViewModel()

        viewModel.onSignInClick("user@folio.app", "secret")

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(LoginError.INVALID_CREDENTIALS, viewModel.uiState.value.toastError)
        assertNull(viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.passwordError)
        assertNull(viewModel.uiState.value.toastMessage)
        assertFalse(viewModel.uiState.value.shouldNavigateToHome)
    }

    @Test
    fun `onSignInClick transport failure sets SIGN_IN_FAILED toastError`() {
        repository.signInResult = Result.failure(UnknownHostException("offline"))
        val viewModel = createViewModel()

        viewModel.onSignInClick("user@folio.app", "secret")

        assertEquals(LoginError.SIGN_IN_FAILED, viewModel.uiState.value.toastError)
        assertNull(viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.passwordError)
        assertNull(viewModel.uiState.value.toastMessage)
        assertFalse(viewModel.uiState.value.shouldNavigateToHome)
    }

    @Test
    fun `onToastMessageShown clears toastError`() {
        repository.signInResult = Result.failure(
            AuthApiException(message = "Invalid credentials", statusCode = 401),
        )
        val viewModel = createViewModel()
        viewModel.onSignInClick("user@folio.app", "secret")

        viewModel.onToastMessageShown()

        assertNull(viewModel.uiState.value.toastMessage)
        assertNull(viewModel.uiState.value.toastError)
    }

    @Test
    fun `onSignInClick when auth unavailable sets AUTH_UNAVAILABLE`() {
        val viewModel = createViewModel(isAuthAvailable = false)

        viewModel.onSignInClick("user@folio.app", "secret")

        assertEquals(LoginError.AUTH_UNAVAILABLE, viewModel.uiState.value.formError)
        assertEquals(0, repository.signInCallCount)
    }

    @Test
    fun `onTogglePasswordVisibility toggles flag`() {
        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.passwordVisible)
        viewModel.onTogglePasswordVisibility()
        assertTrue(viewModel.uiState.value.passwordVisible)
        viewModel.onTogglePasswordVisibility()
        assertFalse(viewModel.uiState.value.passwordVisible)
    }

    @Test
    fun `clearEmailError clears email field error only`() {
        val viewModel = createViewModel()
        viewModel.onSignInClick("", "")

        viewModel.clearEmailError()

        assertNull(viewModel.uiState.value.emailError)
        assertEquals(LoginError.PASSWORD_REQUIRED, viewModel.uiState.value.passwordError)
    }

    @Test
    fun `clearPasswordError clears password field error only`() {
        val viewModel = createViewModel()
        viewModel.onSignInClick("", "")

        viewModel.clearPasswordError()

        assertEquals(LoginError.EMAIL_REQUIRED, viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.passwordError)
    }

    @Test
    fun `onNavigationHandled clears navigation flag`() {
        repository.signInResult = Result.success(AuthSession("user@folio.app"))
        val viewModel = createViewModel()
        viewModel.onSignInClick("user@folio.app", "secret")

        viewModel.onNavigationHandled()

        assertFalse(viewModel.uiState.value.shouldNavigateToHome)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
