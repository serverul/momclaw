package com.loa.momclaw.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.loa.momclaw.ui.chat.ChatScreen
import com.loa.momclaw.ui.chat.ChatViewModel
import com.loa.momclaw.ui.models.ModelsScreen
import com.loa.momclaw.ui.models.ModelsViewModel
import com.loa.momclaw.ui.settings.SettingsScreen
import com.loa.momclaw.ui.settings.SettingsViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val icon: @androidx.compose.runtime.Composable () -> Unit
) {
    object Chat : Screen(
        route = "chat",
        title = "Chat",
        icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") }
    )
    
    object Models : Screen(
        route = "models",
        title = "Models",
        icon = { Icon(Icons.Default.Memory, contentDescription = "Models") }
    )
    
    object Settings : Screen(
        route = "settings",
        title = "Settings",
        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }
    )
}

val screens = listOf(Screen.Chat, Screen.Models, Screen.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = screen.icon,
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Chat.route) {
                val viewModel: ChatViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsState()
                
                ChatScreen(
                    uiState = state,
                    onNavigateBack = {},
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onSendMessage = { viewModel.sendMessage() },
                    onUpdateInput = { viewModel.updateInputText(it) },
                    onClearConversation = { viewModel.clearConversation() },
                    onNewConversation = { viewModel.startNewConversation() },
                    onRetry = { viewModel.retry() },
                    onCancelStreaming = { viewModel.cancelStreaming() }
                )
            }
            
            composable(Screen.Models.route) {
                val viewModel: ModelsViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsState()
                
                ModelsScreen(
                    uiState = state,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onDownloadModel = { modelId ->
                        viewModel.downloadModel(modelId)
                    },
                    onLoadModel = { modelId ->
                        viewModel.loadModel(modelId)
                    },
                    onDeleteModel = { modelId ->
                        viewModel.deleteModel(modelId)
                    },
                    onRefresh = {
                        viewModel.loadModels()
                    },
                    onRetry = {
                        viewModel.retry()
                    }
                )
            }
            
            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsState()
                
                SettingsScreen(
                    uiState = state,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSystemPromptChange = { viewModel.setSystemPrompt(it) },
                    onTemperatureChange = { viewModel.setTemperature(it) },
                    onMaxTokensChange = { viewModel.setMaxTokens(it) },
                    onModelPrimaryChange = { viewModel.setModelPrimary(it) },
                    onBaseUrlChange = { viewModel.setBaseUrl(it) },
                    onDarkThemeChange = { viewModel.setDarkTheme(it) },
                    onStreamingEnabledChange = { viewModel.setStreamingEnabled(it) },
                    onNotificationsEnabledChange = { viewModel.setNotificationsEnabled(it) },
                    onBackgroundAgentChange = { viewModel.setBackgroundAgentEnabled(it) },
                    onResetToDefaults = { viewModel.resetToDefaults() },
                    onSave = { viewModel.saveChanges() }
                )
            }
        }
    }
}
