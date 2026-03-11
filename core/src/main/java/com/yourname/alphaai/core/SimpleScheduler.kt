package com.yourname.alphaai.core

import android.content.Intent
import com.yourname.alphaai.data.AlphaAIDatabase
import com.yourname.alphaai.data.UserAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import java.text.Normalizer

class SimpleScheduler(
    private val skills: Map<String, Skill>,
    private val appResolver: AppResolver,
    private val database: AlphaAIDatabase
) : Scheduler {

    override suspend fun submit(intent: String): Flow<ExecutionEvent> = flow {
        val normalizedIntent = Normalizer.normalize(intent, Normalizer.Form.NFKC).trim()
        val parsedIntent = normalizedIntent
            .replace(Regex("[;,，；]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        when {
            parsedIntent.contains("toast", ignoreCase = true) -> {
                executeSkill("system.toast", mapOf("message" to parsedIntent), normalizedIntent)
            }

            parsedIntent.contains("photo", ignoreCase = true) ||
                parsedIntent.contains("camera", ignoreCase = true) -> {
                executeSkill("camera.take_photo", emptyMap(), normalizedIntent)
            }

            parsedIntent.contains("location", ignoreCase = true) ||
                parsedIntent.contains("where", ignoreCase = true) -> {
                executeSkill("location.get", emptyMap(), normalizedIntent)
            }

            parsedIntent.contains("notification", ignoreCase = true) ||
                parsedIntent.contains("notify", ignoreCase = true) -> {
                executeSkill(
                    "notification.show",
                    mapOf(
                        "title" to "AlphaAI",
                        "content" to "This is a test notification."
                    ),
                    normalizedIntent
                )
            }

            isDialIntent(parsedIntent) -> {
                val number = extractPhoneNumber(parsedIntent)
                if (number.isNullOrBlank()) {
                    emit(ExecutionEvent.Failed("Phone number is missing."))
                    logAction(normalizedIntent, null, false, "Phone number is missing.")
                } else {
                    executeSkill(
                        "intent.execute",
                        mapOf(
                            "action" to Intent.ACTION_DIAL,
                            "data" to "tel:$number"
                        ),
                        normalizedIntent
                    )
                }
            }

            isShareIntent(parsedIntent) -> {
                val shareTarget = extractShareTarget(parsedIntent)
                val shareText = extractShareText(parsedIntent)

                if (shareText.isBlank()) {
                    emit(ExecutionEvent.Failed("Share content cannot be empty."))
                    logAction(normalizedIntent, null, false, "Share content cannot be empty.")
                } else {
                    val baseParams = mutableMapOf<String, Any>(
                        "action" to Intent.ACTION_SEND,
                        "type" to "text/plain",
                        "extras" to mapOf(Intent.EXTRA_TEXT to shareText)
                    )

                    if (!shareTarget.isNullOrBlank()) {
                        val packageName = appResolver.resolvePackageName(shareTarget)
                        if (packageName != null) {
                            baseParams["package"] = packageName
                        }
                    }

                    executeSkill("intent.execute", baseParams, normalizedIntent)
                }
            }

            isMapIntent(parsedIntent) -> {
                val keyword = extractMapQuery(parsedIntent)
                val geo = if (keyword.isBlank()) {
                    "geo:0,0"
                } else {
                    "geo:0,0?q=$keyword"
                }
                executeSkill(
                    "intent.execute",
                    mapOf(
                        "action" to Intent.ACTION_VIEW,
                        "data" to geo
                    ),
                    normalizedIntent
                )
            }

            isAccessibilityIntent(parsedIntent) -> {
                executeSkill("accessibility.control", buildAccessibilityParams(parsedIntent), normalizedIntent)
            }

            isCloudIntent(parsedIntent) -> {
                executeSkill("cloud.api", buildCloudApiParams(parsedIntent), normalizedIntent)
            }

            isOpenUrlIntent(parsedIntent) -> {
                val url = extractUrl(parsedIntent)
                val fullUrl = if (url.isNullOrBlank()) {
                    "https://www.google.com"
                } else if (url.startsWith("http://") || url.startsWith("https://")) {
                    url
                } else {
                    "https://$url"
                }

                executeSkill(
                    "intent.execute",
                    mapOf(
                        "action" to Intent.ACTION_VIEW,
                        "data" to fullUrl
                    ),
                    normalizedIntent
                )
            }

            isOpenAppIntent(parsedIntent) -> {
                val appName = extractAppName(parsedIntent)
                if (appName.isBlank()) {
                    emit(ExecutionEvent.Failed("Please specify an app to open."))
                    logAction(normalizedIntent, null, false, "Please specify an app to open.")
                } else {
                    val packageName = appResolver.findPackageByAppName(appName)
                    if (packageName != null) {
                        executeSkill(
                            "intent.execute",
                            mapOf(
                                "package" to packageName,
                                "action" to Intent.ACTION_MAIN,
                                "categories" to listOf(Intent.CATEGORY_LAUNCHER)
                            ),
                            normalizedIntent
                        )
                    } else {
                        emit(ExecutionEvent.Failed("App not found: $appName"))
                        logAction(normalizedIntent, "intent.execute", false, "App not found: $appName")
                    }
                }
            }

            else -> {
                emit(ExecutionEvent.Failed("Unable to understand intent: $normalizedIntent"))
                logAction(normalizedIntent, null, false, "Unable to understand intent")
            }
        }
    }

    private suspend fun FlowCollector<ExecutionEvent>.executeSkill(
        skillId: String,
        params: Map<String, Any>,
        originalIntent: String
    ) {
        val skill = skills[skillId]
        if (skill != null) {
            emit(ExecutionEvent.Started(skill.id))
            val result = skill.execute(params)
            result.fold(
                onSuccess = {
                    emit(ExecutionEvent.Completed(it))
                    logAction(originalIntent, skillId, true, it.toString())
                },
                onFailure = {
                    val message = it.message ?: "Unknown error"
                    emit(ExecutionEvent.Failed(message))
                    logAction(originalIntent, skillId, false, message)
                }
            )
        } else {
            emit(ExecutionEvent.Failed("Matching skill not found."))
            logAction(originalIntent, null, false, "Matching skill not found.")
        }
    }

    private suspend fun logAction(
        intent: String,
        skillId: String?,
        success: Boolean,
        resultMessage: String?
    ) {
        try {
            database.userActionDao().insert(
                UserAction(
                    intent = intent,
                    skillId = skillId,
                    success = success,
                    resultMessage = resultMessage
                )
            )
        } catch (_: Exception) {
            // Learning log should never break the main execution path.
        }
    }

    private fun isOpenAppIntent(intent: String): Boolean {
        return intent.startsWith("open ", ignoreCase = true) &&
            !intent.startsWith("open http", ignoreCase = true) &&
            !intent.startsWith("open url ", ignoreCase = true)
    }

    private fun isOpenUrlIntent(intent: String): Boolean {
        return intent.startsWith("open url ", ignoreCase = true) ||
            intent.startsWith("open http", ignoreCase = true) ||
            intent.startsWith("visit ", ignoreCase = true) ||
            (intent.startsWith("open ", ignoreCase = true) &&
                looksLikeUrlCandidate(intent.substring(5).trim()))
    }

    private fun looksLikeUrlCandidate(value: String): Boolean {
        val candidate = value.trim()
        if (candidate.isBlank()) return false
        if (candidate.startsWith("http://", ignoreCase = true) ||
            candidate.startsWith("https://", ignoreCase = true)
        ) {
            return true
        }

        val hostLikePattern = "^[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+([/:?#].*)?$".toRegex()
        return hostLikePattern.matches(candidate)
    }

    private fun isDialIntent(intent: String): Boolean {
        return intent.startsWith("dial ", ignoreCase = true) ||
            intent.startsWith("call ", ignoreCase = true) ||
            intent.contains("phone", ignoreCase = true)
    }

    private fun isShareIntent(intent: String): Boolean {
        return intent.startsWith("share ", ignoreCase = true) ||
            intent.contains("share", ignoreCase = true)
    }

    private fun isMapIntent(intent: String): Boolean {
        return intent.startsWith("map ", ignoreCase = true) ||
            intent.startsWith("navigate ", ignoreCase = true) ||
            intent.contains("map", ignoreCase = true)
    }

    private fun isAccessibilityIntent(intent: String): Boolean {
        return intent.startsWith("access", ignoreCase = true)
    }

    private fun isCloudIntent(intent: String): Boolean {
        return intent.startsWith("cloud", ignoreCase = true) ||
            intent.startsWith("weather", ignoreCase = true) ||
            intent.startsWith("translate", ignoreCase = true) ||
            intent.startsWith("news", ignoreCase = true) ||
            intent.startsWith("stock", ignoreCase = true)
    }

    private fun extractAppName(intent: String): String {
        return if (intent.startsWith("open ", ignoreCase = true)) {
            intent.substring(5).trim()
        } else {
            ""
        }
    }

    private fun extractUrl(intent: String): String {
        val regex = "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)".toRegex()
        val matched = regex.find(intent)?.value
        if (!matched.isNullOrBlank()) return matched

        return when {
            intent.startsWith("open url ", ignoreCase = true) -> intent.substring(9).trim()
            intent.startsWith("visit ", ignoreCase = true) -> intent.substring(6).trim()
            intent.startsWith("open ", ignoreCase = true) -> intent.substring(5).trim()
            else -> ""
        }
    }

    private fun extractPhoneNumber(intent: String): String? {
        val regex = "(\\d{3,})".toRegex()
        return regex.find(intent)?.value
    }

    private fun extractShareText(intent: String): String {
        if (intent.startsWith("share ", ignoreCase = true)) {
            val body = intent.substring(6).trim()
            val toIndex = body.lastIndexOf(" to ")
            return if (toIndex > 0) body.substring(0, toIndex).trim() else body
        }
        return ""
    }

    private fun extractShareTarget(intent: String): String? {
        if (intent.startsWith("share ", ignoreCase = true)) {
            val body = intent.substring(6).trim()
            val toIndex = body.lastIndexOf(" to ")
            if (toIndex > 0 && toIndex + 4 < body.length) {
                return body.substring(toIndex + 4).trim()
            }
        }
        return null
    }

    private fun extractMapQuery(intent: String): String {
        return when {
            intent.startsWith("map ", ignoreCase = true) -> intent.substring(4).trim()
            intent.startsWith("navigate ", ignoreCase = true) -> intent.substring(9).trim()
            intent.contains("map", ignoreCase = true) -> intent.substringAfter("map", "").trim()
            else -> ""
        }
    }

    private fun buildCloudApiParams(intent: String): Map<String, Any> {
        val tokens = intent.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) {
            return mapOf(
                "service" to "weather",
                "endpoint" to "weather",
                "method" to "GET",
                "params" to mapOf("q" to "London", "units" to "metric")
            )
        }

        val head = tokens.first().lowercase()

        if (head == "weather") {
            val city = tokens.drop(1).joinToString(" ").ifBlank { "London" }
            return mapOf(
                "service" to "weather",
                "endpoint" to "weather",
                "method" to "GET",
                "params" to mapOf(
                    "q" to city,
                    "units" to "metric"
                )
            )
        }

        if (head == "translate") {
            val body = tokens.drop(1).joinToString(" ")
            val toIndex = body.lastIndexOf(" to ")
            val text = if (toIndex > 0) body.substring(0, toIndex).trim() else body
            val target = if (toIndex > 0 && toIndex + 4 < body.length) body.substring(toIndex + 4).trim() else "en"
            return mapOf(
                "service" to "translate",
                "endpoint" to "text",
                "params" to mapOf("text" to text, "target" to target)
            )
        }

        if (head == "news") {
            val topic = tokens.drop(1).joinToString(" ")
            return mapOf(
                "service" to "news",
                "endpoint" to "summary",
                "method" to "GET",
                "params" to mapOf("q" to topic)
            )
        }

        if (head == "stock") {
            val symbol = tokens.drop(1).joinToString(" ")
            return mapOf(
                "service" to "stock",
                "endpoint" to "quote",
                "method" to "GET",
                "params" to mapOf("symbol" to symbol)
            )
        }

        if (head == "cloud") {
            val service = tokens.getOrNull(1).orEmpty()
            val endpoint = tokens.getOrNull(2).orEmpty()
            val raw = if (tokens.size > 3) tokens.subList(3, tokens.size).joinToString(" ") else ""
            return mapOf(
                "service" to service,
                "endpoint" to endpoint,
                "params" to mapOf("raw" to raw)
            )
        }

        return mapOf(
            "service" to "weather",
            "endpoint" to "current",
            "params" to emptyMap<String, String>()
        )
    }

    private fun buildAccessibilityParams(intent: String): Map<String, Any> {
        val tokens = intent.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.size == 1) {
            return mapOf("action" to "status")
        }

        if (tokens.getOrNull(1).equals("settings", ignoreCase = true)) {
            return mapOf("action" to "open_settings")
        }

        if (tokens.getOrNull(1).equals("status", ignoreCase = true)) {
            return mapOf("action" to "status")
        }

        if (tokens.getOrNull(1).equals("click", ignoreCase = true)) {
            if (tokens.getOrNull(2).equals("text", ignoreCase = true)) {
                val delaySec = parseDelaySeconds(tokens)
                val textTokens = trimTrailingDelayClause(tokens.drop(3))
                val text = textTokens.joinToString(" ")
                return mapOf(
                    "action" to "click_by_text",
                    "text" to text,
                    "delayMs" to delaySec * 1000L
                )
            }
            if (tokens.getOrNull(2).equals("coord", ignoreCase = true)) {
                val x = tokens.getOrNull(3)?.toFloatOrNull()
                val y = tokens.getOrNull(4)?.toFloatOrNull()
                if (x != null && y != null) {
                    val delaySec = parseDelaySeconds(tokens)
                    return mapOf(
                        "action" to "click_by_coord",
                        "x" to x,
                        "y" to y,
                        "delayMs" to delaySec * 1000L
                    )
                }
            }
        }

        return mapOf("action" to "status")
    }

    private fun parseDelaySeconds(tokens: List<String>): Long {
        val inIndex = tokens.indexOfLast { it.equals("in", ignoreCase = true) }
        if (inIndex < 0 || inIndex + 1 >= tokens.size) return 0L
        return tokens[inIndex + 1].toLongOrNull()?.coerceAtLeast(0L) ?: 0L
    }

    private fun trimTrailingDelayClause(tokens: List<String>): List<String> {
        if (tokens.size < 3) return tokens
        val inIndex = tokens.indexOfLast { it.equals("in", ignoreCase = true) }
        if (inIndex < 0 || inIndex + 1 >= tokens.size) return tokens
        if (tokens[inIndex + 1].toLongOrNull() == null) return tokens
        return tokens.subList(0, inIndex)
    }
}
