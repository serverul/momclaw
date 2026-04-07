package com.loa.momclaw.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.loa.momclaw.ui.chat.ChatScreen
import com.loa.momclaw.ui.chat.ChatViewModel
import com.loa.momclaw.ui.screens.ModelsScreen
import com.loa.momclaw.ui.screens.ModelsScreenViewModel
import com.loa.momclaw.ui.settings.SettingsScreen
import com.loa.momclaw.ui.settings.SettingsViewModel

/**
 * Navigation routes for the application.
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Chat : Screen("chat")
    object Models : Screen("models")
    object Settings : Screen("settings")
}

/**
 * Main navigation graph for the application.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Chat.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToChat = { navController.navigateToChat() },
                onNavigateToModels = { navController.navigateToModels() },
                onNavigateToSettings = { navController.navigateToSettings() }
            )
        }

        composable(Screen.Chat.route) {
            val viewModel: ChatViewModel = viewModel()
            val state by viewModel.state.collectAsState()
            
            ChatScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onNavigateBack = { navController.navigateBack() }
            )
        }

        composable(Screen.Models.route) {
            ModelsScreen(
                onNavigateBack = { navController.navigateBack() }
            )
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel()
            val state by viewModel.state.collectAsState()
            
            SettingsScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onNavigateBack = { navController.navigateBack() }
            )
        }
    }
}

/**
 * Home screen with quick access to main features.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToModels: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "MomClaw",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome to MomClaw",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = "Your AI assistant powered by LiteRT-LM",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick action cards
            ElevatedCard(
                onClick = onNavigateToChat,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Start Chat",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Chat with your AI assistant",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = androidx.compose.ui.Modifier.rotate(180f)
                    )
                }
            }
            
            ElevatedCard(
                onClick = onNavigateToModels,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Manage Models",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Download and activate AI models",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            ElevatedCard(
                onClick = onNavigateToSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Configure your assistant",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Extension functions for navigation.
 */
fun NavHostController.navigateToChat() {
    navigate(Screen.Chat.route) {
        launchSingleTop = true
    }
}

fun NavHostController.navigateToModels() {
    navigate(Screen.Models.route) {
        launchSingleTop = true
    }
}

fun NavHostController.navigateToSettings() {
    navigate(Screen.Settings.route) {
        launchSingleTop = true
    }
}

fun NavHostController.navigateBack() {
    popBackStack()
}

// Modifier extension for rotation
private fun Modifier.rotate(degrees: Float): Modifier {
    return androidx.compose.ui.graphics.graphicsLayer { rotationZ = degrees }
}
