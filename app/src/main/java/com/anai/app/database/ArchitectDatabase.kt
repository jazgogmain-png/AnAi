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
        PlatformEntity::class
    ],
    version = 2,
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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}