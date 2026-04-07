package com.loa.momclaw.ui.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loa.momclaw.domain.model.Model
import com.loa.momclaw.domain.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State for the Models screen.
 */
data class ModelsState(
    val models: List<Model> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val downloadingModelId: String? = null,
    val loadingModelId: String? = null,
    val downloadProgress: Map<String, Float> = emptyMap(), // ModelId -> Progress (0.0 to 1.0)
    val selectedModelId: String? = null // Currently selected/active model
)

/**
 * Sealed class representing Models screen events.
 */
sealed class ModelsEvent {
    data class DownloadModel(val modelId: String) : ModelsEvent()
    data class LoadModel(val modelId: String) : ModelsEvent()
    data class DeleteModel(val modelId: String) : ModelsEvent()
    data class SelectModel(val modelId: String) : ModelsEvent() // Switch to this model
    object RefreshModels : ModelsEvent()
    object ClearError : ModelsEvent()
}

/**
 * ViewModel for managing models screen state and operations.
 */
@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val modelRepository: ModelRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ModelsState())
    val state: StateFlow<ModelsState> = _state.asStateFlow()

    companion object {
        private const val TAG = "ModelsViewModel"
    }

    init {
        loadModels()
    }

    /**
     * Handles Models screen events.
     */
    fun onEvent(event: ModelsEvent) {
        when (event) {
            is ModelsEvent.DownloadModel -> downloadModel(event.modelId)
            is ModelsEvent.LoadModel -> loadModel(event.modelId)
            is ModelsEvent.DeleteModel -> deleteModel(event.modelId)
            is ModelsEvent.SelectModel -> selectModel(event.modelId)
            is ModelsEvent.RefreshModels -> loadModels()
            is ModelsEvent.ClearError -> clearError()
        }
    }

    /**
     * Loads available models.
     */
    private fun loadModels() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            modelRepository.getAvailableModels()
                .onSuccess { models ->
                    _state.update { it.copy(
                        models = models,
                        isLoading = false
                    )}
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load models", error)
                    _state.update { it.copy(
                        error = "Failed to load models: ${error.message}",
                        isLoading = false
                    )}
                }
        }
    }

    /**
     * Downloads a model with simulated progress for UI feedback.
     */
    private fun downloadModel(modelId: String) {
        viewModelScope.launch {
            _state.update { it.copy(
                downloadingModelId = modelId,
                error = null,
                downloadProgress = _state.value.downloadProgress + (modelId to 0f)
            )}
            
            // Simulate progress updates (repository may not support real progress)
            val progressJob = launch {
                var progress = 0f
                while (progress < 0.95f && _state.value.downloadingModelId == modelId) {
                    delay(200)
                    progress += 0.05f
                    _state.update { it.copy(
                        downloadProgress = _state.value.downloadProgress + (modelId to progress.coerceAtMost(0.95f))
                    )}
                }
            }
            
            modelRepository.downloadModel(modelId)
                .onSuccess {
                    progressJob.cancel()
                    _state.update { it.copy(
                        downloadingModelId = null,
                        downloadProgress = _state.value.downloadProgress - modelId
                    )}
                    loadModels() // Refresh list
                }
                .onFailure { error ->
                    progressJob.cancel()
                    Log.e(TAG, "Failed to download model", error)
                    _state.update { it.copy(
                        downloadingModelId = null,
                        downloadProgress = _state.value.downloadProgress - modelId,
                        error = "Failed to download model: ${error.message}"
                    )}
                }
        }
    }

    /**
     * Loads a model into memory and selects it.
     */
    private fun loadModel(modelId: String) {
        viewModelScope.launch {
            _state.update { it.copy(loadingModelId = modelId, error = null) }
            
            modelRepository.loadModel(modelId)
                .onSuccess {
                    _state.update { it.copy(
                        loadingModelId = null,
                        selectedModelId = modelId // Auto-select when loaded
                    )}
                    loadModels() // Refresh list to show loaded status
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load model", error)
                    _state.update { it.copy(
                        loadingModelId = null,
                        error = "Failed to load model: ${error.message}"
                    )}
                }
        }
    }
    
    /**
     * Selects a model as active (switch to this model).
     */
    private fun selectModel(modelId: String) {
        val model = _state.value.models.find { it.id == modelId }
        if (model?.downloaded == true && !model.loaded) {
            // If downloaded but not loaded, load it first
            loadModel(modelId)
        } else if (model?.loaded == true) {
            // If already loaded, just select it
            _state.update { it.copy(selectedModelId = modelId) }
        }
    }

    /**
     * Deletes a downloaded model.
     */
    private fun deleteModel(modelId: String) {
        viewModelScope.launch {
            modelRepository.deleteModel(modelId)
                .onSuccess {
                    loadModels() // Refresh list
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to delete model", error)
                    _state.update { it.copy(
                        error = "Failed to delete model: ${error.message}"
                    )}
                }
        }
    }

    /**
     * Clears any error message.
     */
    private fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    /**
     * Helper to get currently loaded model.
     */
    fun getLoadedModel(): Model? {
        return _state.value.models.find { it.loaded }
    }
}
