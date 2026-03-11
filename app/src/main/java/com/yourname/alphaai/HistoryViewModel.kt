package com.yourname.alphaai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yourname.alphaai.data.AlphaAIDatabase

class HistoryViewModel(private val database: AlphaAIDatabase) : ViewModel() {
    val recentActions = database.userActionDao().getRecentActionsLive(20)
}

class HistoryViewModelFactory(
    private val database: AlphaAIDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
