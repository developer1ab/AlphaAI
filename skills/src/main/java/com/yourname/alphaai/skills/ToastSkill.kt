package com.yourname.alphaai.skills

import android.content.Context
import android.widget.Toast
import com.yourname.alphaai.core.Skill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ToastSkill(private val context: Context) : Skill {
    override val id = "system.toast"
    override val name = "显示提示"
    override val description = "在屏幕上显示一条短暂的消息"

    override suspend fun execute(params: Map<String, Any>): Result<Map<String, Any>> {
        return try {
            val message = params["message"] as? String ?: "默认消息"
            withContext(Dispatchers.Main) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            Result.success(mapOf("success" to true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
