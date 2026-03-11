package com.yourname.alphaai.recommendation

import java.time.LocalTime

class RuleMatcher(
    private val locationContext: LocationContext,
    private val recentActionContext: RecentActionContext
) {

    suspend fun matches(rule: RecommendationRule): Boolean {
        val condition = rule.condition

        condition.timeRange?.let { range ->
            if (range.size >= 2) {
                val now = LocalTime.now()
                val start = LocalTime.parse(range[0])
                val end = LocalTime.parse(range[1])
                val inRange = if (end.isBefore(start)) {
                    now >= start || now <= end
                } else {
                    now >= start && now <= end
                }
                if (!inRange) return false
            }
        }

        condition.dayOfWeek?.let { days ->
            val today = TimeContext.dayOfWeek()
            if (today !in days) return false
        }

        condition.location?.let { expected ->
            val current = locationContext.getPlaceType()
            if (current != expected) return false
        }

        condition.lastActionSkill?.let { expected ->
            val last = recentActionContext.getLastSkillId()
            if (last != expected) return false
        }

        return true
    }
}
