package org.projects.scheduler

import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.Delayed
import java.util.concurrent.TimeUnit

data class SchedulerTaskDescription (
    val name: String,
    val repeatEvery: Duration? = null,
    val initialDelay: Duration = Duration.ZERO,
    val runsLeft: Int = -1,
    val cancelled: Boolean = false,
)

data class SchedulerTask(
    val id: UUID,
    val name: String,
    val status: LambdaStatus,
    val scheduledAt: LocalDateTime,
    val startedAt: LocalDateTime? = null,
    val executedAt: LocalDateTime? = null,
    val jsonPayload: String,
    val result: String? = null,
) : Delayed {
    override fun getDelay(unit: TimeUnit): Long {
        val now = LocalDateTime.now()
        val until = now.until(scheduledAt, ChronoUnit.NANOS)
        return unit.convert(until, TimeUnit.NANOSECONDS)
    }

    override fun compareTo(other: Delayed): Int {
        val o = other as SchedulerTask
        return this.scheduledAt.compareTo(o.scheduledAt)
    }
}

enum class LambdaStatus {
    SCHEDULED, RUNNING, COMPLETED, FAILED
}

fun SchedulerTask.isTerminal() = this.status in listOf(LambdaStatus.COMPLETED, LambdaStatus.FAILED)