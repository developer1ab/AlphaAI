package com.yourname.alphaai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yourname.alphaai.data.AlphaAIDatabase

class RecommendationLogViewModel(private val database: AlphaAIDatabase) : ViewModel() {
    val recentLogs = database.recommendationLogDao().getRecentLogsLive(20)
}

class RecommendationLogViewModelFactory(
    private val database: AlphaAIDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecommendationLogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecommendationLogViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
