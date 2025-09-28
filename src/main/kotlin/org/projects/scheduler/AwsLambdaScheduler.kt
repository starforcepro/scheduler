package org.projects.scheduler

import software.amazon.awssdk.services.lambda.model.PackageType
import software.amazon.awssdk.services.lambda.model.Runtime
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AwsLambdaScheduler(
    val taskRepository: TaskRepository = TaskRepositoryInMemory(),
    val taskExecutor: TaskExecutor,
    val lambdaFunctionHandler: LambdaFunctionHandler
) : AutoCloseable {
    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    init {
        executor.submit { executeTasks() }
    }

    fun schedule(task: CreateAndScheduleLambdaFunctionTask) {
        lambdaFunctionHandler.create(task.awsLambda)
        val schedulerTask = SchedulerTask(
            id = UUID.randomUUID(),
            name = task.awsLambda.name,
            status = LambdaStatus.SCHEDULED,
            scheduledAt = LocalDateTime.now().plus(task.initialDelay),
            jsonPayload = task.jsonPayload,
        )
        val schedulerTaskDescription = SchedulerTaskDescription(
            name = task.awsLambda.name,
            repeatEvery = task.repeatEvery,
            initialDelay = task.initialDelay,
            runsLeft = task.totalRunsNumber,
        )
        schedule(schedulerTask, schedulerTaskDescription)
    }

    fun schedule(task: ScheduleLambdaFunctionTask) {
        val schedulerTask = SchedulerTask(
            id = UUID.randomUUID(),
            name = task.name,
            status = LambdaStatus.SCHEDULED,
            scheduledAt = LocalDateTime.now().plus(task.initialDelay),
            jsonPayload = task.jsonPayload,
        )
        val schedulerTaskDescription = SchedulerTaskDescription(
            name = task.name,
            repeatEvery = task.repeatEvery,
            initialDelay = task.initialDelay,
            runsLeft = task.totalRunsNumber,
        )
        schedule(schedulerTask, schedulerTaskDescription)
    }

    private fun schedule(task: SchedulerTask, schedulerTaskDescription: SchedulerTaskDescription) {
        if (schedulerTaskDescription.runsLeft == 0) return
        taskRepository.upsert(task, schedulerTaskDescription)
    }

    private fun executeTasks() {
        while (!Thread.currentThread().isInterrupted) {
            val next = taskRepository.next()
            if (next != null) {
                val description = taskRepository.findDescription(next.name)
                if (description.cancelled) continue
                executor.submit { taskExecutor.execute(next) }
                val runsRemaining = if (description.runsLeft == -1) -1 else description.runsLeft - 1
                if (description.repeatEvery != null && runsRemaining != 0) {
                    val recurrentTask = SchedulerTask(
                        id = UUID.randomUUID(),
                        name = next.name,
                        status = LambdaStatus.SCHEDULED,
                        scheduledAt = LocalDateTime.now().plus(description.repeatEvery),
                        jsonPayload = next.jsonPayload,
                    )
                    schedule(recurrentTask, description.copy(runsLeft = runsRemaining))
                }
            }
        }
    }

    fun list(): List<SchedulerTask> {
        return taskRepository.list()
    }

    fun findByName(name: String): List<SchedulerTask> {
        return taskRepository.findByName(name)
    }

    fun cancel(name: String) {
        taskRepository.cancel(name)
    }

    override fun close() {
        executor.shutdownNow()
        executor.awaitTermination(10, TimeUnit.SECONDS)
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