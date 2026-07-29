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
import com.nus.folio.presentation.home.HomeScreen
import com.nus.folio.presentation.login.LoginScreen
import com.nus.folio.presentation.resetpassword.ResetPasswordScreen
import com.nus.folio.presentation.signup.SignUpScreen

object FolioDestination {
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val RESET_PASSWORD = "reset_password"
    const val HOME = "home"

    fun resetPassword(email: String = ""): String =
        "$RESET_PASSWORD?email=${Uri.encode(email)}"
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
                onNavigateToHome = {
                    navController.navigate(FolioDestination.HOME) {
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
                onNavigateToHome = {
                    navController.navigate(FolioDestination.HOME) {
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
        composable(FolioDestination.HOME) {
            HomeScreen(
                onSignOut = {
                    navController.navigate(FolioDestination.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
