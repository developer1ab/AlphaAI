package com.yourname.alphaai

import android.app.Application
import com.yourname.alphaai.data.AlphaAIDatabase
import com.yourname.alphaai.recommendation.RecommendationScheduler

class AlphaAIApplication : Application() {
    val database: AlphaAIDatabase by lazy { AlphaAIDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        RecommendationScheduler.schedule(this)
    }
}
