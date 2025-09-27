package org.projects.scheduler

import software.amazon.awssdk.services.lambda.model.PackageType
import software.amazon.awssdk.services.lambda.model.Runtime
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AwsLambdaScheduler(
    val taskRepository: TaskRepository,
    val taskExecutor: TaskExecutor,
    val lambdaFunctionHandler: LambdaFunctionHandler
) : AutoCloseable {
    private val executor = Executors.newSingleThreadExecutor()

    init {
        executor.submit { executeTasks() }
    }

    fun schedule(task: CreateAndScheduleLambdaFunctionTask) {
        lambdaFunctionHandler.create(task.awsLambda)
        val schedulerTask = SchedulerTask(
            id = UUID.randomUUID(),
            name = task.awsLambda.name,
            jsonPayload = task.jsonPayload,
            status = LambdaStatus.SCHEDULED,
            scheduledAt = LocalDateTime.now().plus(task.initialDelay),
            totalRunsNumber = task.totalRunsNumber,
            repeatEvery = task.repeatEvery
        )
        schedule(schedulerTask)
    }

    fun schedule(task: ScheduleLambdaFunctionTask) {
        val schedulerTask = SchedulerTask(
            id = UUID.randomUUID(),
            name = task.name,
            jsonPayload = task.jsonPayload,
            status = LambdaStatus.SCHEDULED,
            scheduledAt = LocalDateTime.now().plus(task.initialDelay),
            totalRunsNumber = task.totalRunsNumber,
            repeatEvery = task.repeatEvery
        )
        schedule(schedulerTask)
    }

    private fun schedule(task: SchedulerTask) {
        if (task.totalRunsNumber == 0) return
        taskRepository.add(task)
    }

    private fun executeTasks() {
        while (!Thread.currentThread().isInterrupted) {
            val next = taskRepository.next()
            if (next != null) {
                taskExecutor.execute(next)
                val runsRemaining = if (next.totalRunsNumber == -1) -1 else next.totalRunsNumber - 1
                if (next.repeatEvery != null && runsRemaining != 0) {
                    val recurrentTask = SchedulerTask(
                        id = UUID.randomUUID(),
                        name = next.name,
                        jsonPayload = next.jsonPayload,
                        status = LambdaStatus.SCHEDULED,
                        scheduledAt = LocalDateTime.now().plus(next.repeatEvery),
                        totalRunsNumber = runsRemaining,
                        repeatEvery = next.repeatEvery
                    )
                    schedule(recurrentTask)
                }
            }
        }
    }


    override fun close() {
        executor.shutdownNow()
        executor.awaitTermination(10, TimeUnit.SECONDS)
        taskExecutor.close()
    }
}

data class CreateAndScheduleLambdaFunctionTask(
    val jsonPayload: String,
    val repeatEvery: Duration? = null,
    val initialDelay: Duration = Duration.ZERO,
    val totalRunsNumber: Int = -1,
    val awsLambda: AwsLambda,
)

data class AwsLambda(
    val name: String,
    val runtime: Runtime,
    val packageType: PackageType,
    val packageFileBase64: String,
    val handler: String = "index.handler",
)

data class ScheduleLambdaFunctionTask(
    val name: String,
    val jsonPayload: String,
    val repeatEvery: Duration? = null,
    val initialDelay: Duration = Duration.ZERO,
    val totalRunsNumber: Int = -1
)