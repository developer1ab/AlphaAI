package com.yourname.alphaai.recommendation

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourname.alphaai.data.AlphaAIDatabase
import com.yourname.alphaai.data.RecommendationLog

class RecommendationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AlphaAIDatabase.getInstance(applicationContext)
        val rules = RuleLoader.loadRules(applicationContext)
        if (rules.isEmpty()) return Result.success()

        val matcher = RuleMatcher(
            locationContext = LocationContext(applicationContext),
            recentActionContext = RecentActionContext(database)
        )

        val selected = rules
            .sortedByDescending { it.weight }
            .firstOrNull { matcher.matches(it) }

        if (selected != null) {
            RecommendationNotifier.showRecommendation(applicationContext, selected.action)
            database.recommendationLogDao().insert(
                RecommendationLog(
                    ruleId = selected.id,
                    ruleName = selected.name,
                    content = selected.action.content,
                    status = "shown"
                )
            )
            database.userProfileDao().insertOrUpdate(
                com.yourname.alphaai.data.UserProfile("last_recommendation_rule", selected.id)
            )
        }

        return Result.success()
    }
}
