package com.yourname.alphaai.accessibility

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.yourname.alphaai.core.Skill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class AccessibilitySkill(
    private val context: Context,
    private val enabled: Boolean
) : Skill {
    override val id = "accessibility.control"
    override val name = "Accessibility Control"
    override val description = "Optional high-risk screen simulation via Accessibility Service"

    override suspend fun execute(params: Map<String, Any>): Result<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            try {
                if (!enabled) {
                    return@withContext Result.failure(
                        IllegalStateException("Accessibility simulation is disabled in this build.")
                    )
                }

                val action = (params["action"] as? String)?.lowercase()
                    ?: return@withContext Result.failure(IllegalArgumentException("Missing action."))

                when (action) {
                    "open_settings" -> {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ContextCompat.startActivity(context, intent, null)
                        Result.success(mapOf("summary" to "Opened accessibility settings."))
                    }

                    "status" -> {
                        val service = AutoClickService.instance
                        val running = service != null
                        val activePackage = service?.activePackageName().orEmpty()
                        Result.success(
                            mapOf(
                                "running" to running,
                                "activePackage" to activePackage,
                                "summary" to if (running) {
                                    if (activePackage.isNotBlank()) {
                                        "Accessibility service is running. Active package: $activePackage"
                                    } else {
                                        "Accessibility service is running."
                                    }
                                } else {
                                    "Accessibility service is not running. Enable it in settings."
                                }
                            )
                        )
                    }

                    "click_by_text" -> {
                        val text = params["text"] as? String
                            ?: return@withContext Result.failure(IllegalArgumentException("Missing text."))
                        val service = AutoClickService.instance
                            ?: return@withContext Result.failure(
                                IllegalStateException("Accessibility service is not running.")
                            )
                        val delayMs = (params["delayMs"] as? Number)?.toLong() ?: 0L
                        if (delayMs > 0) {
                            delay(delayMs)
                        }
                        val clickResult = service.clickByText(text)
                        val summary = if (clickResult.success) {
                            val labelPart = clickResult.clickedLabel?.let { " label='$it'." } ?: ""
                            "Clicked by text '$text'.$labelPart matches=${clickResult.matchedCount}, clickableCandidates=${clickResult.clickableCandidates}."
                        } else {
                            val pkg = service.activePackageName().orEmpty()
                            val pkgPart = if (pkg.isNotBlank()) " activePackage=$pkg." else ""
                            "No clickable node found for '$text'. matches=${clickResult.matchedCount}, clickableCandidates=${clickResult.clickableCandidates}.$pkgPart"
                        }
                        Result.success(
                            mapOf(
                                "success" to clickResult.success,
                                "matchedCount" to clickResult.matchedCount,
                                "clickableCandidates" to clickResult.clickableCandidates,
                                "delayMs" to delayMs,
                                "summary" to summary
                            )
                        )
                    }

                    "click_by_coord" -> {
                        val x = (params["x"] as? Number)?.toFloat()
                            ?: return@withContext Result.failure(IllegalArgumentException("Missing x."))
                        val y = (params["y"] as? Number)?.toFloat()
                            ?: return@withContext Result.failure(IllegalArgumentException("Missing y."))
                        val service = AutoClickService.instance
                            ?: return@withContext Result.failure(
                                IllegalStateException("Accessibility service is not running.")
                            )
                        val delayMs = (params["delayMs"] as? Number)?.toLong() ?: 0L
                        if (delayMs > 0) {
                            delay(delayMs)
                        }
                        val ok = service.clickByCoordinates(x, y)
                        Result.success(
                            mapOf(
                                "success" to ok,
                                "delayMs" to delayMs,
                                "summary" to if (ok) "Clicked coordinates ($x, $y)." else "Coordinate click failed."
                            )
                        )
                    }

                    else -> Result.failure(IllegalArgumentException("Unsupported action: $action"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
