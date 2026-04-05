package com.loa.momclaw.ui.settings

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
import kotlin.math.roundToInt

/**
 * UI State for Settings screen
 */
data class SettingsUiState(
    val systemPrompt: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val modelPrimary: String = "",
    val baseUrl: String = "",
    val darkTheme: Boolean = true,
    val streamingEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val backgroundAgentEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val hasChanges: Boolean = false
)

/**
 * Settings screen for app configuration
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
    onSave: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.hasChanges) {
                        TextButton(onClick = onSave) {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Agent Settings Section
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
                    supportingText = { Text("Instructions for the AI assistant") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Temperature Slider
                SettingsSlider(
                    label = "Temperature",
                    value = uiState.temperature,
                    onValueChange = onTemperatureChange,
                    valueRange = 0f..2f,
                    steps = 19,
                    supportingText = "Controls randomness: 0 = deterministic, 2 = creative"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Max Tokens Slider
                SettingsSlider(
                    label = "Max Tokens",
                    value = uiState.maxTokens.toFloat(),
                    onValueChange = { onMaxTokensChange(it.roundToInt()) },
                    valueRange = 256f..8192f,
                    steps = 30,
                    supportingText = "Maximum length of responses",
                    displayValue = { "${it.roundToInt()}" }
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
                    supportingText = { Text("Model ID for inference") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Base URL
                OutlinedTextField(
                    value = uiState.baseUrl,
                    onValueChange = onBaseUrlChange,
                    label = { Text("Agent URL") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    supportingText = { Text("NullClaw endpoint") }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // App Settings Section
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
                    icon = Icons.Default.BackgroundDotSmall,
                    checked = uiState.backgroundAgentEnabled,
                    onCheckedChange = onBackgroundAgentChange
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // About Section
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
                    }
                )

                ListItem(
                    headlineContent = { Text("Powered by") },
                    supportingContent = { Text("NullClaw + llama.cpp + Gemma") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Memory,
                            contentDescription = null
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Reset to Defaults
                OutlinedButton(
                    onClick = onResetToDefaults,
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
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save Button
            if (uiState.hasChanges) {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
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
                style = MaterialTheme.typography.titleMedium
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
    displayValue: (Float) -> String = { String.format("%.1f", it) }
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = displayValue(value),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                onCheckedChange = onCheckedChange
            )
        }
    )
}
