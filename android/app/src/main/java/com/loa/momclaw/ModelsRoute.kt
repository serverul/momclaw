package com.loa.momclaw

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.loa.momclaw.ui.screens.ModelsScreen

/**
 * Route definitions for the MOMCLAW app.
 */
object ModelsRoute {
    const val route = "models"
    
    /**
     * Navigation destination for models screen.
     */
    @Composable
    fun ModelsScreen(
        navController: NavHostController
    ) {
        ModelsScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }
}
