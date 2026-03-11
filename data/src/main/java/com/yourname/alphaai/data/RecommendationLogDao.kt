package com.yourname.alphaai.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RecommendationLogDao {
    @Insert
    suspend fun insert(log: RecommendationLog)

    @Query("SELECT * FROM recommendation_logs ORDER BY triggerTime DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int): List<RecommendationLog>

    @Query("SELECT * FROM recommendation_logs ORDER BY triggerTime DESC LIMIT :limit")
    fun getRecentLogsLive(limit: Int): LiveData<List<RecommendationLog>>

    @Query("DELETE FROM recommendation_logs")
    suspend fun clearAll()
}
