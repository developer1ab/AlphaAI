package com.yourname.alphaai.recommendation

data class RecommendationRule(
    val id: String,
    val name: String,
    val description: String,
    val condition: Condition,
    val action: Action,
    var weight: Float = 1.0f,
    val feedbackWeights: FeedbackWeights = FeedbackWeights(0.1f, -0.02f, -0.05f)
)

data class Condition(
    val timeRange: List<String>? = null,
    val dayOfWeek: List<Int>? = null,
    val location: String? = null,
    val lastActionSkill: String? = null,
    val appForeground: String? = null
)

data class Action(
    val type: String,
    val content: String,
    val intent: String,
    val icon: String? = null
)

data class FeedbackWeights(
    val accept: Float,
    val ignore: Float,
    val dismiss: Float
)
