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
    version = 5, // 🛠️ Bump to 5 to force a clean break from the mess
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
                    // 🧨 NUCLEAR OPTION: This wipes the old database and starts fresh.
                    // Since you backed up to your desktop, we are safe to do this!
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}