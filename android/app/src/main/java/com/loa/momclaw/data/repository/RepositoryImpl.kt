package com.loa.momclaw.data.repository

import android.util.Log
import com.loa.momclaw.data.local.database.*
import com.loa.momclaw.data.local.preferences.SettingsPreferences
import com.loa.momclaw.data.remote.AgentClient
import com.loa.momclaw.data.remote.MessageDto
import com.loa.momclaw.domain.model.*
import com.loa.momclaw.domain.repository.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ChatRepository using Room database.
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val settingsPreferences: SettingsPreferences
) : ChatRepository {

    companion object {
        private const val TAG = "ChatRepository"
    }

    override fun getConversationMessages(conversationId: Long): Flow<List<Message>> {
        return database.messageDao().getConversationMessages(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCurrentConversation(): Flow<List<Message>> {
        return database.messageDao().getCurrentConversationMessages().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveMessage(message: Message): Result<Long> {
        return try {
            val entity = message.toEntity()
            val id = database.messageDao().insertMessage(entity)
            
            // Update conversation timestamp
            database.conversationDao().getConversation(message.conversationId)?.let { conv ->
                database.conversationDao().updateConversation(
                    conv.copy(updatedAt = System.currentTimeMillis())
                )
            }
            
            Result.success(id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save message", e)
            Result.failure(e)
        }
    }

    override suspend fun clearConversation(conversationId: Long): Result<Unit> {
        return try {
            database.messageDao().deleteConversationMessages(conversationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear conversation", e)
            Result.failure(e)
        }
    }

    override suspend fun clearCurrentConversation(): Result<Unit> {
        return try {
            val currentId = getCurrentConversationId()
            if (currentId > 0) {
                clearConversation(currentId)
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear current conversation", e)
            Result.failure(e)
        }
    }

    override fun getAllConversations(): Flow<List<Conversation>> {
        return database.conversationDao().getAllConversations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createConversation(title: String): Result<Long> {
        return try {
            val entity = ConversationEntity(title = title)
            val id = database.conversationDao().insertConversation(entity)
            settingsPreferences.setCurrentConversationId(id)
            Result.success(id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create conversation", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteConversation(conversationId: Long): Result<Unit> {
        return try {
            database.conversationDao().deleteConversationById(conversationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete conversation", e)
            Result.failure(e)
        }
    }

    override suspend fun getCurrentConversationId(): Long {
        val currentId = settingsPreferences.getCurrentConversationId()
        
        // If no conversation exists, create one
        if (currentId == 0L) {
            val newId = database.conversationDao().insertConversation(
                ConversationEntity(title = "New Chat")
            )
            settingsPreferences.setCurrentConversationId(newId)
            return newId
        }
        
        return currentId
    }

    override suspend fun setCurrentConversation(conversationId: Long) {
        settingsPreferences.setCurrentConversationId(conversationId)
    }
}

/**
 * Implementation of SettingsRepository using DataStore.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsPreferences: SettingsPreferences
) : SettingsRepository {

    override fun getSettings(): Flow<AgentSettings> {
        return settingsPreferences.getSettings()
    }

    override suspend fun updateSettings(settings: AgentSettings): Result<Unit> {
        return settingsPreferences.updateSettings(settings)
    }

    override suspend fun resetSettings(): Result<Unit> {
        return settingsPreferences.resetSettings()
    }
}

/**
 * Implementation of ModelRepository (placeholder).
 */
@Singleton
class ModelRepositoryImpl @Inject constructor(
    // Will inject LiteRTBridge when available
) : ModelRepository {

    private var currentModel: String? = null
    private val availableModels = listOf(
        Model(
            id = "gemma-4e4b",
            name = "Gemma 4E4B",
            description = "Gemma 4B Efficient model optimized for mobile",
            size = "3.65 GB",
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm"
        )
    )

    override suspend fun getAvailableModels(): Result<List<Model>> {
        return Result.success(availableModels)
    }

    override suspend fun downloadModel(modelId: String): Result<Unit> {
        // TODO: Implement model download from HuggingFace
        return Result.failure(NotImplementedError("Model download not implemented yet"))
    }

    override suspend fun loadModel(modelId: String): Result<Unit> {
        currentModel = modelId
        return Result.success(Unit)
    }

    override suspend fun deleteModel(modelId: String): Result<Unit> {
        if (currentModel == modelId) {
            currentModel = null
        }
        return Result.success(Unit)
    }

    override suspend fun getCurrentModel(): String? {
        return currentModel
    }
}

// Extension functions for mapping between domain and entity models

fun MessageEntity.toDomain(): Message = Message(
    id = id,
    conversationId = conversationId,
    role = role,
    content = content,
    timestamp = timestamp
)

fun Message.toEntity(): MessageEntity = MessageEntity(
    id = if (id == 0L) 0 else id,
    conversationId = conversationId,
    role = role,
    content = content,
    timestamp = timestamp
)

fun ConversationEntity.toDomain(): Conversation = Conversation(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Conversation.toEntity(): ConversationEntity = ConversationEntity(
    id = if (id == 0L) 0 else id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt
)
