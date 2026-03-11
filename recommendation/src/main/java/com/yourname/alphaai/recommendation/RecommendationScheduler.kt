package com.yourname.alphaai.recommendation

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object RecommendationScheduler {
    private const val WORK_NAME = "recommendation_periodic_work"
    private const val DEBUG_WORK_NAME = "recommendation_debug_work"

    fun schedule(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<RecommendationWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun triggerNow(context: Context) {
        val oneTime = OneTimeWorkRequestBuilder<RecommendationWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            DEBUG_WORK_NAME,
            androidx.work.ExistingWorkPolicy.REPLACE,
            oneTime
        )
    }
}
