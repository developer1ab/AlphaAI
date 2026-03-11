package com.yourname.alphaai.data

class ProfileGenerator(private val database: AlphaAIDatabase) {

    suspend fun generateBasicProfile() {
        val recent = database.userActionDao().getRecentActions(200)
        if (recent.isEmpty()) return

        val topSkill = recent
            .asSequence()
            .filter { it.success && !it.skillId.isNullOrBlank() }
            .groupingBy { it.skillId!! }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        val peakHour = recent
            .groupingBy { ((it.timestamp / (1000 * 60 * 60)) % 24).toInt() }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        if (topSkill != null) {
            database.userProfileDao().insertOrUpdate(
                UserProfile("top_skill", topSkill)
            )
        }

        if (peakHour != null) {
            database.userProfileDao().insertOrUpdate(
                UserProfile("peak_hour", peakHour.toString())
            )
        }
    }
}
