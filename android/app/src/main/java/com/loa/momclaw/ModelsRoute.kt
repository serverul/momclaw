package com.loa.momclaw

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.loa.momclaw.ui.models.ModelItem
import com.loa.momclaw.ui.models.ModelsScreen
import com.loa.momclaw.ui.models.ModelsUiState

/**
 * Route composable that connects ViewModel to ModelsScreen
 * In production, this would use hiltViewModel()
 */
@Composable
fun ModelsRoute(
    onNavigateBack: () -> Unit
) {
    // Placeholder state - in real app, use viewModel()
    val uiState = ModelsUiState(
        models = listOf(
            ModelItem(
                id = "gemma-4e4b",
                name = "Gemma 4E4B-it",
                size = "2.5 GB",
                downloaded = true,
                loaded = true
            ),
            ModelItem(
                id = "llama-3.2-3b",
                name = "Llama 3.2 3B",
                size = "2.1 GB",
                downloaded = true,
                loaded = false
            ),
            ModelItem(
                id = "phi-3-mini",
                name = "Phi-3 Mini",
                size = "1.8 GB",
                downloaded = false,
                loaded = false
            )
        ),
        isLoading = false
    )

    ModelsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onDownloadModel = {},
        onLoadModel = {},
        onDeleteModel = {},
        onRefresh = {},
        onRetry = {}
    )
}

@Preview(showBackground = true)
@Composable
fun ModelsRoutePreview() {
    ModelsRoute(
        onNavigateBack = {}
    )
}
