package com.yourname.alphaai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AppUsageDao {
    @Insert
    suspend fun insert(usage: AppUsage)

    @Update
    suspend fun update(usage: AppUsage)

    @Query("SELECT * FROM app_usage WHERE endTime IS NULL")
    suspend fun getCurrentUsage(): AppUsage?

    @Query("SELECT packageName, COUNT(*) as launchCount FROM app_usage GROUP BY packageName ORDER BY launchCount DESC LIMIT 5")
    suspend fun getMostUsedApps(): List<PackageLaunchCount>

    @Query("DELETE FROM app_usage")
    suspend fun clearAll()
}

data class PackageLaunchCount(
    val packageName: String,
    val launchCount: Int
)
