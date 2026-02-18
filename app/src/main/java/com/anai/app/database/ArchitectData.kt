package com.anai.app.database

import androidx.room.*
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "architect_history")
data class ArchitectEntry(
    @PrimaryKey val timestamp: Long = System.currentTimeMillis(),
    val type: String,
    val inputContext: String,
    val aiResponse: String,
    val mediaUri: String? = null
)

@Entity(tableName = "personas")
data class PersonaEntity(
    @PrimaryKey val name: String,
    val instructions: String
)

@Entity(tableName = "api_keys")
data class KeyEntity(
    @PrimaryKey val key: String
)

@Dao
interface ArchitectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: ArchitectEntry): Long

    @Query("SELECT * FROM architect_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ArchitectEntry>>

    @Query("DELETE FROM architect_history")
    suspend fun nukeHistory()

    // --- PERSONA METHODS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePersona(persona: PersonaEntity)

    @Query("SELECT * FROM personas")
    fun getAllPersonas(): Flow<List<PersonaEntity>>

    @Query("DELETE FROM personas WHERE name = :name")
    suspend fun deletePersona(name: String)

    // --- KEY VAULT METHODS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveKey(key: KeyEntity)

    @Query("SELECT * FROM api_keys")
    fun getAllKeys(): Flow<List<KeyEntity>>

    @Query("DELETE FROM api_keys WHERE `key` = :key")
    suspend fun deleteKey(key: String)
}

@Database(entities = [ArchitectEntry::class, PersonaEntity::class, KeyEntity::class], version = 5)
abstract class ArchitectDatabase : RoomDatabase() {
    abstract fun architectDao(): ArchitectDao

    companion object {
        @Volatile private var INSTANCE: ArchitectDatabase? = null
        fun getDatabase(context: Context): ArchitectDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ArchitectDatabase::class.java,
                    "anai_architect_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}