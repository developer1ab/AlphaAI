package com.yourname.alphaai.data

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UserAction::class, AppUsage::class, UserProfile::class, RecommendationLog::class],
    version = 2,
    exportSchema = false
)
abstract class AlphaAIDatabase : RoomDatabase() {
    abstract fun userActionDao(): UserActionDao
    abstract fun appUsageDao(): AppUsageDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun recommendationLogDao(): RecommendationLogDao

    companion object {
        @Volatile
        private var INSTANCE: AlphaAIDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recommendation_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ruleId TEXT NOT NULL,
                        ruleName TEXT NOT NULL,
                        content TEXT NOT NULL,
                        triggerTime INTEGER NOT NULL,
                        status TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AlphaAIDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AlphaAIDatabase::class.java,
                    "alphaai.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
