package com.yourname.alphaai.core

import kotlinx.coroutines.flow.Flow

/**
 * Skill contract: smallest executable unit in the system.
 */
interface Skill {
    val id: String
    val name: String
    val description: String

    /**
     * Execute skill with key-value parameters.
     */
    suspend fun execute(params: Map<String, Any>): Result<Map<String, Any>>
}

/**
 * Scheduler contract: route intent text to a skill execution.
 */
interface Scheduler {
    /**
     * Submit an intent text.
     */
    suspend fun submit(intent: String): Flow<ExecutionEvent>
}

/**
 * Execution events for UI progress updates.
 */
sealed class ExecutionEvent {
    data class Started(val skillId: String) : ExecutionEvent()
    data class Completed(val result: Map<String, Any>) : ExecutionEvent()
    data class Failed(val error: String) : ExecutionEvent()
}
