package com.yourname.alphaai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfile)

    @Query("SELECT * FROM user_profile WHERE `key` = :key")
    suspend fun get(key: String): UserProfile?

    @Query("DELETE FROM user_profile")
    suspend fun clearAll()
}
