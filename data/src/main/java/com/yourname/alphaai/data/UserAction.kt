package com.yourname.alphaai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_actions")
data class UserAction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val intent: String,
    val skillId: String?,
    val success: Boolean,
    val resultMessage: String?
)
