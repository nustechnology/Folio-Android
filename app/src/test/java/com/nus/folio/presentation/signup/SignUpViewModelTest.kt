package com.nus.folio.presentation.signup

import com.nus.folio.domain.model.AuthApiException
import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.usecase.SignUpUseCase
import com.nus.folio.testing.FakeAuthRepository
import com.nus.folio.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class SignUpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()

    private fun createViewModel(isAuthAvailable: Boolean = true): SignUpViewModel = SignUpViewModel(
        signUpUseCase = SignUpUseCase(repository),
        isAuthAvailable = isAuthAvailable,
    )

    @Test
    fun `onSignUpClick with blank fields sets all field errors`() {
        val viewModel = createViewModel()

        viewModel.onSignUpClick(
            name = " ",
            email = "",
            password = " ",
            confirmPassword = "",
        )

        assertEquals(SignUpError.NAME_REQUIRED, viewModel.uiState.value.nameError)
        assertEquals(SignUpError.EMAIL_REQUIRED, viewModel.uiState.value.emailError)
        assertEquals(SignUpError.PASSWORD_REQUIRED, viewModel.uiState.value.passwordError)
        assertEquals(
            SignUpError.CONFIRM_PASSWORD_REQUIRED,
            viewModel.uiState.value.confirmPasswordError,
        )
        assertNull(viewModel.uiState.value.formError)
        assertEquals(0, repository.signUpCallCount)
    }

    @Test
    fun `onSignUpClick with blank email sets EMAIL_REQUIRED`() {
        val viewModel = createViewModel()

        viewModel.onSignUpClick(
            name = "Jordan Lee",
            email = "",
            password = "secret",
            confirmPassword = "secret",
        )

        assertNull(viewModel.uiState.value.nameError)
        assertEquals(SignUpError.EMAIL_REQUIRED, viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.passwordError)
        assertNull(viewModel.uiState.value.confirmPasswordError)
        assertEquals(0, repository.signUpCallCount)
    }

    @Test
    fun `onSignUpClick with invalid email sets EMAIL_INVALID`() {
        val viewModel = createViewModel()

        viewModel.onSignUpClick(
            name = "Jordan Lee",
            email = "not-an-email",
            password = "secret",
            confirmPassword = "secret",
        )

        assertEquals(SignUpError.EMAIL_INVALID, viewModel.uiState.value.emailError)
        assertEquals(0, repository.signUpCallCount)
    }

    @Test
    fun `onSignUpClick with blank password sets PASSWORD_REQUIRED`() {
        val viewModel = createViewModel()

        viewModel.onSignUpClick(
            name = "Jordan Lee",
            email = "a@folio.app",
            password = " ",
            confirmPassword = "secret",
        )

        assertEquals(SignUpError.PASSWORD_REQUIRED, viewModel.uiState.value.passwordError)
        assertEquals(0, repository.signUpCallCount)
    }

    @Test
    fun `onSignUpClick with short password sets PASSWORD_TOO_SHORT`() {
        val viewModel = createViewModel()

        viewModel.onSignUpClick(
            name = "Jordan Lee",
            email = "a@folio.app",
            password = "abc",
            confirmPassword = "abc",
        )

        assertEquals(SignUpError.PASSWORD_TOO_SHORT, viewModel.uiState.value.passwordError)
        assertEquals(0, repository.signUpCallCount)
    }

    @Test
    fun `onSignUpClick with blank confirm password sets CONFIRM_PASSWORD_REQUIRED`() {
        val viewModel = createViewModel()

        viewModel.onSignUpClick(
            name = "Jordan Lee",
            email = "a@folio.app",
            password = "secret",
            confirmPassword = " ",
        )

        assertEquals(
            SignUpError.CONFIRM_PASSWORD_REQUIRED,
            viewModel.uiState.value.confirmPasswordError,
        )
        assertEquals(0, repository.signUpCallCount)
    }

    @Test
    fun `onSignUpClick with mismatched passwords sets PASSWORDS_DO_NOT_MATCH`() {
        val viewModel = createViewModel()

        viewModel.onSignUpClick(
            name = "Jordan Lee",
            email = "a@folio.app",
            password = "secret",
            confirmPassword = "other",
        )

        assertEquals(
            SignUpError.PASSWORDS_DO_NOT_MATCH,
            viewModel.uiState.value.confirmPasswordError,
        )
        assertEquals(0, repository.signUpCallCount)
    }

    @Test
    fun `onSignUpClick success navigates home`() {
        repository.signUpResult = Result.success(AuthSession("a@folio.app"))
        val viewModel = createViewModel()

        viewModel.onSignUpClick("Jordan Lee", "a@folio.app", "secret", "secret")

        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.shouldNavigateToHome)
        assertNull(viewModel.uiState.value.formError)
        assertEquals("Jordan Lee", repository.lastSignUpName)
        assertEquals("secret", repository.lastSignUpConfirmPassword)
    }

    @Test
    fun `onSignUpClick existing email sets EMAIL_ALREADY_EXISTS toastError`() {
        repository.signUpResult = Result.failure(AuthApiException("Email already registered"))
        val viewModel = createViewModel()

        viewModel.onSignUpClick("Jordan Lee", "a@folio.app", "secret", "secret")

        assertEquals(SignUpError.EMAIL_ALREADY_EXISTS, viewModel.uiState.value.toastError)
        assertNull(viewModel.uiState.value.toastMessage)
        assertNull(viewModel.uiState.value.formError)
        assertFalse(viewModel.uiState.value.shouldNavigateToHome)
    }

    @Test
    fun `onSignUpClick conflict status sets EMAIL_ALREADY_EXISTS toastError`() {
        repository.signUpResult = Result.failure(
            AuthApiException(message = "Conflict", statusCode = 409),
        )
        val viewModel = createViewModel()

        viewModel.onSignUpClick("Jordan Lee", "a@folio.app", "secret", "secret")

        assertEquals(SignUpError.EMAIL_ALREADY_EXISTS, viewModel.uiState.value.toastError)
        assertFalse(viewModel.uiState.value.shouldNavigateToHome)
    }

    @Test
    fun `onSignUpClick failure with other api message sets toastMessage`() {
        repository.signUpResult = Result.failure(AuthApiException("Rate limited"))
        val viewModel = createViewModel()

        viewModel.onSignUpClick("Jordan Lee", "a@folio.app", "secret", "secret")

        assertEquals("Rate limited", viewModel.uiState.value.toastMessage)
        assertNull(viewModel.uiState.value.toastError)
        assertNull(viewModel.uiState.value.formError)
        assertFalse(viewModel.uiState.value.shouldNavigateToHome)
    }

    @Test
    fun `onSignUpClick failure without message sets SIGN_UP_FAILED`() {
        repository.signUpResult = Result.failure(AuthApiException(""))
        val viewModel = createViewModel()

        viewModel.onSignUpClick("Jordan Lee", "a@folio.app", "secret", "secret")

        assertEquals(SignUpError.SIGN_UP_FAILED, viewModel.uiState.value.formError)
        assertNull(viewModel.uiState.value.toastMessage)
        assertNull(viewModel.uiState.value.toastError)
        assertFalse(viewModel.uiState.value.shouldNavigateToHome)
    }

    @Test
    fun `onSignUpClick missing field IOException maps to SIGN_UP_FAILED`() {
        repository.signUpResult =
            Result.failure(IOException("Sign-up failed: missing access token"))
        val viewModel = createViewModel()

        viewModel.onSignUpClick("Jordan Lee", "a@folio.app", "secret", "secret")

        assertEquals(SignUpError.SIGN_UP_FAILED, viewModel.uiState.value.formError)
        assertNull(viewModel.uiState.value.toastMessage)
    }

    @Test
    fun `onSignUpClick non-server IOException message maps to SIGN_UP_FAILED`() {
        repository.signUpResult = Result.failure(IOException("Broken pipe"))
        val viewModel = createViewModel()

        viewModel.onSignUpClick("Jordan Lee", "a@folio.app", "secret", "secret")

        assertEquals(SignUpError.SIGN_UP_FAILED, viewModel.uiState.value.formError)
        assertNull(viewModel.uiState.value.toastMessage)
    }

    @Test
    fun `onSignUpClick illegal argument does not toast raw developer message`() {
        repository.signUpResult =
            Result.failure(IllegalArgumentException("Passwords do not match"))
        val viewModel = createViewModel()

        viewModel.onSignUpClick("Jordan Lee", "a@folio.app", "secret", "secret")

        assertEquals(SignUpError.SIGN_UP_FAILED, viewModel.uiState.value.formError)
        assertNull(viewModel.uiState.value.toastMessage)
    }

    @Test
    fun `onSignUpClick unknown host maps to NETWORK_ERROR`() {
        repository.signUpResult =
            Result.failure(java.net.UnknownHostException("folio.nustechnology.com"))
        val viewModel = createViewModel()

        viewModel.onSignUpClick("Jordan Lee", "a@folio.app", "secret", "secret")

        assertEquals(SignUpError.NETWORK_ERROR, viewModel.uiState.value.formError)
        assertNull(viewModel.uiState.value.toastMessage)
    }

    @Test
    fun `onSignUpClick socket timeout maps to NETWORK_ERROR`() {
        repository.signUpResult = Result.failure(java.net.SocketTimeoutException("timeout"))
        val viewModel = createViewModel()

        viewModel.onSignUpClick("Jordan Lee", "a@folio.app", "secret", "secret")

        assertEquals(SignUpError.NETWORK_ERROR, viewModel.uiState.value.formError)
        assertNull(viewModel.uiState.value.toastMessage)
    }

    @Test
    fun `onSignUpClick socket exception maps to NETWORK_ERROR`() {
        repository.signUpResult = Result.failure(java.net.SocketException("Connection reset"))
        val viewModel = createViewModel()

        viewModel.onSignUpClick("Jordan Lee", "a@folio.app", "secret", "secret")

        assertEquals(SignUpError.NETWORK_ERROR, viewModel.uiState.value.formError)
        assertNull(viewModel.uiState.value.toastMessage)
    }

    @Test
    fun `onToastMessageShown clears toastMessage and toastError`() {
        repository.signUpResult = Result.failure(AuthApiException("Email already registered"))
        val viewModel = createViewModel()
        viewModel.onSignUpClick("Jordan Lee", "a@folio.app", "secret", "secret")

        viewModel.onToastMessageShown()

        assertNull(viewModel.uiState.value.toastMessage)
        assertNull(viewModel.uiState.value.toastError)
    }

    @Test
    fun `onTogglePasswordVisibility toggles flag`() {
        val viewModel = createViewModel()

        viewModel.onTogglePasswordVisibility()

        assertTrue(viewModel.uiState.value.passwordVisible)
    }

    @Test
    fun `onToggleConfirmPasswordVisibility toggles flag`() {
        val viewModel = createViewModel()

        viewModel.onToggleConfirmPasswordVisibility()

        assertTrue(viewModel.uiState.value.confirmPasswordVisible)
    }

    @Test
    fun `clearNameError clears name field error`() {
        val viewModel = createViewModel()
        viewModel.onSignUpClick("", "a@folio.app", "secret", "secret")

        viewModel.clearNameError()

        assertNull(viewModel.uiState.value.nameError)
    }

    @Test
    fun `authUnavailable is set when isAuthAvailable is false`() {
        val viewModel = createViewModel(isAuthAvailable = false)

        assertTrue(viewModel.uiState.value.authUnavailable)
        assertNull(viewModel.uiState.value.formError)
    }

    @Test
    fun `onNavigationHandled clears navigation flag and loading`() {
        repository.signUpResult = Result.success(AuthSession("a@folio.app"))
        val viewModel = createViewModel()
        viewModel.onSignUpClick("Jordan Lee", "a@folio.app", "secret", "secret")

        viewModel.onNavigationHandled()

        assertFalse(viewModel.uiState.value.shouldNavigateToHome)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
