package com.nus.folio.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nus.folio.presentation.home.HomeScreen
import com.nus.folio.presentation.login.LoginScreen

object FolioDestination {
    const val LOGIN = "login"
    const val HOME = "home"
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
                )
            }
            composable(FolioDestination.HOME) {
                HomeScreen()
            }
    }
}
