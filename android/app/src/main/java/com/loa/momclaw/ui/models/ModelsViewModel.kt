package com.loa.momclaw.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loa.momclaw.data.remote.ModelInfo
import com.loa.momclaw.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Models screen
 */
@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = _uiState.asStateFlow()

    init {
        loadModels()
    }

    /**
     * Load available models
     */
    fun loadModels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = chatRepository.getAvailableModels()
            
            result.fold(
                onSuccess = { models ->
                    val modelItems = models.map { info ->
                        ModelItem(
                            id = info.id,
                            name = info.name ?: info.id,
                            size = formatSize(info.size),
                            downloaded = info.downloaded,
                            loaded = info.loaded
                        )
                    }
                    _uiState.update { 
                        it.copy(
                            models = modelItems,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }

    /**
     * Download a model
     */
    fun downloadModel(modelId: String) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isDownloading = true,
                    downloadingModelId = modelId,
                    downloadProgress = 0f
                )
            }
            
            // Simulate download progress (in real app, this would be a real download)
            // For now, just mark as downloaded
            _uiState.update { state ->
                val updatedModels = state.models.map { model ->
                    if (model.id == modelId) {
                        model.copy(downloading = true)
                    } else {
                        model
                    }
                }
                state.copy(models = updatedModels)
            }

            // Simulate progress
            repeat(10) { i ->
                kotlinx.coroutines.delay(200)
                _uiState.update { it.copy(downloadProgress = (i + 1) / 10f) }
            }

            // Mark as complete
            _uiState.update { state ->
                val updatedModels = state.models.map { model ->
                    if (model.id == modelId) {
                        model.copy(downloading = false, downloaded = true)
                    } else {
                        model
                    }
                }
                state.copy(
                    models = updatedModels,
                    isDownloading = false,
                    downloadingModelId = null,
                    downloadProgress = 0f
                )
            }
        }
    }

    /**
     * Load a model into memory
     */
    fun loadModel(modelId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingModelId = modelId) }
            
            val result = chatRepository.loadModel(modelId)
            
            result.fold(
                onSuccess = { success ->
                    _uiState.update { state ->
                        val updatedModels = state.models.map { model ->
                            if (model.id == modelId) {
                                model.copy(loaded = success)
                            } else {
                                model.copy(loaded = false)  // Only one model loaded at a time
                            }
                        }
                        state.copy(
                            models = updatedModels,
                            loadingModelId = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            loadingModelId = null,
                            error = error.message
                        )
                    }
                }
            )
        }
    }

    /**
     * Delete a downloaded model
     */
    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                val updatedModels = state.models.map { model ->
                    if (model.id == modelId) {
                        model.copy(downloaded = false, loaded = false)
                    } else {
                        model
                    }
                }
                state.copy(models = updatedModels)
            }
        }
    }

    /**
     * Retry loading models after error
     */
    fun retry() {
        loadModels()
    }

    /**
     * Format file size for display
     */
    private fun formatSize(bytes: Long?): String {
        if (bytes == null) return "Unknown"
        
        val mb = bytes / (1024 * 1024)
        val gb = mb / 1024
        
        return if (gb > 0) {
            String.format("%.1f GB", gb.toFloat())
        } else {
            String.format("%d MB", mb)
        }
    }
}
