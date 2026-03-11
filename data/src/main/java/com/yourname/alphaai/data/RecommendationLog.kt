package com.yourname.alphaai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recommendation_logs")
data class RecommendationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ruleId: String,
    val ruleName: String,
    val content: String,
    val triggerTime: Long = System.currentTimeMillis(),
    val status: String = "shown"
)
