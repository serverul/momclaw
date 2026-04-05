package com.loa.momclaw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Get settings ViewModel to access dark theme preference
            // Using the defaultViewModelFactory from Hilt
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = viewModelFactory
            )
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            
            MOMCLAWTheme(
                darkTheme = settingsState.darkTheme,
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
