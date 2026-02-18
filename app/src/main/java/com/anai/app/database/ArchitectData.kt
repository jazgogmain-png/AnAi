package com.anai.app.database

import androidx.room.*
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
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val instructions: String
)

// --- NEW ENTITY ---
@Entity(tableName = "engines")
data class EngineEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val instructions: String,
    val draftTemplate: String
)

@Entity(tableName = "api_keys")
data class KeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val key: String
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

    @Delete
    suspend fun deletePersona(persona: PersonaEntity)

    // --- ENGINE METHODS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEngine(engine: EngineEntity)

    @Query("SELECT * FROM engines")
    fun getAllEngines(): Flow<List<EngineEntity>>

    @Delete
    suspend fun deleteEngine(engine: EngineEntity)

    // --- KEY VAULT METHODS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveKey(key: KeyEntity)

    @Query("SELECT * FROM api_keys")
    fun getAllKeys(): Flow<List<KeyEntity>>

    @Delete
    suspend fun deleteKey(key: KeyEntity)
}