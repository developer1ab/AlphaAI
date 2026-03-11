package com.yourname.alphaai.recommendation

import com.yourname.alphaai.data.AppHubDatabase

class RecentActionContext(private val database: AppHubDatabase) {
    suspend fun getLastSkillId(): String? {
        return database.userActionDao().getLastSuccessSkillId()
    }
}
