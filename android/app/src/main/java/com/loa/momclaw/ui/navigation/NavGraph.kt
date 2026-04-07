package com.loa.momclaw.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.loa.momclaw.ui.chat.ChatScreen
import com.loa.momclaw.ui.models.ModelsScreen
import com.loa.momclaw.ui.settings.SettingsScreen

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
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            // Home screen would go here - for now, redirect to Chat
            // In a full implementation, this would show recent conversations
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            ) {
                androidx.compose.material3.Text("Home Screen - Coming Soon")
            }
        }

        composable(Screen.Chat.route) {
            // Chat screen will be injected via Hilt in the Activity
            // This is a placeholder
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            ) {
                androidx.compose.material3.Text("Chat Screen")
            }
        }

        composable(Screen.Models.route) {
            // Models screen will be injected via Hilt in the Activity
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            ) {
                androidx.compose.material3.Text("Models Screen")
            }
        }

        composable(Screen.Settings.route) {
            // Settings screen will be injected via Hilt in the Activity
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            ) {
                androidx.compose.material3.Text("Settings Screen")
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
