package com.yourname.alphaai.core

import kotlinx.coroutines.flow.Flow

/**
 * 技能接口：所有可执行操作的最小单元
 */
interface Skill {
    val id: String
    val name: String
    val description: String

    /**
     * 执行技能
     * @param params 输入参数（JSON格式，可后续扩展）
     * @return 执行结果（JSON格式）
     */
    suspend fun execute(params: Map<String, Any>): Result<Map<String, Any>>
}

/**
 * 调度器接口：负责将意图映射到技能并执行
 */
interface Scheduler {
    /**
     * 提交一个任务（意图文本）
     */
    suspend fun submit(intent: String): Flow<ExecutionEvent>
}

/**
 * 执行事件：用于向UI反馈进度
 */
sealed class ExecutionEvent {
    data class Started(val skillId: String) : ExecutionEvent()
    data class Completed(val result: Map<String, Any>) : ExecutionEvent()
    data class Failed(val error: String) : ExecutionEvent()
}
