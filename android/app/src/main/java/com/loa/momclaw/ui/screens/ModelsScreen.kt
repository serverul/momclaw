package com.loa.momclaw.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loa.momclaw.model.LiteRModel
import com.loa.momclaw.model.ModelDownloader
import com.loa.momclaw.model.ModelManager
import com.loa.momclaw.model.StorageInfo
import kotlinx.coroutines.flow.collectLatest

/**
 * Models Screen - Manages LiteRT-LM model downloads and activation.
 * 
 * Features:
 * - List available models with download status
 * - Download progress with real-time updates
 * - Model activation/deactivation
 * - Delete downloaded models
 * - Storage information display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ModelsScreenViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.initialize(ModelManager.getInstance(context))
    }
    
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
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Storage info banner
            state.storageInfo?.let { storageInfo ->
                StorageInfoBanner(
                    storageInfo = storageInfo,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // Error message
            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                state.error?.let { error ->
                    ErrorBanner(
                        error = error,
                        onDismiss = { viewModel.clearError() },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
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
                    val downloadProgress = state.downloadProgress[model.id]
                    
                    ModelCard(
                        model = model,
                        downloadProgress = downloadProgress,
                        onActivate = { viewModel.activateModel(model.id) },
                        onDownload = { viewModel.downloadModel(model.id) },
                        onCancelDownload = { viewModel.cancelDownload(model.id) },
                        onDelete = { viewModel.deleteModel(model.id) }
                    )
                }
                
                // Empty state
                if (state.models.isEmpty() && !state.isLoading) {
                    item {
                        EmptyModelState(
                            onRefresh = { viewModel.refresh() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Storage information banner.
 */
@Composable
fun StorageInfoBanner(
    storageInfo: StorageInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Storage",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = storageInfo.formatInfo(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            
            // Storage warning if low
            if (storageInfo.freeSpaceGB < 5.0) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Low storage",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Error message banner.
 */
@Composable
fun ErrorBanner(
    error: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

/**
 * Model card with download progress and actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelCard(
    model: LiteRModel,
    downloadProgress: ModelDownloader.DownloadProgress?,
    onActivate: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Model header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Size: ${model.sizeDisplay}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status badge
                when {
                    model.isActive -> {
                        StatusBadge(
                            text = "Active",
                            icon = Icons.Default.CheckCircle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    model.isDownloaded -> {
                        StatusBadge(
                            text = "Ready",
                            icon = Icons.Default.Check,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    downloadProgress?.isDownloading == true -> {
                        StatusBadge(
                            text = "${downloadProgress.percentComplete}%",
                            icon = Icons.Default.Downloading,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            
            // Download progress bar
            downloadProgress?.let { progress ->
                if (progress.isDownloading || progress.isPaused) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress.percentComplete / 100f,
                        label = "download_progress"
                    )
                    
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = progress.formatProgress(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = progress.formatRemaining(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            ModelActionButtons(
                model = model,
                downloadProgress = downloadProgress,
                onActivate = onActivate,
                onDownload = onDownload,
                onCancelDownload = onCancelDownload,
                onDelete = onDelete
            )
        }
    }
}

/**
 * Status badge component.
 */
@Composable
fun StatusBadge(
    text: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}

/**
 * Action buttons based on model state.
 */
@Composable
fun ModelActionButtons(
    model: LiteRModel,
    downloadProgress: ModelDownloader.DownloadProgress?,
    onActivate: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            downloadProgress?.isDownloading == true -> {
                // Cancel download button
                OutlinedButton(
                    onClick = onCancelDownload,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Download")
                }
            }
            
            model.isActive -> {
                // Model is active
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
            
            model.isDownloaded -> {
                // Model is downloaded, can be activated
                Button(onClick = onActivate) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Activate")
                }
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onDelete) {
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download ${model.sizeDisplay}")
                }
            }
        }
    }
}

/**
 * Empty state component.
 */
@Composable
fun EmptyModelState(
    onRefresh: () -> Unit
) {
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
            text = "Pull to refresh or check your connection",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))
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
