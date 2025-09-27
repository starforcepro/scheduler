package org.projects.scheduler

import java.time.Duration

data class ScheduleLambdaFunctionTask(
    val name: String,
    val jsonPayload: String,
    val repeatEvery: Duration? = null,
    val initialDelay: Duration = Duration.ZERO,
    val repetitions: Int = -1
)