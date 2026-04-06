package com.loa.momclaw.di

import android.content.Context
import androidx.room.Room
import com.loa.momclaw.data.local.database.MOMCLAWDatabase
import com.loa.momclaw.data.local.database.MessageDao
import com.loa.momclaw.data.local.preferences.SettingsPreferences
import com.loa.momclaw.data.remote.AgentClient
import com.loa.momclaw.domain.model.AgentConfig
import com.loa.momclaw.domain.repository.ChatRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing app-level dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): MOMCLAWDatabase {
        return Room.databaseBuilder(
            context,
            MOMCLAWDatabase::class.java,
            "momclaw_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideSettingsPreferences(
        @ApplicationContext context: Context
    ): SettingsPreferences {
        return SettingsPreferences(context)
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: MOMCLAWDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideAgentConfig(): AgentConfig {
        return AgentConfig.DEFAULT
    }

    @Provides
    @Singleton
    fun provideAgentClient(config: AgentConfig): AgentClient {
        return AgentClient(config)
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        messageDao: MessageDao,
        agentClient: AgentClient,
        settingsPreferences: SettingsPreferences
    ): ChatRepository {
        return ChatRepository(messageDao, agentClient, settingsPreferences)
    }
}
