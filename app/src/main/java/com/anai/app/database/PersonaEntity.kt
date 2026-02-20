package com.anai.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personas")
data class PersonaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val instructions: String
)