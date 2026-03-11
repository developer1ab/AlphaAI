package com.yourname.alphaai.recommendation

import java.time.LocalDateTime

object TimeContext {
    fun currentHour(): Int = LocalDateTime.now().hour
    fun currentMinute(): Int = LocalDateTime.now().minute
    fun dayOfWeek(): Int = LocalDateTime.now().dayOfWeek.value
}
