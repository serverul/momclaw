package com.loa.momclaw.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
    navController: NavHostController = rememberNavController(),
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.COMPACT
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Determine if we should use NavigationRail (for larger screens)
    val useNavigationRail = widthSizeClass != WindowWidthSizeClass.COMPACT

    Row(modifier = Modifier.fillMaxSize()) {
        // Navigation Rail for large screens
        if (useNavigationRail) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                header = {
                    // App logo/brand
                    NavigationRailItem(
                        selected = false,
                        onClick = { /* Logo click - no action */ },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "MOMCLAW",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        label = null
                    )
                }
            ) {
                screens.forEach { screen ->
                    NavigationRailItem(
                        icon = screen.icon,
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }

        // Main content
        Scaffold(
            modifier = Modifier.weight(1f),
            bottomBar = {
                // Navigation Bar only for compact screens
                if (!useNavigationRail) {
                    NavigationBar {
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
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Chat.route,
                modifier = Modifier.padding(paddingValues),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn()
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeOut()
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn()
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeOut()
                }
            ) {
                composable(Screen.Chat.route) {
                    val viewModel: ChatViewModel = hiltViewModel()
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    
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
                        onCancelStreaming = { viewModel.cancelStreaming() },
                        useNavigationRail = useNavigationRail
                    )
                }
                
                composable(Screen.Models.route) {
                    val viewModel: ModelsViewModel = hiltViewModel()
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    
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
                        },
                        useNavigationRail = useNavigationRail
                    )
                }
                
                composable(Screen.Settings.route) {
                    val viewModel: SettingsViewModel = hiltViewModel()
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    
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
                        onSave = { viewModel.saveChanges() },
                        useNavigationRail = useNavigationRail
                    )
                }
            }
        }
    }
}

// Extension to get dp modifier from size class
@Composable
fun Modifier.widthForSizeClass(
    compact: androidx.compose.ui.unit.Dp,
    medium: androidx.compose.ui.unit.Dp,
    expanded: androidx.compose.ui.unit.Dp,
    sizeClass: WindowWidthSizeClass
): Modifier {
    val width = when (sizeClass) {
        WindowWidthSizeClass.COMPACT -> compact
        WindowWidthSizeClass.MEDIUM -> medium
        else -> expanded
    }
    return this.widthIn(max = width)
}
