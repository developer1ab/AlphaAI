package com.yourname.alphaai.skills

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.yourname.alphaai.core.Skill

class NotificationSkill(private val context: Context) : Skill {
    override val id = "notification.show"
    override val name = "Show notification"
    override val description = "Display a status bar notification"

    private val channelId = "alphaai_notifications"
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        // Android 8.0+ requires a notification channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AlphaAI Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    override suspend fun execute(params: Map<String, Any>): Result<Map<String, Any>> {
        return try {
            val title = params["title"] as? String ?: "AlphaAI"
            val content = params["content"] as? String ?: "You have a new message."

            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1, notification)
            Result.success(mapOf("success" to true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
