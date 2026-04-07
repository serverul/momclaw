package com.loa.momclaw.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.loa.momclaw.model.LiteRModel
import com.loa.momclaw.model.ModelDownloader
import com.loa.momclaw.model.ModelManager
import com.loa.momclaw.model.StorageInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * State for the Models Screen.
 */
data class ModelsScreenState(
    val models: List<LiteRModel> = emptyList(),
    val downloadProgress: Map<String, ModelDownloader.DownloadProgress> = emptyMap(),
    val storageInfo: StorageInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val activeModelId: String? = null
)

/**
 * ViewModel for the Models Screen.
 * 
 * Manages model list, downloads, activation, and deletion.
 */
class ModelsScreenViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _state = MutableStateFlow(ModelsScreenState())
    val state: StateFlow<ModelsScreenState> = _state.asStateFlow()
    
    private var modelManager: ModelManager? = null
    
    companion object {
        private const val TAG = "ModelsScreenViewModel"
    }
    
    /**
     * Initialize with ModelManager instance.
     */
    fun initialize(manager: ModelManager) {
        if (modelManager != null) return
        
        modelManager = manager
        
        // Observe models list
        viewModelScope.launch {
            manager.models.collect { models ->
                _state.update { it.copy(models = models) }
                Log.d(TAG, "Models updated: ${models.size} total")
            }
        }
        
        // Observe active model
        viewModelScope.launch {
            manager.activeModelId.collect { activeId ->
                _state.update { it.copy(activeModelId = activeId) }
                Log.d(TAG, "Active model: $activeId")
            }
        }
        
        // Observe download progress
        viewModelScope.launch {
            manager.getDownloadProgress().collect { progressMap ->
                _state.update { it.copy(downloadProgress = progressMap) }
                
                // Log progress for active downloads
                progressMap.forEach { (modelId, progress) ->
                    if (progress.isDownloading) {
                        Log.d(TAG, "Download progress: $modelId - ${progress.percentComplete}%")
                    }
                }
            }
        }
        
        // Initial storage info
        updateStorageInfo()
    }
    
    /**
     * Refresh models list.
     */
    fun refresh() {
        modelManager?.refreshModels()
        updateStorageInfo()
    }
    
    /**
     * Download a model.
     */
    fun downloadModel(modelId: String) {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            
            modelManager?.downloadModel(modelId)?.collectLatest { progress ->
                when {
                    progress.isComplete -> {
                        Log.i(TAG, "Model downloaded successfully: $modelId")
                        updateStorageInfo()
                    }
                    progress.isFailed -> {
                        Log.e(TAG, "Model download failed: ${progress.errorMessage}")
                        _state.update { it.copy(
                            error = progress.errorMessage ?: "Download failed"
                        )}
                    }
                }
            }
        }
    }
    
    /**
     * Cancel a download.
     */
    fun cancelDownload(modelId: String) {
        modelManager?.cancelDownload(modelId)
        Log.i(TAG, "Download cancelled: $modelId")
    }
    
    /**
     * Activate a model.
     */
    fun activateModel(modelId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                modelManager?.setActiveModel(modelId)
                Log.i(TAG, "Model activated: $modelId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to activate model", e)
                _state.update { it.copy(
                    error = "Failed to activate model: ${e.message}"
                )}
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
    
    /**
     * Delete a model.
     */
    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            modelManager?.deleteModel(modelId)
                ?.onSuccess {
                    Log.i(TAG, "Model deleted: $modelId")
                    updateStorageInfo()
                }
                ?.onFailure { error ->
                    Log.e(TAG, "Failed to delete model", error)
                    _state.update { it.copy(
                        error = "Failed to delete model: ${error.message}"
                    )}
                }
            
            _state.update { it.copy(isLoading = false) }
        }
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    private fun updateStorageInfo() {
        modelManager?.getStorageInfo()?.let { info ->
            _state.update { it.copy(storageInfo = info) }
        }
    }
}
