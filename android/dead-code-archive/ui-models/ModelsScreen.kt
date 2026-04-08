package com.loa.momclaw.ui.models

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loa.momclaw.domain.model.Model
import com.loa.momclaw.ui.common.HapticUtils
import com.loa.momclaw.ui.components.modelCardAccessibility

/**
 * Models screen composable for managing AI models.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(
    state: ModelsState,
    onEvent: (ModelsEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val hapticManager = HapticUtils.rememberHapticManager()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Models",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            hapticManager.lightTap()
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            hapticManager.lightTap()
                            onEvent(ModelsEvent.RefreshModels)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Refresh"
                        )
                    }
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
        ) {
            // Error message
            state.error?.let { error ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                hapticManager.lightTap()
                                onEvent(ModelsEvent.ClearError)
                            }
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            // Loading indicator
            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Models list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = state.models,
                    key = { it.id }
                ) { model ->
                    ModelCard(
                        model = model,
                        isDownloading = state.downloadingModelId == model.id,
                        isLoading = state.loadingModelId == model.id,
                        downloadProgress = state.downloadProgress[model.id] ?: 0f,
                        isSelected = state.selectedModelId == model.id,
                        onSelect = { 
                            hapticManager.lightTap()
                            onEvent(ModelsEvent.SelectModel(model.id)) 
                        },
                        onDownload = { 
                            hapticManager.mediumTap()
                            onEvent(ModelsEvent.DownloadModel(model.id)) 
                        },
                        onLoad = { 
                            hapticManager.success()
                            onEvent(ModelsEvent.LoadModel(model.id)) 
                        },
                        onDelete = { 
                            hapticManager.heavyTap()
                            onEvent(ModelsEvent.DeleteModel(model.id)) 
                        }
                    )
                }

                // Empty state
                if (state.models.isEmpty() && !state.isLoading) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📦",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No models available",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Check your internet connection and try again",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Model status enum for visual indicators
 */
enum class ModelStatus(val icon: ImageVector, val label: String, val color: @Composable () -> Color) {
    NOT_DOWNLOADED(Icons.Default.CloudDownload, "Not Downloaded", { MaterialTheme.colorScheme.onSurfaceVariant }),
    DOWNLOADED(Icons.Default.DownloadDone, "Downloaded", { MaterialTheme.colorScheme.primary }),
    LOADED(Icons.Default.CheckCircle, "Active", { MaterialTheme.colorScheme.primary }),
    FAILED(Icons.Default.Error, "Failed", { MaterialTheme.colorScheme.error })
}

/**
 * Individual model card component with enhanced features.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelCard(
    model: Model,
    isDownloading: Boolean,
    isLoading: Boolean,
    downloadProgress: Float = 0f,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    onDownload: () -> Unit,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    // Determine model status
    val status = when {
        model.loaded -> ModelStatus.LOADED
        model.downloaded -> ModelStatus.DOWNLOADED
        isDownloading || model.downloadUrl == null -> ModelStatus.NOT_DOWNLOADED
        else -> ModelStatus.NOT_DOWNLOADED
    }

    // Animated elevation for selected state
    val elevation by animateFloatAsState(
        targetValue = if (isSelected) 8f else 2f,
        label = "elevation"
    )
    
    // Animated border color for selected state
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "borderColor"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${model.name}. ${status.label}. Size ${model.size}. ${if (isSelected) "Currently selected" else "Not selected"}"
            },
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = elevation.dp
        ),
        onClick = onSelect
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Model header with status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status icon
                    Surface(
                        color = status.color().copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = status.icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = status.color()
                            )
                        }
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = model.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Size: ${model.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "•",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = status.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = status.color()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Model description
            Text(
                text = model.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Download progress bar (only when downloading)
            if (isDownloading && downloadProgress > 0f) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = downloadProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Downloading...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    isDownloading && downloadProgress == 0f -> {
                        // Starting download
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Starting download...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    isLoading -> {
                        // Loading model into memory
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Loading model...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    model.loaded -> {
                        // Model is loaded and active
                        AssistChip(
                            onClick = { },
                            label = { Text("Active") },
                            enabled = false,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.semantics {
                                contentDescription = "Delete ${model.name} model"
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                    model.downloaded -> {
                        // Model is downloaded but not loaded - can switch to it
                        Button(
                            onClick = {
                                onLoad()
                                onSelect()
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "Load and switch to ${model.name}"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Switch to Model")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.semantics {
                                contentDescription = "Delete ${model.name} model"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                    else -> {
                        // Model needs to be downloaded
                        Button(
                            onClick = onDownload,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = "Download ${model.name} model, size ${model.size}"
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download ${model.size}")
                        }
                    }
                }
            }
        }
    }
}
