package com.loa.momclaw.ui.models

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Models management screen with Material3 compliance
 * and responsive layout for different screen sizes
 * 
 * Note: ModelsUiState and ModelItem are defined in ModelsViewModel.kt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(
    uiState: ModelsUiState,
    onNavigateBack: () -> Unit,
    onDownloadModel: (String) -> Unit,
    onLoadModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    useNavigationRail: Boolean = false
) {
    // Use grid layout for larger screens
    val useGridLayout = useNavigationRail
    val gridColumns = if (useGridLayout) 2 else 1
    val contentPadding = if (useGridLayout) 24.dp else 16.dp
    val itemSpacing = if (useGridLayout) 16.dp else 12.dp

    // Derive states to minimize recomposition
    val showLoading = remember(uiState.isLoading, uiState.models.isEmpty()) {
        uiState.isLoading && uiState.models.isEmpty()
    }
    
    val showEmpty = remember(uiState.isLoading, uiState.models.isEmpty()) {
        !uiState.isLoading && uiState.models.isEmpty()
    }
    
    val showList = remember(uiState.models.isNotEmpty()) {
        uiState.models.isNotEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Models",
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
                    IconButton(
                        onClick = onRefresh,
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Error banner
            AnimatedVisibility(visible = uiState.error != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.error ?: "Unknown error",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
            }

            // Download progress indicator
            AnimatedVisibility(
                visible = uiState.isDownloading && uiState.downloadingModelId != null
            ) {
                DownloadProgressIndicator(
                    modelId = uiState.downloadingModelId ?: "",
                    progress = uiState.downloadProgress
                )
            }

            // Loading indicator
            if (showLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading models...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Empty state
            if (showEmpty) {
                EmptyModelsState(onRefresh = onRefresh)
            }

            // Models list
            if (showList) {
                if (useGridLayout) {
                    // Grid layout for tablets
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(contentPadding),
                        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                        verticalArrangement = Arrangement.spacedBy(itemSpacing)
                    ) {
                        items(
                            items = uiState.models,
                            key = { it.id }
                        ) { model ->
                            ModelCard(
                                model = model,
                                onDownload = { onDownloadModel(model.id) },
                                onLoad = { onLoadModel(model.id) },
                                onDelete = { onDeleteModel(model.id) },
                                isDownloading = uiState.downloadingModelId == model.id && uiState.isDownloading,
                                isLoading = uiState.loadingModelId == model.id,
                                compactMode = true
                            )
                        }
                    }
                } else {
                    // List layout for phones
                    LazyColumn(
                        contentPadding = PaddingValues(contentPadding),
                        verticalArrangement = Arrangement.spacedBy(itemSpacing)
                    ) {
                        items(
                            items = uiState.models,
                            key = { it.id }
                        ) { model ->
                            ModelCard(
                                model = model,
                                onDownload = { onDownloadModel(model.id) },
                                onLoad = { onLoadModel(model.id) },
                                onDelete = { onDeleteModel(model.id) },
                                isDownloading = uiState.downloadingModelId == model.id && uiState.isDownloading,
                                isLoading = uiState.loadingModelId == model.id,
                                compactMode = false
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Download progress indicator with proper Material3 styling
 */
@Composable
private fun DownloadProgressIndicator(
    modelId: String,
    progress: Float
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Downloading $modelId",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Empty state when no models are available
 */
@Composable
private fun EmptyModelsState(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Inventory,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No models available",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pull models from HuggingFace to get started",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh")
            }
        }
    }
}

/**
 * Model card with actions - optimized for different layouts
 */
@Composable
fun ModelCard(
    model: ModelItem,
    onDownload: () -> Unit,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
    isDownloading: Boolean,
    isLoading: Boolean,
    compactMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // Determine status color
    val statusColor = when {
        model.loaded -> MaterialTheme.colorScheme.primaryContainer
        model.downloaded -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        if (compactMode) {
            // Compact layout for grid view (tablets)
            CompactModelContent(
                model = model,
                statusColor = statusColor,
                rotation = rotation,
                isDownloading = isDownloading,
                isLoading = isLoading,
                onDownload = onDownload,
                onLoad = onLoad,
                onDelete = onDelete
            )
        } else {
            // Full layout for list view (phones)
            FullModelContent(
                model = model,
                statusColor = statusColor,
                rotation = rotation,
                isDownloading = isDownloading,
                isLoading = isLoading,
                onDownload = onDownload,
                onLoad = onLoad,
                onDelete = onDelete
            )
        }
    }
}

/**
 * Compact model card content for tablet grid layout
 */
@Composable
private fun CompactModelContent(
    model: ModelItem,
    statusColor: androidx.compose.ui.graphics.Color,
    rotation: Float,
    isDownloading: Boolean,
    isLoading: Boolean,
    onDownload: () -> Unit,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status icon
        ModelStatusIcon(
            model = model,
            statusColor = statusColor,
            rotation = rotation,
            isLoading = isLoading || isDownloading
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Model name
        Text(
            text = model.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Size and status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = model.size,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (model.loaded) {
                Spacer(modifier = Modifier.width(8.dp))
                SuggestionChip(
                    onClick = {},
                    label = { Text("Active", fontSize = 10.sp) },
                    modifier = Modifier.height(20.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons
        ModelActions(
            model = model,
            isDownloading = isDownloading,
            isLoading = isLoading,
            onDownload = onDownload,
            onLoad = onLoad,
            onDelete = onDelete,
            compactMode = true
        )
    }
}

/**
 * Full model card content for phone list layout
 */
@Composable
private fun FullModelContent(
    model: ModelItem,
    statusColor: androidx.compose.ui.graphics.Color,
    rotation: Float,
    isDownloading: Boolean,
    isLoading: Boolean,
    onDownload: () -> Unit,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status icon
        ModelStatusIcon(
            model = model,
            statusColor = statusColor,
            rotation = rotation,
            isLoading = isLoading || isDownloading
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Model info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = model.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = model.size,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (model.loaded) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Active") },
                        modifier = Modifier.height(24.dp),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }

        // Action buttons
        ModelActions(
            model = model,
            isDownloading = isDownloading,
            isLoading = isLoading,
            onDownload = onDownload,
            onLoad = onLoad,
            onDelete = onDelete,
            compactMode = false
        )
    }
}

/**
 * Model status indicator icon
 */
@Composable
private fun ModelStatusIcon(
    model: ModelItem,
    statusColor: androidx.compose.ui.graphics.Color,
    rotation: Float,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(statusColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            model.loaded -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            model.downloaded -> {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Model action buttons
 */
@Composable
private fun ModelActions(
    model: ModelItem,
    isDownloading: Boolean,
    isLoading: Boolean,
    onDownload: () -> Unit,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
    compactMode: Boolean
) {
    if (compactMode) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!model.downloaded) {
                FilledTonalButton(
                    onClick = onDownload,
                    enabled = !isDownloading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download", fontSize = 12.sp)
                    }
                }
            } else if (!model.loaded) {
                OutlinedButton(
                    onClick = onLoad,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Load", fontSize = 12.sp)
                    }
                }
            }

            if (model.downloaded && !model.loaded) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!model.downloaded) {
                // Download button
                FilledTonalButton(
                    onClick = onDownload,
                    enabled = !isDownloading
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download")
                }
            } else if (!model.loaded) {
                // Load button
                OutlinedButton(
                    onClick = onLoad,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Load")
                }
            }

            if (model.downloaded) {
                // Delete button
                IconButton(
                    onClick = onDelete,
                    enabled = !model.loaded
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = if (model.loaded) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }
    }
}
