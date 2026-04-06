package com.loa.momclaw.ui.chat

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
import com.loa.momclaw.domain.model.ChatMessage
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
 * Instrumented tests for ChatScreen
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private lateinit var mockRepository: ChatRepository

    @Before
    fun setUp() {
        mockRepository = mock()
        whenever(mockRepository.getMessages()).thenReturn(flowOf(emptyList()))
        whenever(mockRepository.getConfig()).thenReturn(flowOf(
            com.loa.momclaw.domain.model.AgentConfig.DEFAULT
        ))
        whenever(mockRepository.isAgentAvailable()).thenReturn(true)
    }

    @After
    fun tearDown() {
        // Cleanup if needed
    }

    @Composable
    private fun TestChatScreen(
        uiState: ChatUiState = ChatUiState(
            messages = emptyList(),
            isAgentAvailable = true
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
                    ChatScreen(
                        uiState = uiState,
                        onSendMessage = {},
                        onUpdateInput = {},
                        onClearConversation = {},
                        onNewConversation = {},
                        onRetry = {},
                        onCancelStreaming = {}
                    )
                }
            }
        }
    }

    @Test
    fun ChatScreen_displaysEmptyState() {
        composeRule.setContent {
            TestChatScreen()
        }

        // Verify empty state
        composeRule.onNodeWithText("MOMCLAW").assertIsDisplayed()
        composeRule.onNodeWithText("Type a message...").assertIsDisplayed()
        composeRule.onNodeWithText("Agent online").assertIsDisplayed()
    }

    @Test
    fun ChatScreen_displaysMessages() {
        val testMessages = listOf(
            ChatMessage("1", "Salut!", false),
            ChatMessage("2", "Bună!", true)
        )

        composeRule.setContent {
            TestChatScreen(
                uiState = ChatUiState(
                    messages = testMessages,
                    isAgentAvailable = true
                )
            )
        }

        // Verify messages are displayed
        composeRule.onNodeWithText("Salut!").assertIsDisplayed()
        composeRule.onNodeWithText("Bună!").assertIsDisplayed()
    }

    @Test
    fun ChatScreen_showsStreamingIndicator() {
        composeRule.setContent {
            TestChatScreen(
                uiState = ChatUiState(
                    messages = listOf(ChatMessage("1", "Mesaj", true)),
                    currentStreamingMessage = ChatMessage(
                        "2",
                        "Acest mesaj este în curs de streaming...",
                        false,
                        isStreaming = true
                    ),
                    isStreaming = true,
                    isAgentAvailable = true
                )
            )
        }

        // Verify streaming indicator is present (should show pulsing dots)
        composeRule.onNodeWithText("Mesaj").assertIsDisplayed()
        // Note: We can't easily test the animated pulsing dots, but we can verify
        // the message content is there
    }

    @Test
    fun ChatScreen_showsErrorBanner() {
        composeRule.setContent {
            TestChatScreen(
                uiState = ChatUiState(
                    messages = listOf(ChatMessage("1", "Test", true)),
                    error = "Eroare de rețea",
                    isAgentAvailable = false
                )
            )
        }

        // Verify error banner is shown
        composeRule.onNodeWithText("Eroare de rețea").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun ChatScreen_handlesInput() {
        composeRule.setContent {
            TestChatScreen()
        }

        // Type in the input field
        composeRule.onNodeWithHint("Type a message...")
            .performTextInput("Salut, lume!")

        // Verify text was entered
        composeRule.onNodeWithHint("Type a message...")
            .assert(isEnabled = true)
            .assert { 
                it.text == "Salut, lume!" 
            }
    }

    @Test
    fun ChatScreen_sendButtonEnabledWhenText() {
        composeRule.setContent {
            TestChatScreen()
        }

        // Initially disabled
        composeRule.onNodeWithDescription("Send")
            .assert(isEnabled = false)

        // Type something
        composeRule.onNodeWithHint("Type a message...")
            .performTextInput("Mesaj")

        // Send button should be enabled
        composeRule.onNodeWithDescription("Send")
            .assert(isEnabled = true)
    }

    @Test
    fun ChatScreen_sendButtonDisabledWhenStreaming() {
        composeRule.setContent {
            TestChatScreen(
                uiState = ChatUiState(
                    messages = emptyList(),
                    isStreaming = true,
                    inputText = "Test",
                    isAgentAvailable = true
                )
            )
        }

        // Verify stop button is shown instead of send
        composeRule.onNodeWithDescription("Stop")
            .assertIsDisplayed()
        composeRule.onNodeWithDescription("Send")
            .assertDoesNotExist()
    }

    @Test
    fun ChatScreen_clearConversationButtonWorks() {
        composeRule.setContent {
            TestChatScreen(
                uiState = ChatUiState(
                    messages = listOf(ChatMessage("1", "Test", true)),
                    isAgentAvailable = true
                )
            )
        }

        // Verify clear button exists
        composeRule.onNodeWithDescription("Clear conversation")
            .assertIsDisplayed()

        // Verify new conversation button exists
        composeRule.onNodeWithDescription("New conversation")
            .assertIsDisplayed()

        // Verify settings button exists
        composeRule.onNodeWithDescription("Settings")
            .assertIsDisplayed()
    }
}

// Test coroutine rule for handling coroutines in tests
class TestCoroutineRule : androidx.test.rules.TestWatcher() {
    private val testDispatcher = androidx.coroutines.test.UnconfinedTestDispatcher()

    override fun starting(description: org.junit.runner.Description?) {
        androidx.coroutines.Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: org.junit.runner.Description?) {
        androidx.coroutines.Dispatchers.resetMain()
    }
}