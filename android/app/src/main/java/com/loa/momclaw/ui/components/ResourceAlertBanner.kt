package com.loa.momclaw.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loa.momclaw.bridge.ResourceValidator

/**
 * Resource Alert Banner
 * 
 * Displays warnings when critical resources are missing or running in limited mode.
 * Shows at the top of screens when there are resource issues.
 */
@Composable
fun ResourceAlertBanner(
    validationResult: ResourceValidator.ValidationResult?,
    onDismiss: () -> Unit,
    onDownloadModel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = validationResult != null && validationResult ! is ResourceValidator.ValidationResult.Success,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        when (validationResult) {
            is ResourceValidator.ValidationResult.Warning -> {
                WarningBanner(
                    warnings = validationResult.warnings,
                    onDismiss = onDismiss,
                    modifier = modifier
                )
            }
            is ResourceValidator.ValidationResult.Error -> {
                ErrorBanner(
                    missingResources = validationResult.missingResources,
                    recoverySteps = validationResult.recoverySteps,
                    onDownloadModel = onDownloadModel,
                    onDismiss = onDismiss,
                    modifier = modifier
                )
            }
            else -> {}
        }
    }
}

/**
 * Warning banner for limited mode operation
 */
@Composable
private fun WarningBanner(
    warnings: List<String>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Limited Mode",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = warnings.firstOrNull() ?: "Some features may be limited",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Error banner for missing resources
 */
@Composable
private fun ErrorBanner(
    missingResources: List<ResourceValidator.MissingResource>,
    recoverySteps: List<String>,
    onDownloadModel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Setup Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Missing resources list
            missingResources.take(2).forEach { resource ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getResourceIcon(resource.type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column {
                        Text(
                            text = resource.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = resource.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        )
                        if (resource.size != null) {
                            Text(
                                text = "Size: ${resource.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Recovery steps
            if (recoverySteps.isNotEmpty()) {
                Text(
                    text = "How to fix:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                recoverySteps.take(3).forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                            modifier = Modifier.width(20.dp)
                        )
                        
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action button
            if (missingResources.any { it.type == ResourceValidator.ResourceType.MODEL }) {
                FilledTonalButton(
                    onClick = onDownloadModel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Model")
                }
            }
        }
    }
}

/**
 * Get icon for resource type
 */
@Composable
private fun getResourceIcon(type: ResourceValidator.ResourceType): ImageVector {
    return when (type) {
        ResourceValidator.ResourceType.BINARY -> Icons.Default.Memory
        ResourceValidator.ResourceType.MODEL -> Icons.Default.CloudDownload
        ResourceValidator.ResourceType.CONFIG -> Icons.Default.Settings
    }
}

/**
 * Compact resource status indicator for app bar
 */
@Composable
fun ResourceStatusIndicator(
    validationResult: ResourceValidator.ValidationResult?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, tint, contentDescription) = when (validationResult) {
        is ResourceValidator.ValidationResult.Success -> {
            Triple(Icons.Default.CheckCircle, MaterialTheme.colorScheme.primary, "All resources available")
        }
        is ResourceValidator.ValidationResult.Warning -> {
            Triple(Icons.Default.Warning, MaterialTheme.colorScheme.secondary, "Limited mode")
        }
        is ResourceValidator.ValidationResult.Error -> {
            Triple(Icons.Default.Error, MaterialTheme.colorScheme.error, "Resources missing")
        }
        null -> {
            Triple(Icons.Default.HelpOutline, MaterialTheme.colorScheme.outlineVariant, "Checking resources")
        }
    }
    
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier
        )
    }
}
