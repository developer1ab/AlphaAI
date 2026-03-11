package com.yourname.alphaai.recommendation

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object RuleLoader {
    fun loadRules(context: Context): List<RecommendationRule> {
        return try {
            val json = context.assets.open("recommendations.json").bufferedReader().use { it.readText() }
            parseRules(JSONArray(json))
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseRules(array: JSONArray): List<RecommendationRule> {
        val rules = mutableListOf<RecommendationRule>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            rules.add(parseRule(obj))
        }
        return rules
    }

    private fun parseRule(obj: JSONObject): RecommendationRule {
        val conditionObj = obj.optJSONObject("condition") ?: JSONObject()
        val actionObj = obj.optJSONObject("action") ?: JSONObject()
        val feedbackObj = obj.optJSONObject("feedbackWeights") ?: JSONObject()

        return RecommendationRule(
            id = obj.optString("id"),
            name = obj.optString("name"),
            description = obj.optString("description"),
            condition = Condition(
                timeRange = conditionObj.optJSONArray("timeRange")?.toStringList(),
                dayOfWeek = conditionObj.optJSONArray("dayOfWeek")?.toIntList(),
                location = conditionObj.optString("location").ifBlank { null },
                lastActionSkill = conditionObj.optString("lastActionSkill").ifBlank { null },
                appForeground = conditionObj.optString("appForeground").ifBlank { null }
            ),
            action = Action(
                type = actionObj.optString("type", "suggest"),
                content = actionObj.optString("content", ""),
                intent = actionObj.optString("intent", ""),
                icon = actionObj.optString("icon").ifBlank { null }
            ),
            weight = obj.optDouble("weight", 1.0).toFloat(),
            feedbackWeights = FeedbackWeights(
                accept = feedbackObj.optDouble("accept", 0.1).toFloat(),
                ignore = feedbackObj.optDouble("ignore", -0.02).toFloat(),
                dismiss = feedbackObj.optDouble("dismiss", -0.05).toFloat()
            )
        )
    }

    private fun JSONArray.toStringList(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until length()) {
            list.add(optString(i))
        }
        return list
    }

    private fun JSONArray.toIntList(): List<Int> {
        val list = mutableListOf<Int>()
        for (i in 0 until length()) {
            list.add(optInt(i))
        }
        return list
    }
}
