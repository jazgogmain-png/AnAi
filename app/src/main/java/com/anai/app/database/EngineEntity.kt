package com.anai.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "engines")
data class EngineEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val instructions: String,
    val draftTemplate: String
)