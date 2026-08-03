package com.nus.folio.presentation.signup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nus.folio.R
import com.nus.folio.ui.theme.FolioAndroidTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignUpScreenAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadingState_disablesFormControlsAndExposesProgressSemantics() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val loadingLabel = context.getString(R.string.signup_loading)
        val createAccountLabel = context.getString(R.string.signup_create_account)
        val appleLabel = context.getString(R.string.signup_continue_with_apple)

        composeTestRule.setContent {
            FolioAndroidTheme(dynamicColor = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SignUpContent(
                        uiState = SignUpUiState(isLoading = true),
                        onClearError = {},
                        onTogglePasswordVisibility = {},
                        onSignUpClick = { _, _, _ -> },
                        onContinueWithAppleClick = {},
                        onSignInClick = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                    SignUpLoadingScreen(modifier = Modifier.fillMaxSize())
                }
            }
        }

        composeTestRule
            .onNodeWithContentDescription(loadingLabel)
            .assertExists()
        composeTestRule
            .onNode(hasIndeterminateProgressBar())
            .assertExists()
        composeTestRule
            .onNodeWithText(createAccountLabel)
            .assertIsNotEnabled()
        composeTestRule
            .onNodeWithText(appleLabel)
            .assertIsNotEnabled()
        composeTestRule
            .onAllNodes(hasSetTextAction() and isNotEnabled())
            .fetchSemanticsNodes()
            .let { nodes ->
                assertTrue(
                    "Expected disabled text fields while loading, found ${nodes.size}",
                    nodes.size >= 3,
                )
            }
    }

    private fun hasIndeterminateProgressBar(): SemanticsMatcher =
        SemanticsMatcher("is indeterminate progress bar") { node ->
            node.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo) ==
                ProgressBarRangeInfo.Indeterminate
        }
}
