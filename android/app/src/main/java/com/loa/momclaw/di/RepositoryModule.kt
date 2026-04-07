package com.loa.momclaw.di

import com.loa.momclaw.bridge.LlmEngineWrapper
import com.loa.momclaw.bridge.ModelFallbackManager
import com.loa.momclaw.bridge.ModelLoader
import com.loa.momclaw.data.download.ModelDownloadManager
import com.loa.momclaw.data.local.database.AppDatabase
import com.loa.momclaw.data.local.preferences.SettingsPreferences
import com.loa.momclaw.data.repository.*
import com.loa.momclaw.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import android.content.Context
import javax.inject.Singleton

/**
 * Hilt module for providing repository implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideChatRepository(
        database: AppDatabase,
        settingsPreferences: SettingsPreferences
    ): ChatRepository {
        return ChatRepositoryImpl(database, settingsPreferences)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        settingsPreferences: SettingsPreferences
    ): SettingsRepository {
        return SettingsRepositoryImpl(settingsPreferences)
    }

    @Provides
    @Singleton
    fun provideModelDownloadManager(
        @ApplicationContext context: Context
    ): ModelDownloadManager {
        return ModelDownloadManager(context)
    }

    @Provides
    @Singleton
    fun provideLlmEngineWrapper(
        @ApplicationContext context: Context
    ): LlmEngineWrapper {
        return LlmEngineWrapper(context)
    }

    @Provides
    @Singleton
    fun provideModelFallbackManager(
        @ApplicationContext context: Context,
        engine: LlmEngineWrapper
    ): ModelFallbackManager {
        return ModelFallbackManager(context, engine)
    }

    @Provides
    @Singleton
    fun provideModelRepository(
        @ApplicationContext context: Context,
        downloadManager: ModelDownloadManager,
        engineWrapper: LlmEngineWrapper,
        fallbackManager: ModelFallbackManager
    ): ModelRepository {
        return ModelRepositoryImpl(context, downloadManager, engineWrapper, fallbackManager)
    }
}
