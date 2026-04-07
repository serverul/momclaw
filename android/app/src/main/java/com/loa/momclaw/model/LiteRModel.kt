package com.loa.momclaw.model

/**
 * Data class representing a LiteRT-LM model for mobile inference.
 * 
 * @property id Unique identifier (e.g., "gemma-4-e4b-lt")
 * @property name Human-readable name (e.g., "Gemma 4E 4B Lite")
 * @property size Size in bytes
 * @property downloadUrl Remote download URL (null if bundled with app)
 * @property checksum SHA-256 or MD5 checksum for integrity verification
 * @property isDownloaded Whether the model has been downloaded
 * @property isActive Whether the model is currently loaded and active
 * @property downloadedSize Bytes downloaded so far (for partial downloads)
 * @property progress Download progress (0.0 - 1.0)
 */
data class LiteRModel(
    val id: String,
    val name: String,
    val size: Long,
    val downloadUrl: String?,
    val checksum: String,
    val isDownloaded: Boolean = false,
    val isActive: Boolean = false,
    val downloadedSize: Long = 0L,
    val progress: Float = 0f
) {
    /**
     * Size in GB for display
     */
    val sizeGB: Double get() = size / (1024.0 * 1024.0 * 1024.0)
    
    /**
     * Size in MB for display
     */
    val sizeMB: Long get() = size / (1024 * 1024)
    
    /**
     * Formatted size string
     */
    val sizeDisplay: String get() = String.format("%.2f GB", sizeGB)
    
    /**
     * Progress percentage (0-100)
     */
    val progressPercent: Int get() = (progress * 100).toInt()
    
    /**
     * Whether download is in progress
     */
    val isDownloading: Boolean get() = !isDownloaded && progress > 0f && progress < 1f
    
    /**
     * Whether this is a bundled model (included with app)
     */
    val isBundled: Boolean get() = downloadUrl == null
    
    /**
     * Formatted downloaded size for display
     */
    val downloadedSizeDisplay: String 
        get() = if (downloadedSize > 0) {
            String.format("%.2f GB / %.2f GB", 
                downloadedSize / (1024.0 * 1024.0 * 1024.0),
                sizeGB
            )
        } else {
            sizeDisplay
        }
    
    /**
     * Status text for UI display
     */
    val statusText: String
        get() = when {
            isActive -> "Active"
            isDownloaded -> "Ready"
            isDownloading -> "Downloading ($progressPercent%)"
            else -> "Not downloaded"
        }
    
    companion object {
        /**
         * Default model ID for new installations
         */
        const val DEFAULT_MODEL_ID = "gemma-4-e4b-lt"
        
        /**
         * Create a LiteRModel from domain Model
         */
        fun fromDomainModel(model: com.loa.momclaw.domain.model.Model): LiteRModel {
            val sizeBytes = parseSizeToBytes(model.size)
            return LiteRModel(
                id = model.id,
                name = model.name,
                size = sizeBytes,
                downloadUrl = model.downloadUrl,
                checksum = "", // Would be fetched from metadata
                isDownloaded = model.downloaded,
                isActive = model.loaded,
                downloadedSize = if (model.downloaded) sizeBytes else 0L,
                progress = if (model.downloaded) 1f else 0f
            )
        }
        
        /**
         * Parse size string (e.g., "3.90 GB") to bytes
         */
        private fun parseSizeToBytes(sizeStr: String): Long {
            val parts = sizeStr.trim().split(" ")
            if (parts.size != 2) return 0L
            
            val value = parts[0].toDoubleOrNull() ?: return 0L
            val unit = parts[1].uppercase()
            
            return when (unit) {
                "GB" -> (value * 1024 * 1024 * 1024).toLong()
                "MB" -> (value * 1024 * 1024).toLong()
                "KB" -> (value * 1024).toLong()
                "B" -> value.toLong()
                else -> 0L
            }
        }
    }
}
