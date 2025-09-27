package org.projects.scheduler

import java.time.Duration
import java.time.LocalDateTime
import java.util.*

data class SchedulerTask(
    val id: UUID,
    val name: String,
    val jsonPayload: String,
    val status: LambdaStatus,
    val scheduledAt: LocalDateTime,
    val executedAt: LocalDateTime? = null,
    val repeatEvery: Duration? = null,
    val repetitions: Int,
    val result: String? = null
)

enum class LambdaStatus {
    NEW, RUNNING, COMPLETED, FAILED
}