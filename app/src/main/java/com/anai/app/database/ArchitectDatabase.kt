package com.anai.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ArchitectEntry::class,
        PersonaEntity::class,
        KeyEntity::class,
        EngineEntity::class // <--- MAKE SURE THIS IS HERE
    ],
    version = 6, // <--- INCREMENT THIS (from 5 to 6)
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
                    "anai_architect_db"
                )
                    .fallbackToDestructiveMigration() // Wipes DB to avoid migration errors
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}