package com.anai.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blueprint_history")
data class BlueprintEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val videoUri: String,
    val thumbnailUri: String? = null, // <--- THE VISUAL ANCHOR
    val personaName: String,
    val platform: String,
    val titleUsed: String,
    val hookTimestamp: String,
    val auraProfile: String,
    val fullDescription: String, // Also stores the Cinematic Prompt in Lab entries
    val isStarred: Boolean = false,
    val alias: String? = null
)