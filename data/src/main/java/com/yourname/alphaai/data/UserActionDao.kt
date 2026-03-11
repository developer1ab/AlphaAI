package com.yourname.alphaai.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserActionDao {
    @Insert
    suspend fun insert(action: UserAction)

    @Query("SELECT * FROM user_actions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentActions(limit: Int): List<UserAction>

    @Query("SELECT * FROM user_actions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentActionsLive(limit: Int): LiveData<List<UserAction>>

    @Query("SELECT COUNT(*) FROM user_actions WHERE skillId = :skillId AND success = 1")
    suspend fun getSkillSuccessCount(skillId: String): Int

    @Query("SELECT skillId FROM user_actions WHERE success = 1 AND skillId IS NOT NULL ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSuccessSkillId(): String?

    @Query("DELETE FROM user_actions")
    suspend fun clearAll()
}
