package com.anai.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Room

/**
 * The core data model for every "Save Point" in the app.
 * We use the timestamp as the name/ID as you suggested.
 */
@Entity(tableName = "architect_history")
data class ArchitectEntry(
    @PrimaryKey val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "VIDEO_SCAN", "STATS_SCREENSHOT", or "CHAT"
    val inputContext: String, // Your text input or the prompt used
    val aiResponse: String,   // What Gemini 3 Flash returned
    val mediaUri: String? = null // Path to the video or screenshot if applicable
)

@Dao
interface ArchitectDao {
    @Insert
    suspend fun insertEntry(entry: ArchitectEntry)

    @Query("SELECT * FROM architect_history ORDER BY timestamp DESC")
    fun getAllHistory(): kotlinx.coroutines.flow.Flow<List<ArchitectEntry>>

    @Query("DELETE FROM architect_history WHERE timestamp = :id")
    suspend fun deleteEntry(id: Long)
}

@Database(entities = [ArchitectEntry::class], version = 1)
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}