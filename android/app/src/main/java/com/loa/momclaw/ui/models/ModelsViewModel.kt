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
    val loadingModelId: String? = null
)

/**
 * Sealed class representing Models screen events.
 */
sealed class ModelsEvent {
    data class DownloadModel(val modelId: String) : ModelsEvent()
    data class LoadModel(val modelId: String) : ModelsEvent()
    data class DeleteModel(val modelId: String) : ModelsEvent()
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
     * Downloads a model.
     */
    private fun downloadModel(modelId: String) {
        viewModelScope.launch {
            _state.update { it.copy(downloadingModelId = modelId, error = null) }
            
            modelRepository.downloadModel(modelId)
                .onSuccess {
                    _state.update { it.copy(downloadingModelId = null) }
                    loadModels() // Refresh list
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to download model", error)
                    _state.update { it.copy(
                        downloadingModelId = null,
                        error = "Failed to download model: ${error.message}"
                    )}
                }
        }
    }

    /**
     * Loads a model into memory.
     */
    private fun loadModel(modelId: String) {
        viewModelScope.launch {
            _state.update { it.copy(loadingModelId = modelId, error = null) }
            
            modelRepository.loadModel(modelId)
                .onSuccess {
                    _state.update { it.copy(loadingModelId = null) }
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
}
