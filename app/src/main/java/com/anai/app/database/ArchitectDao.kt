package com.anai.app.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchitectDao {
    // --- KEYS ---
    @Query("SELECT * FROM keys")
    fun getAllKeys(): Flow<List<KeyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: KeyEntity)

    @Delete
    suspend fun deleteKey(key: KeyEntity)

    // --- PERSONAS (SOULS) ---
    @Query("SELECT * FROM personas")
    fun getAllPersonas(): Flow<List<PersonaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersona(persona: PersonaEntity)

    @Delete
    suspend fun deletePersona(persona: PersonaEntity) // <--- THIS WAS LIKELY MISSING

    // --- ENGINES (SCRIPTS) ---
    @Query("SELECT * FROM engines")
    fun getAllEngines(): Flow<List<EngineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEngine(engine: EngineEntity)

    @Delete
    suspend fun deleteEngine(engine: EngineEntity) // <--- THIS TOO

    // --- PLATFORMS ---
    @Query("SELECT * FROM platforms")
    fun getAllPlatforms(): Flow<List<PlatformEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlatform(platform: PlatformEntity)

    @Delete
    suspend fun deletePlatform(platform: PlatformEntity)
}