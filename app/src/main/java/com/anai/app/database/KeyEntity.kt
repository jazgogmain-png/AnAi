package com.anai.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "keys")
data class KeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val key: String
)