package com.nus.folio.presentation.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nus.folio.di.LocalAppContainer
import com.nus.folio.presentation.account.AccountSettingsScreen
import com.nus.folio.presentation.home.HomeScreen
import com.nus.folio.presentation.login.LoginScreen
import com.nus.folio.presentation.resetpassword.ResetPasswordScreen
import com.nus.folio.presentation.signup.SignUpScreen
import com.nus.folio.presentation.space.SpaceScreen

object FolioDestination {
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val RESET_PASSWORD = "reset_password"
    const val SPACES = "spaces"
    const val ACCOUNT = "account"
    const val HOME = "home"

    fun resetPassword(email: String = ""): String =
        "$RESET_PASSWORD?email=${Uri.encode(email)}"

    fun home(spaceId: String, spaceTitle: String = ""): String =
        "$HOME/${Uri.encode(spaceId)}?title=${Uri.encode(spaceTitle)}"
}

@Composable
fun FolioNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    // Always LOGIN until auth session persistence is implemented.
    NavHost(
        navController = navController,
        startDestination = FolioDestination.LOGIN,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(FolioDestination.LOGIN) {
            LoginScreen(
                onNavigateToSpaces = {
                    navController.navigate(FolioDestination.SPACES) {
                        popUpTo(FolioDestination.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(FolioDestination.SIGN_UP)
                },
                onNavigateToResetPassword = { email ->
                    navController.navigate(FolioDestination.resetPassword(email))
                },
            )
        }
        composable(FolioDestination.SIGN_UP) {
            val isAuthAvailable = LocalAppContainer.current.isAuthAvailable
            LaunchedEffect(isAuthAvailable) {
                if (!isAuthAvailable) navController.popBackStack()
            }
            SignUpScreen(
                onNavigateToSpaces = {
                    navController.navigate(FolioDestination.SPACES) {
                        popUpTo(FolioDestination.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
            )
        }
        composable(
            route = "${FolioDestination.RESET_PASSWORD}?email={email}",
            arguments = listOf(
                navArgument("email") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            ResetPasswordScreen(
                initialEmail = entry.arguments?.getString("email").orEmpty(),
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(FolioDestination.SPACES) {
            SpaceScreen(
                onSpaceSelected = { space ->
                    navController.navigate(
                        FolioDestination.home(spaceId = space.id, spaceTitle = space.title),
                    )
                },
                onNavigateToAccount = {
                    navController.navigate(FolioDestination.ACCOUNT)
                },
            )
        }
        composable(FolioDestination.ACCOUNT) {
            val container = LocalAppContainer.current
            val session = container.getCurrentSessionUseCase()
            AccountSettingsScreen(
                displayName = session?.displayName.orEmpty(),
                email = session?.email.orEmpty(),
                onBackClick = { navController.popBackStack() },
                onSignOut = {
                    container.clearAuthSessionUseCase()
                    navController.navigate(FolioDestination.LOGIN) {
                        popUpTo(FolioDestination.SPACES) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = "${FolioDestination.HOME}/{spaceId}?title={title}",
            arguments = listOf(
                navArgument("spaceId") { type = NavType.StringType },
                navArgument("title") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            HomeScreen(
                spaceId = entry.arguments?.getString("spaceId").orEmpty(),
                spaceTitle = entry.arguments?.getString("title").orEmpty(),
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(FolioDestination.LOGIN) {
                        popUpTo(FolioDestination.SPACES) { inclusive = true }
                    }
                },
            )
        }
    }
}
