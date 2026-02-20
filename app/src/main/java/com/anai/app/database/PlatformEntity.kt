package com.anai.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "platforms")
data class PlatformEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val hasDescription: Boolean,
    val hasTags: Boolean
)