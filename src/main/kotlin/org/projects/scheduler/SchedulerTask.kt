package org.projects.scheduler

import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.Delayed
import java.util.concurrent.TimeUnit

data class SchedulerTask(
    val id: UUID,
    val name: String,
    val jsonPayload: String,
    val status: LambdaStatus,
    val scheduledAt: LocalDateTime,
    val executedAt: LocalDateTime? = null,
    val repeatEvery: Duration? = null,
    val totalRunsNumber: Int,
    val result: String? = null
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