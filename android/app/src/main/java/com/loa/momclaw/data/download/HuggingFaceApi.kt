package com.loa.momclaw.data.download

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

/**
 * HuggingFace API client for model metadata and download URLs.
 */
interface HuggingFaceApi {
    
    @GET("repos/{namespace}/{repoId}")
    suspend fun getRepository(
        @Path("namespace") namespace: String,
        @Path("repoId") repoId: String
    ): Response<HuggingFaceRepo>
    
    @GET("repos/{namespace}/{repoId}/tree/main")
    suspend fun listFiles(
        @Path("namespace") namespace: String,
        @Path("repoId") repoId: String
    ): Response<List<HuggingFaceFile>>
    
    @HEAD
    @GET("{namespace}/{repoId}/resolve/main/{filename}")
    suspend fun checkFile(
        @Path("namespace") namespace: String,
        @Path("repoId") repoId: String,
        @Path("filename") filename: String
    ): Response<Unit>
    
    companion object {
        const val BASE_URL = "https://huggingface.co/api/"
        const val FILE_BASE_URL = "https://huggingface.co/"
        
        fun create(): HuggingFaceApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
            
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(
                    kotlinx.serialization.json.Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }.asConverterFactory("application/json".toMediaType())
                )
                .build()
                .create(HuggingFaceApi::class.java)
        }
    }
}

@Serializable
data class HuggingFaceRepo(
    @SerialName("_id") val id: String,
    val id: String? = null,
    val author: String,
    @SerialName("sha") val sha: String,
    val siblings: List<HuggingFaceSibling>? = null,
    val tags: List<String>? = null,
    val downloads: Long? = null,
    @SerialName("likes") val likes: Int? = null,
    @SerialName("private") val isPrivate: Boolean? = null,
    val gated: Boolean? = null
)

@Serializable
data class HuggingFaceSibling(
    val rfilename: String,
    val size: Long? = null
)

@Serializable
data class HuggingFaceFile(
    val type: String,
    val oid: String,
    val size: Long,
    val path: String,
    val lfs: LfsInfo? = null
)

@Serializable
data class LfsInfo(
    val sha256: String,
    val size: Long,
    @SerialName("pointerSize") val pointerSize: Int? = null
)

/**
 * Model metadata from HuggingFace.
 */
data class ModelMetadata(
    val namespace: String,
    val repoId: String,
    val filename: String,
    val sizeBytes: Long,
    val sha256: String?,
    val downloadUrl: String,
    val huggingFaceUrl: String
) {
    companion object {
        /**
         * Predefined models available for MOMCLAW.
         */
        val AVAILABLE_MODELS = listOf(
            ModelMetadata(
                namespace = "litert-community",
                repoId = "gemma-3-E4B-it-litertlm",
                filename = "gemma-3-E4B-it.litertlm",
                sizeBytes = 3_900_000_000L, // ~3.9 GB
                sha256 = null,
                downloadUrl = "https://huggingface.co/litert-community/gemma-3-E4B-it-litertlm/resolve/main/gemma-3-E4B-it.litertlm",
                huggingFaceUrl = "https://huggingface.co/litert-community/gemma-3-E4B-it-litertlm"
            )
        )
        
        /**
         * Get download URL for a model file.
         */
        fun getDownloadUrl(namespace: String, repoId: String, filename: String): String {
            return "${HuggingFaceApi.FILE_BASE_URL}$namespace/$repoId/resolve/main/$filename"
        }
    }
    
    val sizeGB: Double get() = sizeBytes / (1024.0 * 1024.0 * 1024.0)
    val sizeMB: Long get() = sizeBytes / (1024 * 1024)
    val sizeDisplay: String get() = String.format("%.2f GB", sizeGB)
}
