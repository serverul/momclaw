package com.loa.momclaw.di

import android.content.Context
import androidx.room.Room
import com.loa.momclaw.data.local.database.AppDatabase
import com.loa.momclaw.data.local.database.ConversationDao
import com.loa.momclaw.data.local.database.MessageDao
import com.loa.momclaw.data.local.preferences.SettingsPreferences
import com.loa.momclaw.data.remote.AgentClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing application-level dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides Room database instance.
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "momclaw_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * Provides MessageDao.
     */
    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }

    /**
     * Provides ConversationDao.
     */
    @Provides
    fun provideConversationDao(database: AppDatabase): ConversationDao {
        return database.conversationDao()
    }

    /**
     * Provides SettingsPreferences.
     */
    @Provides
    @Singleton
    fun provideSettingsPreferences(
        @ApplicationContext context: Context
    ): SettingsPreferences {
        return SettingsPreferences(context)
    }

    /**
     * Provides AgentClient for NullClaw communication.
     */
    @Provides
    @Singleton
    fun provideAgentClient(): AgentClient {
        return AgentClient(baseUrl = "http://localhost:9090")
    }
}
