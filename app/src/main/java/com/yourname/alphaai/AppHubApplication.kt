package com.yourname.alphaai

import android.app.Application
import com.yourname.alphaai.data.AppHubDatabase
import com.yourname.alphaai.recommendation.RecommendationScheduler

class AppHubApplication : Application() {
    val database: AppHubDatabase by lazy { AppHubDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        RecommendationScheduler.schedule(this)
    }
}
