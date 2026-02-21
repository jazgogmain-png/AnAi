package com.anai.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blueprint_history")
data class BlueprintEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val videoUri: String,
    val thumbnailUri: String? = null, // <--- NEW: Path to the snatched frame
    val personaName: String,
    val platform: String,
    val titleUsed: String,
    val hookTimestamp: String,
    val auraProfile: String,
    val fullDescription: String,
    val isStarred: Boolean = false
)