package com.loa.momclaw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.loa.momclaw.ui.chat.ChatEvent
import com.loa.momclaw.ui.chat.ChatScreen
import com.loa.momclaw.ui.chat.ChatViewModel
import com.loa.momclaw.ui.models.ModelsEvent
import com.loa.momclaw.ui.models.ModelsScreen
import com.loa.momclaw.ui.models.ModelsViewModel
import com.loa.momclaw.ui.settings.SettingsEvent
import com.loa.momclaw.ui.settings.SettingsScreen
import com.loa.momclaw.ui.settings.SettingsViewModel
import com.loa.momclaw.ui.theme.MomClawTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Navigation item definition.
 */
sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val title: String
) {
    object Chat : BottomNavItem(
        route = "chat",
        icon = Icons.Default.Chat,
        title = "Chat"
    )

    object Models : BottomNavItem(
        route = "models",
        icon = Icons.Default.ModelTraining,
        title = "Models"
    )

    object Settings : BottomNavItem(
        route = "settings",
        icon = Icons.Default.Settings,
        title = "Settings"
    )
}

/**
 * Main Activity for the MomClaw application.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MomClawTheme {
                val navController = rememberNavController()
                
                Scaffold(
                    bottomBar = {
                        BottomNavigationBar(navController)
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = BottomNavItem.Chat.route,
                        modifier = Modifier.padding(padding)
                    ) {
                        composable(BottomNavItem.Chat.route) {
                            val viewModel: ChatViewModel = hiltViewModel()
                            ChatScreen(
                                state = viewModel.state.collectAsState().value,
                                onEvent = viewModel::onEvent,
                                onNavigateBack = { /* Handle back navigation */ }
                            )
                        }

                        composable(BottomNavItem.Models.route) {
                            val viewModel: ModelsViewModel = hiltViewModel()
                            ModelsScreen(
                                state = viewModel.state.collectAsState().value,
                                onEvent = viewModel::onEvent,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(BottomNavItem.Settings.route) {
                            val viewModel: SettingsViewModel = hiltViewModel()
                            SettingsScreen(
                                state = viewModel.state.collectAsState().value,
                                onEvent = viewModel::onEvent,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom navigation bar component.
 */
@Composable
fun BottomNavigationBar(
    navController: NavHostController
) {
    val items = listOf(
        BottomNavItem.Chat,
        BottomNavItem.Models,
        BottomNavItem.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
