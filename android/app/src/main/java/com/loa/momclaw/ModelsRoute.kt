package com.loa.momclaw

import androidx.compose.runtime.Composable
import com.loa.momclaw.ui.models.EnhancedModelsScreen
import com.loa.momclaw.ui.models.EnhancedModelsState
import com.loa.momclaw.ui.models.EnhancedModelsEvent
import com.loa.momclaw.ui.models.EnhancedModelsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

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
        navController: NavHostController,
        viewModel: EnhancedModelsViewModel = viewModel()
    ) {
        val state by viewModel.state
        
        EnhancedModelsScreen(
            state = state,
            onEvent = { event ->
                viewModel.onEvent(event)
            },
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }
}
