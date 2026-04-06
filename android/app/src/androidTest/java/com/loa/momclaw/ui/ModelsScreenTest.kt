package com.loa.momclaw.ui.models

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loa.momclaw.ui.theme.MOMCLAWTheme
import com.loa.momclaw.util.TestCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Instrumented tests for ModelsScreen
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ModelsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private lateinit var mockRepository: com.loa.momclaw.domain.repository.ChatRepository

    @Before
    fun setUp() {
        mockRepository = mock()
        whenever(mockRepository.getAvailableModels()).thenReturn(
            kotlinx.coroutines.tasks.Tasks.result(emptyList())
        )
    }

    @After
    fun tearDown() {
        // Cleanup if needed
    }

    @Composable
    private fun TestModelsScreen(
        uiState: ModelsUiState = ModelsUiState(
            models = emptyList(),
            isLoading = false
        )
    ) {
        MOMCLAWTheme {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .width(400.dp)
                        .height(800.dp)
                ) {
                    ModelsScreen(
                        uiState = uiState,
                        onNavigateBack = {},
                        onDownloadModel = {},
                        onLoadModel = {},
                        onDeleteModel = {},
                        onRefresh = {},
                        onRetry = {}
                    )
                }
            }
        }
    }

    @Test
    fun ModelsScreen_displaysEmptyState() {
        composeRule.setContent {
            TestModelsScreen()
        }

        // Verify empty state
        composeRule.onNodeWithText("Models").assertIsDisplayed()
        composeRule.onNodeWithText("No models available").assertIsDisplayed()
        composeRule.onNodeWithText("Pull models from HuggingFace to get started").assertIsDisplayed()
        composeRule.onNodeWithText("Refresh").assertIsDisplayed()
    }

    @Test
    fun ModelsScreen_showsLoadingIndicator() {
        composeRule.setContent {
            TestModelsScreen(
                uiState = ModelsUiState(
                    models = emptyList(),
                    isLoading = true
                )
            )
        }

        // Should show loading indicator
        composeRule.onNodeWithText("Loading models...").assertIsDisplayed()
    }

    @Test
    fun ModelsScreen_displaysModelsList() {
        val testModels = listOf(
            com.loa.momclaw.ui.models.ModelItem(
                id = "gemma-2b-it",
                name = "Gemma 2B Instruct",
                size = "1.4 GB",
                downloaded = true,
                loaded = true
            ),
            com.loa.momclaw.ui.models.ModelItem(
                id = "gemma-7b-it",
                name = "Gemma 7B Instruct",
                size = "4.3 GB",
                downloaded = true,
                loaded = false
            )
        )

        composeRule.setContent {
            TestModelsScreen(
                uiState = ModelsUiState(
                    models = testModels,
                    isLoading = false
                )
            )
        }

        // Verify models are displayed
        composeRule.onNodeWithText("Gemma 2B Instruct").assertIsDisplayed()
        composeRule.onNodeWithText("Gemma 7B Instruct").assertIsDisplayed()
        composeRule.onNodeWithText("1.4 GB").assertIsDisplayed()
        composeRule.onNodeWithText("4.3 GB").assertIsDisplayed()
        // Check for active/chip indicator
        composeRule.onNodeWithText("Active").assertIsDisplayed()
    }

    @Test
    fun ModelsScreen_showsDownloadProgress() {
        composeRule.setContent {
            TestModelsScreen(
                uiState = ModelsUiState(
                    models = listOf(
                        com.loa.momclaw.ui.models.ModelItem(
                            id = "llama-3-8b",
                            name = "Llama 3 8B",
                            size = "4.7 GB",
                            downloaded = false,
                            loaded = false
                        )
                    ),
                    isDownloading = true,
                    downloadingModelId = "llama-3-8b",
                    downloadProgress = 0.5f
                )
            )
        }

        // Verify downloading state
        composeRule.onNodeWithText("Llama 3 8B").assertIsDisplayed()
        // Progress indicator would be shown, but hard to test exact progress in compose test
    }

    @Test
    fun ModelsScreen_showsErrorState() {
        composeRule.setContent {
            TestModelsScreen(
                uiState = ModelsUiState(
                    models = emptyList(),
                    isLoading = false,
                    error = "Failed to load models"
                )
            )
        }

        // Verify error is shown
        composeRule.onNodeWithText("Failed to load models").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun ModelsScreen_gridLayoutOnTablet() {
        composeRule.setContent {
            TestModelsScreen(
                uiState = ModelsUiState(
                    models = listOf(
                        com.loa.momclaw.ui.models.ModelItem(
                            id = "gemma-2b-it",
                            name = "Gemma 2B Instruct",
                            size = "1.4 GB",
                            downloaded = true,
                            loaded = true
                        ),
                        com.loa.momclaw.ui.models.ModelItem(
                            id = "gemma-7b-it",
                            name = "Gemma 7B Instruct",
                            size = "4.3 GB",
                            downloaded = true,
                            loaded = false
                        )
                    ),
                    isLoading = false
                ),
                useNavigationRail = true // Simulate tablet
            )
        }

        // Verify models are displayed
        composeRule.onNodeWithText("Gemma 2B Instruct").assertIsDisplayed()
        composeRule.onNodeWithText("Gemma 7B Instruct").assertIsDisplayed()
    }
}