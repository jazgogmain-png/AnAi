package com.anai.app.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchitectDao {
    // --- KEYS ---
    @Query("SELECT * FROM keys")
    fun getAllKeys(): Flow<List<KeyEntity>>

    // 🛡️ LIFEBOAT: Direct access for API Key backup
    @Query("SELECT * FROM keys")
    suspend fun getAllKeysDirect(): List<KeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: KeyEntity)

    @Delete
    suspend fun deleteKey(key: KeyEntity)

    // --- PERSONAS (SOULS) ---
    @Query("SELECT * FROM personas")
    fun getAllPersonas(): Flow<List<PersonaEntity>>

    // 🛡️ LIFEBOAT: Direct access for Souls backup
    @Query("SELECT * FROM personas")
    suspend fun getAllPersonasDirect(): List<PersonaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersona(persona: PersonaEntity)

    @Delete
    suspend fun deletePersona(persona: PersonaEntity)

    // --- ENGINES (SCRIPTS) ---
    @Query("SELECT * FROM engines")
    fun getAllEngines(): Flow<List<EngineEntity>>

    // 🛡️ LIFEBOAT: Direct access for Engines backup
    @Query("SELECT * FROM engines")
    suspend fun getAllEnginesDirect(): List<EngineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEngine(engine: EngineEntity)

    @Delete
    suspend fun deleteEngine(engine: EngineEntity)

    // --- PLATFORMS ---
    @Query("SELECT * FROM platforms")
    fun getAllPlatforms(): Flow<List<PlatformEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlatform(platform: PlatformEntity)

    @Delete
    suspend fun deletePlatform(platform: PlatformEntity)

    // --- SUCCESS VAULT (BLUEPRINT HISTORY) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlueprint(blueprint: BlueprintEntity)

    @Query("SELECT * FROM blueprint_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<BlueprintEntity>>

    // 🛡️ LIFEBOAT: Direct access for Success Vault backup
    @Query("SELECT * FROM blueprint_history")
    suspend fun getAllHistoryDirect(): List<BlueprintEntity>

    @Query("SELECT * FROM blueprint_history WHERE isStarred = 1 ORDER BY timestamp DESC")
    fun getWinnersCircle(): Flow<List<BlueprintEntity>>

    @Query("SELECT * FROM blueprint_history WHERE isStarred = 1 ORDER BY timestamp DESC LIMIT 5")
    fun getRecentVibes(): Flow<List<BlueprintEntity>>

    @Query("UPDATE blueprint_history SET isStarred = :starred WHERE id = :id")
    suspend fun toggleStar(id: Int, starred: Boolean)

    @Query("UPDATE blueprint_history SET personaName = :newName WHERE id = :id")
    suspend fun updateBlueprintAlias(id: Int, newName: String)

    @Query("DELETE FROM blueprint_history WHERE id = :id")
    suspend fun deleteBlueprint(id: Int)
}