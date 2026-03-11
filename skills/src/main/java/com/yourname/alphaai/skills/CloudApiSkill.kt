package com.yourname.alphaai.skills

import com.yourname.alphaai.core.Skill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class CloudApiSkill(
    private val gatewayBaseUrl: String,
    private val userApiKey: String
) : Skill {
    override val id = "cloud.api"
    override val name = "Cloud API"
    override val description = "Call third-party cloud services through self-hosted API gateway"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .dns(Ipv4FirstDns)
            .build()
    }

    override suspend fun execute(params: Map<String, Any>): Result<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            try {
                if (gatewayBaseUrl.isBlank()) {
                    return@withContext Result.failure(
                        IllegalStateException("Gateway base URL is empty. Configure CLOUD_GATEWAY_BASE_URL.")
                    )
                }
                if (userApiKey.isBlank()) {
                    return@withContext Result.failure(
                        IllegalStateException("Gateway API key is empty. Configure CLOUD_GATEWAY_API_KEY.")
                    )
                }

                val service = params["service"] as? String
                    ?: return@withContext Result.failure(IllegalArgumentException("Missing service."))
                val endpoint = params["endpoint"] as? String
                    ?: return@withContext Result.failure(IllegalArgumentException("Missing endpoint."))
                val method = (params["method"] as? String)?.uppercase() ?: "POST"
                val requestParams = (params["params"] as? Map<*, *>)
                    ?.filterKeys { it is String }
                    ?.mapKeys { it.key as String }
                    ?.mapValues { it.value }
                    ?: emptyMap()

                val payload = JSONObject().apply {
                    put("service", service)
                    put("endpoint", endpoint)
                    put("method", method)
                    put("params", JSONObject(requestParams))
                }

                val targetUrl = gatewayBaseUrl.trimEnd('/') + "/api"
                val request = Request.Builder()
                    .url(targetUrl)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-API-Key", userApiKey)
                    .build()

                val response = httpClient.newCall(request).execute()
                val statusCode = response.code
                val responseText = response.body?.string().orEmpty()

                if (statusCode !in 200..299) {
                    return@withContext Result.failure(
                        IllegalStateException("Gateway request failed: HTTP $statusCode, body=$responseText")
                    )
                }

                val json = if (responseText.isBlank()) JSONObject() else JSONObject(responseText)
                val result = mutableMapOf<String, Any>(
                    "status" to statusCode,
                    "raw" to responseText
                )
                json.keys().forEach { key ->
                    val value = json.opt(key)
                    if (value != null) {
                        result[key] = value.toString()
                    }
                }

                if (service.equals("weather", ignoreCase = true)) {
                    val summary = buildWeatherSummary(json)
                    if (!summary.isNullOrBlank()) {
                        result["summary"] = summary
                    }
                }
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun buildWeatherSummary(json: JSONObject): String? {
        val city = json.optString("name").takeIf { it.isNotBlank() } ?: return null
        val main = json.optJSONObject("main")
        val weatherArray = json.optJSONArray("weather")
        val description = weatherArray?.firstDescription()?.ifBlank { null }

        val temp = if (main != null && main.has("temp")) main.optDouble("temp") else Double.NaN
        val tempText = if (temp.isNaN()) null else String.format("%.1f", temp)

        return when {
            tempText != null && description != null -> "$city: $tempText C, $description"
            tempText != null -> "$city: $tempText C"
            description != null -> "$city: $description"
            else -> city
        }
    }

    private fun JSONArray.firstDescription(): String {
        if (length() == 0) return ""
        val first = optJSONObject(0) ?: return ""
        return first.optString("description").trim()
    }

    private object Ipv4FirstDns : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val all = Dns.SYSTEM.lookup(hostname)
            val ipv4 = all.filterIsInstance<Inet4Address>()
            return if (ipv4.isNotEmpty()) ipv4 else all
        }
    }
}
