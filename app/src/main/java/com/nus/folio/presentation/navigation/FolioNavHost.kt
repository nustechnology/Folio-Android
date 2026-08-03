package com.nus.folio.presentation.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nus.folio.di.LocalAppContainer
import com.nus.folio.presentation.account.AccountSettingsScreen
import com.nus.folio.presentation.home.HomeScreen
import com.nus.folio.presentation.home.HomeTab
import com.nus.folio.presentation.login.LoginScreen
import com.nus.folio.presentation.resetpassword.ResetPasswordScreen
import com.nus.folio.presentation.signup.SignUpScreen
import com.nus.folio.presentation.sourcedetail.SourceDetailScreen
import com.nus.folio.presentation.space.SpaceScreen

object FolioDestination {
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val RESET_PASSWORD = "reset_password"
    const val SPACES = "spaces"
    const val ACCOUNT = "account"
    const val HOME = "home"
    const val SOURCE_DETAIL = "source_detail"
    const val HOME_TAB_RESULT = "home_tab_result"
    const val HOME_ASK_SOURCE_RESULT = "home_ask_source_result"

    fun resetPassword(email: String = ""): String =
        "$RESET_PASSWORD?email=${Uri.encode(email)}"

    fun home(spaceId: String, spaceTitle: String = ""): String =
        "$HOME/${Uri.encode(spaceId)}?title=${Uri.encode(spaceTitle)}"

    fun sourceDetail(spaceId: String, sourceId: String): String =
        "$SOURCE_DETAIL/${Uri.encode(sourceId)}?spaceId=${Uri.encode(spaceId)}"
}

/** Pops only when there is a destination underneath; avoids an empty (blank) NavHost. */
private fun NavHostController.popBackStackOrIgnore(): Boolean =
    previousBackStackEntry != null && popBackStack()

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
                if (!isAuthAvailable) navController.popBackStackOrIgnore()
            }
            SignUpScreen(
                onNavigateToSpaces = {
                    navController.navigate(FolioDestination.SPACES) {
                        popUpTo(FolioDestination.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStackOrIgnore()
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
                onNavigateBack = { navController.popBackStackOrIgnore() },
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
            AccountSettingsScreen(
                onBackClick = {
                    // Prefer Spaces as the post-auth root; never pop it away on a double tap.
                    if (!navController.popBackStack(FolioDestination.SPACES, inclusive = false)) {
                        navController.popBackStackOrIgnore()
                    }
                },
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
            val pendingTab by entry.savedStateHandle
                .getStateFlow<String?>(FolioDestination.HOME_TAB_RESULT, null)
                .collectAsStateWithLifecycle()
            val pendingAskSourceId by entry.savedStateHandle
                .getStateFlow<String?>(FolioDestination.HOME_ASK_SOURCE_RESULT, null)
                .collectAsStateWithLifecycle()

            HomeScreen(
                spaceId = entry.arguments?.getString("spaceId").orEmpty(),
                spaceTitle = entry.arguments?.getString("title").orEmpty(),
                onNavigateBack = {
                    // Double-tapping back must not pop Spaces (post-auth root) and blank the app.
                    if (!navController.popBackStack(FolioDestination.SPACES, inclusive = false)) {
                        navController.popBackStackOrIgnore()
                    }
                },
                onNavigateToSourceDetail = { sourceId ->
                    navController.navigate(
                        FolioDestination.sourceDetail(
                            spaceId = entry.arguments?.getString("spaceId").orEmpty(),
                            sourceId = sourceId,
                        ),
                    )
                },
                initialTab = pendingTab?.let { runCatching { HomeTab.valueOf(it) }.getOrNull() },
                onInitialTabHandled = {
                    entry.savedStateHandle.remove<String>(FolioDestination.HOME_TAB_RESULT)
                },
                initialAskSourceId = pendingAskSourceId,
                onInitialAskSourceHandled = {
                    entry.savedStateHandle.remove<String>(FolioDestination.HOME_ASK_SOURCE_RESULT)
                },
                onSignOut = {
                    navController.navigate(FolioDestination.LOGIN) {
                        popUpTo(FolioDestination.SPACES) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = "${FolioDestination.SOURCE_DETAIL}/{sourceId}?spaceId={spaceId}",
            arguments = listOf(
                navArgument("sourceId") { type = NavType.StringType },
                navArgument("spaceId") { type = NavType.StringType },
            ),
        ) { entry ->
            SourceDetailScreen(
                spaceId = entry.arguments?.getString("spaceId").orEmpty(),
                sourceId = entry.arguments?.getString("sourceId").orEmpty(),
                onBackClick = { navController.popBackStackOrIgnore() },
                onAskSourceClick = {
                    val sourceId = entry.arguments?.getString("sourceId").orEmpty()
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.apply {
                            set(FolioDestination.HOME_TAB_RESULT, HomeTab.ASK.name)
                            set(FolioDestination.HOME_ASK_SOURCE_RESULT, sourceId)
                        }
                    navController.popBackStackOrIgnore()
                },
            )
        }
    }
}
