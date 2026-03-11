package com.yourname.alphaai.core

import android.content.Intent
import com.yourname.alphaai.data.AppHubDatabase
import com.yourname.alphaai.data.UserAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import java.text.Normalizer

class SimpleScheduler(
    private val skills: Map<String, Skill>,
    private val appResolver: AppResolver,
    private val database: AppHubDatabase
) : Scheduler {

    override suspend fun submit(intent: String): Flow<ExecutionEvent> = flow {
        val normalizedIntent = Normalizer.normalize(intent, Normalizer.Form.NFKC).trim()

        when {
            normalizedIntent.contains("toast", ignoreCase = true) -> {
                executeSkill("system.toast", mapOf("message" to normalizedIntent), normalizedIntent)
            }

            normalizedIntent.contains("拍照", ignoreCase = true) ||
                normalizedIntent.contains("photo", ignoreCase = true) -> {
                executeSkill("camera.take_photo", emptyMap(), normalizedIntent)
            }

            normalizedIntent.contains("位置", ignoreCase = true) ||
                normalizedIntent.contains("location", ignoreCase = true) -> {
                executeSkill("location.get", emptyMap(), normalizedIntent)
            }

            normalizedIntent.contains("通知", ignoreCase = true) ||
                normalizedIntent.contains("notification", ignoreCase = true) ||
                normalizedIntent.contains("notify", ignoreCase = true) -> {
                executeSkill(
                    "notification.show",
                    mapOf(
                        "title" to "AppHub",
                        "content" to "这是一条测试通知"
                    ),
                    normalizedIntent
                )
            }

            isDialIntent(normalizedIntent) -> {
                val number = extractPhoneNumber(normalizedIntent)
                if (number.isNullOrBlank()) {
                    emit(ExecutionEvent.Failed("未指定电话号码"))
                    logAction(normalizedIntent, null, false, "未指定电话号码")
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

            isShareIntent(normalizedIntent) -> {
                val shareTarget = extractShareTarget(normalizedIntent)
                val shareText = extractShareText(normalizedIntent)

                if (shareText.isBlank()) {
                    emit(ExecutionEvent.Failed("分享内容不能为空"))
                    logAction(normalizedIntent, null, false, "分享内容不能为空")
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

            isMapIntent(normalizedIntent) -> {
                val keyword = extractMapQuery(normalizedIntent)
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

            isOpenUrlIntent(normalizedIntent) -> {
                val url = extractUrl(normalizedIntent)
                if (url.isNullOrBlank()) {
                    executeSkill(
                        "intent.execute",
                        mapOf(
                            "action" to Intent.ACTION_VIEW,
                            "data" to "https://www.baidu.com"
                        ),
                        normalizedIntent
                    )
                } else {
                    val fullUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
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
            }

            isOpenAppIntent(normalizedIntent) -> {
                val appName = extractAppName(normalizedIntent)
                if (appName.isBlank()) {
                    emit(ExecutionEvent.Failed("请指定要打开的应用"))
                    logAction(normalizedIntent, null, false, "请指定要打开的应用")
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
                        emit(ExecutionEvent.Failed("未找到应用: $appName"))
                        logAction(normalizedIntent, "intent.execute", false, "未找到应用: $appName")
                    }
                }
            }

            else -> {
                emit(ExecutionEvent.Failed("无法理解意图: $normalizedIntent"))
                logAction(normalizedIntent, null, false, "无法理解意图")
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
                    val message = it.message ?: "未知错误"
                    emit(ExecutionEvent.Failed(message))
                    logAction(originalIntent, skillId, false, message)
                }
            )
        } else {
            emit(ExecutionEvent.Failed("未找到对应技能"))
            logAction(originalIntent, null, false, "未找到对应技能")
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
            // Learning log should never break main execution path.
        }
    }

    private fun isOpenAppIntent(intent: String): Boolean {
        return intent.startsWith("打开") || intent.startsWith("open ", ignoreCase = true)
    }

    private fun isOpenUrlIntent(intent: String): Boolean {
        return intent.startsWith("打开网址") ||
            intent.contains("浏览器") ||
            intent.startsWith("访问") ||
            intent.startsWith("open http", ignoreCase = true) ||
            intent.startsWith("visit ", ignoreCase = true)
    }

    private fun isDialIntent(intent: String): Boolean {
        return intent.contains("打电话") ||
            intent.contains("拨号") ||
            intent.startsWith("dial ", ignoreCase = true) ||
            intent.startsWith("call ", ignoreCase = true)
    }

    private fun isShareIntent(intent: String): Boolean {
        return intent.contains("分享") || intent.startsWith("share ", ignoreCase = true)
    }

    private fun isMapIntent(intent: String): Boolean {
        return intent.contains("地图") ||
            intent.startsWith("map ", ignoreCase = true) ||
            intent.startsWith("navigate ", ignoreCase = true)
    }

    private fun extractAppName(intent: String): String {
        return when {
            intent.startsWith("打开") -> intent.removePrefix("打开").trim()
            intent.startsWith("open ", ignoreCase = true) -> intent.substring(5).trim()
            else -> ""
        }
    }

    private fun extractUrl(intent: String): String {
        val regex = "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)".toRegex()
        val matched = regex.find(intent)?.value
        if (!matched.isNullOrBlank()) return matched

        return when {
            intent.startsWith("打开网址") -> intent.removePrefix("打开网址").trim()
            intent.startsWith("访问") -> intent.removePrefix("访问").trim()
            intent.startsWith("open ", ignoreCase = true) -> intent.substring(5).trim()
            intent.startsWith("visit ", ignoreCase = true) -> intent.substring(6).trim()
            else -> ""
        }
    }

    private fun extractPhoneNumber(intent: String): String? {
        val regex = "(\\d{5,})".toRegex()
        return regex.find(intent)?.value
    }

    private fun extractShareText(intent: String): String {
        if (intent.startsWith("share ", ignoreCase = true)) {
            val body = intent.substring(6).trim()
            val toIndex = body.lastIndexOf(" to ")
            return if (toIndex > 0) body.substring(0, toIndex).trim() else body
        }

        if (intent.startsWith("分享")) {
            val body = intent.removePrefix("分享").trim()
            val toIndex = body.lastIndexOf("到")
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

        if (intent.startsWith("分享")) {
            val body = intent.removePrefix("分享").trim()
            val toIndex = body.lastIndexOf("到")
            if (toIndex > 0 && toIndex + 1 < body.length) {
                return body.substring(toIndex + 1).trim()
            }
        }

        return null
    }

    private fun extractMapQuery(intent: String): String {
        return when {
            intent.startsWith("map ", ignoreCase = true) -> intent.substring(4).trim()
            intent.startsWith("navigate ", ignoreCase = true) -> intent.substring(9).trim()
            intent.contains("地图") -> intent.substringAfter("地图").trim()
            else -> ""
        }
    }
}
