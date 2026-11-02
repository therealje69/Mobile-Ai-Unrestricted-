package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CloudDeploymentEntity::class,
        PersonaProfileEntity::class,
        ConversationSessionEntity::class,
        SessionMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SovereignDatabase : RoomDatabase() {
    abstract fun sovereignDao(): SovereignDao

    companion object {
        @Volatile
        private var INSTANCE: SovereignDatabase? = null

        fun getInstance(context: Context): SovereignDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SovereignDatabase::class.java,
                    "sovereign_voice_vault.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
