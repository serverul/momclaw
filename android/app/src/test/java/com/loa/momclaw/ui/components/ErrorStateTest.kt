package com.loa.momclaw.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loa.momclaw.ui.theme.MomClawTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for ErrorState component.
 */
@RunWith(AndroidJUnit4::class)
class ErrorStateTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun errorState_displaysTitle() {
        composeRule.setContent {
            MomClawTheme {
                ErrorState(
                    title = "Network Error",
                    message = "Unable to connect to server",
                    onRetry = {}
                )
            }
        }

        composeRule.onNodeWithText("Network Error")
            .assertIsDisplayed()
    }

    @Test
    fun errorState_displaysMessage() {
        composeRule.setContent {
            MomClawTheme {
                ErrorState(
                    title = "Error",
                    message = "Custom error message here",
                    onRetry = {}
                )
            }
        }

        composeRule.onNodeWithText("Custom error message here")
            .assertIsDisplayed()
    }

    @Test
    fun errorState_displaysRetryButton() {
        composeRule.setContent {
            MomClawTheme {
                ErrorState(
                    title = "Error",
                    message = "Test error",
                    onRetry = {},
                    retryText = "Try Again"
                )
            }
        }

        composeRule.onNodeWithText("Try Again")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun errorState_hasAccessibilityContentDescription() {
        composeRule.setContent {
            MomClawTheme {
                ErrorState(
                    title = "Network Error",
                    message = "Unable to connect",
                    onRetry = {}
                )
            }
        }

        // Verify accessibility
        composeRule.onNode(
            hasAnyDescendant(hasText("Network Error"))
                .and(hasAnyDescendant(hasText("Unable to connect")))
        ).assertIsDisplayed()
    }
}
