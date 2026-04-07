package com.loa.momclaw.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loa.momclaw.ui.theme.MomClawTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for LoadingScreen component.
 */
@RunWith(AndroidJUnit4::class)
class LoadingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingScreen_displaysMessage() {
        composeRule.setContent {
            MomClawTheme {
                LoadingScreen(message = "Loading models...")
            }
        }

        composeRule.onNodeWithText("Loading models...")
            .assertIsDisplayed()
    }

    @Test
    fun loadingScreen_showsProgressIndicator() {
        composeRule.setContent {
            MomClawTheme {
                LoadingScreen(message = "Loading...")
            }
        }

        // Verify progress indicator exists
        composeRule.onNode(hasTestTag("CircularProgressIndicator"))
            .assertIsDisplayed()
    }
}
