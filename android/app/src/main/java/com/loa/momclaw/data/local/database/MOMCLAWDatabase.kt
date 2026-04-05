package com.loa.momclaw.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for MOMCLAW app
 */
@Database(
    entities = [MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MOMCLAWDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}
