package com.loa.momclaw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.loa.momclaw.domain.model.AgentConfig
import com.loa.momclaw.startup.StartupManager
import com.loa.momclaw.ui.navigation.NavGraph
import com.loa.momclaw.ui.settings.SettingsViewModel
import com.loa.momclaw.ui.theme.MOMCLAWTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity for MOMCLAW app
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    
    @Inject
    lateinit var startupManager: StartupManager
    
    @Inject
    lateinit var agentConfig: AgentConfig
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Add lifecycle observer for automatic service management
        lifecycle.addObserver(startupManager)
        
        // Start services when activity is created
        startupManager.startServices(agentConfig)
        
        enableEdgeToEdge()
        setContent {
            // Get settings ViewModel to access dark theme preference
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = viewModelFactory
            )
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            
            // Calculate window size class for responsive design
            val windowSizeClass = calculateWindowSizeClass(this)
            
            MOMCLAWTheme(
                darkTheme = settingsState.darkTheme,
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        widthSizeClass = windowSizeClass.widthSizeClass
                    )
                }
            }
        }
    }
}
