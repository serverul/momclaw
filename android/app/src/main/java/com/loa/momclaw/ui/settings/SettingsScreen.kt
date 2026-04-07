package com.loa.momclaw.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// Note: SettingsUiState is now defined in SettingsViewModel.kt for proper MVVM architecture

/**
 * Settings screen for app configuration with Material3 compliance
 * and responsive layout for different screen sizes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onSystemPromptChange: (String) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onMaxTokensChange: (Int) -> Unit,
    onModelPrimaryChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onStreamingEnabledChange: (Boolean) -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onBackgroundAgentChange: (Boolean) -> Unit,
    onResetToDefaults: () -> Unit,
    onSave: () -> Unit,
    useNavigationRail: Boolean = false
) {
    val scrollState = rememberScrollState()
    
    // Use two-column layout for larger screens
    val useTwoColumnLayout = useNavigationRail
    
    // Snackbar state for save confirmation
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Validation states
    var baseUrlError by remember { mutableStateOf<String?>(null) }
    var temperatureError by remember { mutableStateOf<String?>(null) }
    var maxTokensError by remember { mutableStateOf<String?>(null) }
    
    // Reset confirmation dialog state
    var showResetDialog by remember { mutableStateOf(false) }
    
    // Derived states to minimize recomposition
    val showSaveButton = remember(uiState.hasChanges) {
        uiState.hasChanges
    }
    
    // Validation functions
    fun validateBaseUrl(url: String): Boolean {
        if (url.isEmpty()) return true // Allow empty
        return try {
            val uri = java.net.URI(url)
            uri.scheme in listOf("http", "https")
        } catch (e: Exception) {
            false
        }
    }
    
    fun validateTemperature(temp: Float): Boolean {
        return temp in 0f..2f
    }
    
    fun validateMaxTokens(tokens: Int): Boolean {
        return tokens in 256..8192
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (showSaveButton) {
                        val hasValidationErrors = baseUrlError != null || temperatureError != null || maxTokensError != null
                        FilledTonalButton(
                            onClick = onSave,
                            enabled = !hasValidationErrors
                        ) {
                            Text("Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (useTwoColumnLayout) {
                // Two-column layout for tablets
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Left column - Agent Settings
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(end = 8.dp)
                    ) {
                        AgentSettingsSection(
                            uiState = uiState,
                            onSystemPromptChange = onSystemPromptChange,
                            onTemperatureChange = onTemperatureChange,
                            onMaxTokensChange = onMaxTokensChange,
                            onModelPrimaryChange = onModelPrimaryChange,
                            onBaseUrlChange = onBaseUrlChange
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Right column - App Settings and About
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 8.dp)
                    ) {
                        AppSettingsSection(
                            uiState = uiState,
                            onDarkThemeChange = onDarkThemeChange,
                            onStreamingEnabledChange = onStreamingEnabledChange,
                            onNotificationsEnabledChange = onNotificationsEnabledChange,
                            onBackgroundAgentChange = onBackgroundAgentChange
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        AboutSection(
                            onResetToDefaults = onResetToDefaults
                        )

                        // Save button in column for tablets
                        val hasValidationErrors = baseUrlError != null || temperatureError != null || maxTokensError != null
                        AnimatedVisibility(
                            visible = showSaveButton,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically()
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onSave,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !hasValidationErrors
                            ) {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Changes")
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            } else {
                // Single-column layout for phones
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    AgentSettingsSection(
                        uiState = uiState,
                        onSystemPromptChange = onSystemPromptChange,
                        onTemperatureChange = onTemperatureChange,
                        onMaxTokensChange = onMaxTokensChange,
                        onModelPrimaryChange = onModelPrimaryChange,
                        onBaseUrlChange = onBaseUrlChange
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    AppSettingsSection(
                        uiState = uiState,
                        onDarkThemeChange = onDarkThemeChange,
                        onStreamingEnabledChange = onStreamingEnabledChange,
                        onNotificationsEnabledChange = onNotificationsEnabledChange,
                        onBackgroundAgentChange = onBackgroundAgentChange
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    AboutSection(
                        onResetToDefaults = onResetToDefaults
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Save button at bottom for phones
                    val hasValidationErrors = baseUrlError != null || temperatureError != null || maxTokensError != null
                    AnimatedVisibility(
                        visible = showSaveButton,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Button(
                            onClick = onSave,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            enabled = !hasValidationErrors
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Changes")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Agent Configuration Settings Section
 */
@Composable
private fun AgentSettingsSection(
    uiState: SettingsUiState,
    onSystemPromptChange: (String) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onMaxTokensChange: (Int) -> Unit,
    onModelPrimaryChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit
) {
    SettingsSection(
        title = "Agent Configuration",
        icon = Icons.Default.SmartToy
    ) {
        // System Prompt
        OutlinedTextField(
            value = uiState.systemPrompt,
            onValueChange = onSystemPromptChange,
            label = { Text("System Prompt") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            supportingText = { Text("Instructions for the AI assistant") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Temperature Slider with validation
        val tempError = if (!validateTemperature(uiState.temperature)) {
            "Temperature must be between 0 and 2"
        } else null
        
        SettingsSlider(
            label = "Temperature",
            value = uiState.temperature,
            onValueChange = { newValue ->
                temperatureError = if (!validateTemperature(newValue)) {
                    "Temperature must be between 0 and 2"
                } else {
                    null
                }
                onTemperatureChange(newValue)
            },
            valueRange = 0f..2f,
            steps = 19,
            supportingText = tempError ?: "Controls randomness: 0 = deterministic, 2 = creative",
            isError = tempError != null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Max Tokens Slider with validation
        val tokensError = if (!validateMaxTokens(uiState.maxTokens)) {
            "Max tokens must be between 256 and 8192"
        } else null
        
        SettingsSlider(
            label = "Max Tokens",
            value = uiState.maxTokens.toFloat(),
            onValueChange = { newValue ->
                val tokens = newValue.roundToInt()
                maxTokensError = if (!validateMaxTokens(tokens)) {
                    "Max tokens must be between 256 and 8192"
                } else {
                    null
                }
                onMaxTokensChange(tokens)
            },
            valueRange = 256f..8192f,
            steps = 30,
            supportingText = tokensError ?: "Maximum length of responses",
            displayValue = { "${it.roundToInt()}" },
            isError = tokensError != null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Model Primary
        OutlinedTextField(
            value = uiState.modelPrimary,
            onValueChange = onModelPrimaryChange,
            label = { Text("Primary Model") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Memory, contentDescription = null)
            },
            supportingText = { Text("Model ID for inference") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Base URL with validation
        OutlinedTextField(
            value = uiState.baseUrl,
            onValueChange = { newValue ->
                baseUrlError = if (newValue.isNotEmpty() && !validateBaseUrl(newValue)) {
                    "Invalid URL format (must start with http:// or https://)"
                } else {
                    null
                }
                onBaseUrlChange(newValue)
            },
            label = { Text("Agent URL") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Link, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            isError = baseUrlError != null,
            supportingText = { 
                Text(baseUrlError ?: "NullClaw endpoint")
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )
    }
}

/**
 * App Settings Section
 */
@Composable
private fun AppSettingsSection(
    uiState: SettingsUiState,
    onDarkThemeChange: (Boolean) -> Unit,
    onStreamingEnabledChange: (Boolean) -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onBackgroundAgentChange: (Boolean) -> Unit
) {
    SettingsSection(
        title = "App Settings",
        icon = Icons.Default.Settings
    ) {
        // Dark Theme Toggle
        SettingsSwitch(
            title = "Dark Theme",
            subtitle = "Use dark color scheme",
            icon = Icons.Default.DarkMode,
            checked = uiState.darkTheme,
            onCheckedChange = onDarkThemeChange
        )

        // Streaming Toggle
        SettingsSwitch(
            title = "Stream Responses",
            subtitle = "Show tokens as they're generated",
            icon = Icons.Default.Stream,
            checked = uiState.streamingEnabled,
            onCheckedChange = onStreamingEnabledChange
        )

        // Notifications Toggle
        SettingsSwitch(
            title = "Notifications",
            subtitle = "Show alerts for agent activity",
            icon = Icons.Default.Notifications,
            checked = uiState.notificationsEnabled,
            onCheckedChange = onNotificationsEnabledChange
        )

        // Background Agent Toggle
        SettingsSwitch(
            title = "Background Agent",
            subtitle = "Keep agent running when app is closed",
            icon = Icons.Default.Loop,
            checked = uiState.backgroundAgentEnabled,
            onCheckedChange = onBackgroundAgentChange
        )
    }
}

/**
 * About Section
 */
@Composable
private fun AboutSection(
    onResetToDefaults: () -> Unit
) {
    SettingsSection(
        title = "About",
        icon = Icons.Default.Info
    ) {
        ListItem(
            headlineContent = { Text("MOMCLAW") },
            supportingContent = { Text("Version 1.0.0") },
            leadingContent = {
                Icon(
                    Icons.Default.Pets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        ListItem(
            headlineContent = { Text("Powered by") },
            supportingContent = { Text("NullClaw + llama.cpp + Gemma") },
            leadingContent = {
                Icon(
                    Icons.Default.Memory,
                    contentDescription = null
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Reset to Defaults with confirmation
        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                Icons.Default.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset to Defaults")
        }
        
        // Reset confirmation dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset to Defaults") },
                text = { Text("This will reset all settings to their default values. This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onResetToDefaults()
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Reset")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * Reusable settings section with header
 */
@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        content()
    }
}

/**
 * Reusable slider with label
 */
@Composable
fun SettingsSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    supportingText: String,
    modifier: Modifier = Modifier,
    displayValue: (Float) -> String = { String.format("%.1f", it) },
    isError: Boolean = false
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = displayValue(value),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                activeTrackColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        )
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Reusable toggle switch
 */
@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
