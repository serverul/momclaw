package com.loa.momclaw.di

import com.loa.momclaw.data.local.database.AppDatabase
import com.loa.momclaw.data.local.preferences.SettingsPreferences
import com.loa.momclaw.data.repository.*
import com.loa.momclaw.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Provides ChatRepository implementation.
     */
    @Provides
    @Singleton
    fun provideChatRepository(
        database: AppDatabase,
        settingsPreferences: SettingsPreferences
    ): ChatRepository {
        return ChatRepositoryImpl(database, settingsPreferences)
    }

    /**
     * Provides SettingsRepository implementation.
     */
    @Provides
    @Singleton
    fun provideSettingsRepository(
        settingsPreferences: SettingsPreferences
    ): SettingsRepository {
        return SettingsRepositoryImpl(settingsPreferences)
    }

    /**
     * Provides ModelRepository implementation.
     */
    @Provides
    @Singleton
    fun provideModelRepository(): ModelRepository {
        return ModelRepositoryImpl()
    }
}
