package com.yourname.alphaai.skills

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.yourname.alphaai.core.Skill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IntentSkill(private val context: Context) : Skill {
    override val id = "intent.execute"
    override val name = "执行Intent操作"
    override val description = "通过Intent打开应用、网址、拨号、分享等"

    override suspend fun execute(params: Map<String, Any>): Result<Map<String, Any>> {
        return try {
            val intent = buildIntent(params)
            val packageManager = context.packageManager
            if (intent.resolveActivity(packageManager) == null) {
                return Result.failure(Exception("没有应用可以处理该操作"))
            }

            if ((intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) == 0) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            withContext(Dispatchers.Main) {
                ContextCompat.startActivity(context, intent, null)
            }
            Result.success(
                mapOf(
                    "success" to true,
                    "action" to (intent.action ?: ""),
                    "package" to (intent.`package` ?: ""),
                    "data" to (intent.dataString ?: "")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildIntent(params: Map<String, Any>): Intent {
        val action = params["action"] as? String
        val dataStr = params["data"] as? String
        val type = params["type"] as? String
        val packageName = params["package"] as? String
        val extras = params["extras"] as? Map<*, *>
        val categories = params["categories"] as? List<*>
        val flags = params["flags"] as? Int

        val intent = if (action != null) Intent(action) else Intent()

        if (dataStr != null && type != null) {
            intent.setDataAndType(Uri.parse(dataStr), type)
        } else {
            dataStr?.let { intent.data = Uri.parse(it) }
            type?.let { intent.type = it }
        }

        packageName?.let { intent.setPackage(it) }

        categories?.forEach { category ->
            if (category is String) {
                intent.addCategory(category)
            }
        }

        extras?.let { map ->
            val bundle = Bundle()
            map.forEach { (key, value) ->
                val safeKey = key as? String ?: return@forEach
                when (value) {
                    is String -> bundle.putString(safeKey, value)
                    is Int -> bundle.putInt(safeKey, value)
                    is Boolean -> bundle.putBoolean(safeKey, value)
                    is Long -> bundle.putLong(safeKey, value)
                    is Float -> bundle.putFloat(safeKey, value)
                    is Double -> bundle.putDouble(safeKey, value)
                }
            }
            intent.putExtras(bundle)
        }

        flags?.let { intent.addFlags(it) }
        return intent
    }
}
