package com.nus.folio.presentation.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
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
import com.nus.folio.presentation.home.HomeScreen
import com.nus.folio.presentation.home.HomeTab
import com.nus.folio.presentation.login.LoginScreen
import com.nus.folio.presentation.resetpassword.ResetPasswordScreen
import com.nus.folio.presentation.signup.SignUpScreen
import com.nus.folio.presentation.sourcedetail.SourceDetailScreen
import com.nus.folio.presentation.space.SpaceScreen
import kotlin.coroutines.cancellation.CancellationException

object FolioDestination {
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val RESET_PASSWORD = "reset_password"
    const val SPACES = "spaces"
    const val HOME = "home"
    const val SOURCE_DETAIL = "source_detail"
    const val HOME_TAB_RESULT = "home_tab_result"
    const val HOME_ASK_SOURCE_RESULT = "home_ask_source_result"
    const val HOME_REFRESH_SOURCES_RESULT = "home_refresh_sources_result"
    const val HOME_RESEARCH_OBJECTIVE = "home_research_objective"
    const val LOGIN_SIGNED_OUT_RESULT = "login_signed_out_result"

    fun resetPassword(): String = RESET_PASSWORD

    fun home(
        spaceId: String,
        spaceTitle: String = "",
        researchObjective: String = "",
    ): String =
        "$HOME/${Uri.encode(spaceId)}" +
            "?title=${Uri.encode(spaceTitle)}" +
            "&objective=${Uri.encode(researchObjective)}"

    fun sourceDetail(
        spaceId: String,
        sourceId: String,
        highlightText: String? = null,
    ): String {
        val base = "$SOURCE_DETAIL/${Uri.encode(sourceId)}?spaceId=${Uri.encode(spaceId)}"
        return if (highlightText.isNullOrBlank()) {
            base
        } else {
            "$base&highlight=${Uri.encode(highlightText)}"
        }
    }
}

/** Pops only when there is a destination underneath; avoids an empty (blank) NavHost. */
private fun NavHostController.popBackStackOrIgnore(): Boolean =
    previousBackStackEntry != null && popBackStack()

private fun NavHostController.navigateToLoginAfterSignOut() {
    navigate(FolioDestination.LOGIN) {
        popUpTo(FolioDestination.SPACES) { inclusive = true }
    }
    currentBackStackEntry?.savedStateHandle?.set(
        FolioDestination.LOGIN_SIGNED_OUT_RESULT,
        true,
    )
}

private suspend fun NavHostController.signOutAndNavigate(
    clearAuthSession: suspend () -> Result<Unit>,
): Result<Unit> {
    val result = try {
        clearAuthSession()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
    if (result.isSuccess) {
        navigateToLoginAfterSignOut()
    }
    return result
}

@Composable
fun FolioNavHost(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val isSessionRestored by container.isSessionRestored.collectAsStateWithLifecycle()
    if (!isSessionRestored) {
        Box(modifier = modifier.fillMaxSize())
        return
    }

    val navController = rememberNavController()
    val startDestination = if (container.getCurrentSessionUseCase() != null) {
        FolioDestination.SPACES
    } else {
        FolioDestination.LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(FolioDestination.LOGIN) { entry ->
            val showSignedOutToast by entry.savedStateHandle
                .getStateFlow(FolioDestination.LOGIN_SIGNED_OUT_RESULT, false)
                .collectAsStateWithLifecycle()

            LoginScreen(
                showSignedOutToast = showSignedOutToast,
                onSignedOutToastShown = {
                    entry.savedStateHandle[FolioDestination.LOGIN_SIGNED_OUT_RESULT] = false
                },
                onNavigateToSpaces = {
                    navController.navigate(FolioDestination.SPACES) {
                        popUpTo(FolioDestination.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(FolioDestination.SIGN_UP)
                },
                onNavigateToResetPassword = {
                    navController.navigate(FolioDestination.resetPassword())
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
        composable(FolioDestination.RESET_PASSWORD) {
            ResetPasswordScreen(
                onNavigateBack = { navController.popBackStackOrIgnore() },
            )
        }
        composable(FolioDestination.SPACES) {
            val container = LocalAppContainer.current
            SpaceScreen(
                onSpaceSelected = { space ->
                    navController.navigate(
                        FolioDestination.home(
                            spaceId = space.id,
                            spaceTitle = space.title,
                            researchObjective = space.description,
                        ),
                    )
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(FolioDestination.HOME_RESEARCH_OBJECTIVE, space.description)
                },
                onSignOut = {
                    navController.signOutAndNavigate {
                        container.clearAuthSessionUseCase()
                    }
                },
                onRequiresReauth = {
                    navController.navigate(FolioDestination.LOGIN) {
                        popUpTo(FolioDestination.SPACES) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = "${FolioDestination.HOME}/{spaceId}?title={title}&objective={objective}",
            arguments = listOf(
                navArgument("spaceId") { type = NavType.StringType },
                navArgument("title") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("objective") {
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
            val pendingRefreshSources by entry.savedStateHandle
                .getStateFlow(FolioDestination.HOME_REFRESH_SOURCES_RESULT, false)
                .collectAsStateWithLifecycle()

            HomeScreen(
                spaceId = entry.arguments?.getString("spaceId").orEmpty(),
                spaceTitle = entry.arguments?.getString("title").orEmpty(),
                researchObjective = entry.savedStateHandle
                    .get<String>(FolioDestination.HOME_RESEARCH_OBJECTIVE)
                    ?.takeIf { it.isNotBlank() }
                    ?: entry.arguments?.getString("objective").orEmpty(),
                onNavigateBack = {
                    // Double-tapping back must not pop Spaces (post-auth root) and blank the app.
                    if (!navController.popBackStack(FolioDestination.SPACES, inclusive = false)) {
                        navController.popBackStackOrIgnore()
                    }
                },
                onNavigateToSourceDetail = { sourceId, highlightText ->
                    navController.navigate(
                        FolioDestination.sourceDetail(
                            spaceId = entry.arguments?.getString("spaceId").orEmpty(),
                            sourceId = sourceId,
                            highlightText = highlightText,
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
                initialRefreshSources = pendingRefreshSources,
                onInitialRefreshSourcesHandled = {
                    entry.savedStateHandle[FolioDestination.HOME_REFRESH_SOURCES_RESULT] = false
                },
                onSignOut = {
                    navController.navigateToLoginAfterSignOut()
                },
            )
        }
        composable(
            route = "${FolioDestination.SOURCE_DETAIL}/{sourceId}?spaceId={spaceId}&highlight={highlight}",
            arguments = listOf(
                navArgument("sourceId") { type = NavType.StringType },
                navArgument("spaceId") { type = NavType.StringType },
                navArgument("highlight") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                },
            ),
        ) { entry ->
            SourceDetailScreen(
                spaceId = entry.arguments?.getString("spaceId").orEmpty(),
                sourceId = entry.arguments?.getString("sourceId").orEmpty(),
                highlightText = entry.arguments?.getString("highlight").orEmpty()
                    .takeIf { it.isNotBlank() },
                onBackClick = { navController.popBackStackOrIgnore() },
                onSourceDeleted = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(FolioDestination.HOME_REFRESH_SOURCES_RESULT, true)
                    navController.popBackStackOrIgnore()
                },
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
