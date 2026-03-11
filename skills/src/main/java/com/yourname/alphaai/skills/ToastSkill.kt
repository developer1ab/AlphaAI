package com.yourname.alphaai.skills

import android.content.Context
import android.widget.Toast
import com.yourname.alphaai.core.Skill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ToastSkill(private val context: Context) : Skill {
    override val id = "system.toast"
    override val name = "Show toast"
    override val description = "Display a short on-screen message"

    override suspend fun execute(params: Map<String, Any>): Result<Map<String, Any>> {
        return try {
            val message = params["message"] as? String ?: "Default message"
            withContext(Dispatchers.Main) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            Result.success(mapOf("success" to true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
