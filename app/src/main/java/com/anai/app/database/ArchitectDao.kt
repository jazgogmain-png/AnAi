package com.anai.app.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchitectDao {
    // --- KEYS ---
    @Query("SELECT * FROM keys")
    fun getAllKeys(): Flow<List<KeyEntity>>

    @Query("SELECT * FROM keys")
    suspend fun getAllKeysDirect(): List<KeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: KeyEntity)

    @Delete
    suspend fun deleteKey(key: KeyEntity)

    // --- PERSONAS (SOULS) ---
    @Query("SELECT * FROM personas")
    fun getAllPersonas(): Flow<List<PersonaEntity>>

    @Query("SELECT * FROM personas")
    suspend fun getAllPersonasDirect(): List<PersonaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersona(persona: PersonaEntity)

    @Delete
    suspend fun deletePersona(persona: PersonaEntity)

    // --- ENGINES (SCRIPTS) ---
    @Query("SELECT * FROM engines")
    fun getAllEngines(): Flow<List<EngineEntity>>

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

    // --- SUCCESS VAULT & PROMPT LAB (BIFURCATED) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlueprint(blueprint: BlueprintEntity)

    // 🎥 SUCCESS VAULT: Only show Video Scans
    @Query("SELECT * FROM blueprint_history WHERE entryType = 'SCAN' ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<BlueprintEntity>>

    // 🧪 PROMPT LAB: Only show Prompt iterations
    @Query("SELECT * FROM blueprint_history WHERE entryType = 'LAB' ORDER BY timestamp DESC")
    fun getLabHistory(): Flow<List<BlueprintEntity>>

    // 🛡️ LIFEBOAT: Direct access for full system backup
    @Query("SELECT * FROM blueprint_history")
    suspend fun getAllHistoryDirect(): List<BlueprintEntity>

    // 🏆 WINNERS CIRCLE: Starred Video Scans to be used as Aura Chips
    @Query("SELECT * FROM blueprint_history WHERE isStarred = 1 AND entryType = 'SCAN' ORDER BY timestamp DESC")
    fun getWinnersCircle(): Flow<List<BlueprintEntity>>

    @Query("UPDATE blueprint_history SET isStarred = :starred WHERE id = :id")
    suspend fun toggleStar(id: Int, starred: Boolean)

    @Query("UPDATE blueprint_history SET personaName = :newName WHERE id = :id")
    suspend fun updateBlueprintAlias(id: Int, newName: String)

    @Query("DELETE FROM blueprint_history WHERE id = :id")
    suspend fun deleteBlueprint(id: Int)

    // --- COMPATIBILITY (Optional: if you still use the old generic call somewhere) ---
    @Query("SELECT * FROM blueprint_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<BlueprintEntity>>
}