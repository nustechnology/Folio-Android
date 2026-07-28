package com.nus.folio.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nus.folio.presentation.home.HomeScreen

object FolioDestination {
    const val HOME = "home"
}

@Composable
fun FolioNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = FolioDestination.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(FolioDestination.HOME) {
                HomeScreen()
            }
        }
    }
}
