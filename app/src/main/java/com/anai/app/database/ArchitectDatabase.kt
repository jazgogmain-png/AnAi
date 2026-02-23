package com.anai.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        KeyEntity::class,
        PersonaEntity::class,
        EngineEntity::class,
        PlatformEntity::class,
        BlueprintEntity::class
    ],
    version = 6, // 🚀 Bumped to 6 for the new Partitioned Architecture
    exportSchema = false
)
abstract class ArchitectDatabase : RoomDatabase() {
    abstract fun architectDao(): ArchitectDao

    companion object {
        @Volatile
        private var INSTANCE: ArchitectDatabase? = null

        fun getDatabase(context: Context): ArchitectDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ArchitectDatabase::class.java,
                    "architect_database"
                )
                    // 🧨 NUCLEAR OPTION: Wipes the local DB to align with the new schema.
                    // Use your Lifeboat string to restore data after launch!
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}