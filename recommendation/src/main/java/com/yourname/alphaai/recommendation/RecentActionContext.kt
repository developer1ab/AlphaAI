package com.yourname.alphaai.recommendation

import com.yourname.alphaai.data.AlphaAIDatabase

class RecentActionContext(private val database: AlphaAIDatabase) {
    suspend fun getLastSkillId(): String? {
        return database.userActionDao().getLastSuccessSkillId()
    }
}
