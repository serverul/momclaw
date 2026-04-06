package com.loa.momclaw.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loa.momclaw.ui.theme.MOMCLAWTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for NavGraph
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class NavGraphTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navGraph_showsBottomNavigationOnPhone() {
        composeRule.setContent {
            MOMCLAWTheme {
                NavGraph(
                    navController = rememberNavController(),
                    widthSizeClass = WindowWidthSizeClass.COMPACT
                )
            }
        }

        // Bottom navigation should show 3 items
        composeRule.onNodeWithText("Chat").assertIsDisplayed()
        composeRule.onNodeWithText("Models").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").performClick()
        
        // After clicking Models, navigation should update
        // Chat tab should still be present
        composeRule.onNodeWithText("Chat").assertExists()
    }

    @Test
    fun navGraph_startsOnChat() {
        composeRule.setContent {
            MOMCLAWTheme {
                NavGraph(
                    navController = rememberNavController(),
                    widthSizeClass = WindowWidthSizeClass.COMPACT
                )
            }
        }

        // Chat screen should be visible by default
        composeRule.onNodeWithText("MOMCLAW").assertIsDisplayed()
        composeRule.onNodeWithText("Type a message...").assertExists()
    }

    @Test
    fun navGraph_navigatesBetweenScreens() {
        composeRule.setContent {
            MOMCLAWTheme {
                NavGraph(
                    navController = rememberNavController(),
                    widthSizeClass = WindowWidthSizeClass.COMPACT
                )
            }
        }

        // Start on Chat
        composeRule.onNodeWithText("MOMCLAW").assertIsDisplayed()

        // Navigate to Models
        composeRule.onNodeWithText("Models").performClick()
        composeRule.onNodeWithText("Models").assertExists()
        composeRule.onNodeWithText("No models available").assertExists()

        // Navigate to Settings
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Agent Configuration").assertExists()

        // Navigate back to Chat
        composeRule.onNodeWithText("Chat").performClick()
        composeRule.onNodeWithText("MOMCLAW").assertIsDisplayed()
    }

    @Test
    fun navGraph_showsNavigationRailOnTablet() {
        composeRule.setContent {
            // Test with tablet size class
            MOMCLAWTheme {
                NavGraph(
                    navController = rememberNavController(),
                    widthSizeClass = WindowWidthSizeClass.MEDIUM
                )
            }
        }

        // Chat should be visible (default first screen)
        composeRule.onNodeWithText("MOMCLAW").assertExists()

        // Navigation rail items should be present
        composeRule.onNodeWithContentDescription("Chat").assertExists()
        composeRule.onNodeWithContentDescription("Models").assertExists()
        composeRule.onNodeWithContentDescription("Settings").assertExists()
    }
}
