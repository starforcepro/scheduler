package org.projects.scheduler

import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executors

class AwsLambdaScheduler(
    val lambdaRepository: AwsLambdaRepository,
    val awsLambdaExecutor: AwsLambdaExecutor,
    val lambdaFunctionHandler: LambdaFunctionHandler
) : AutoCloseable {
    private val executor = Executors.newSingleThreadExecutor()

    init {
        executor.submit { listen() }
    }

    fun schedule(task: CreateAndScheduleLambdaFunctionTask) {
        lambdaFunctionHandler.create(task.awsLambda)
        val schedulerTask = SchedulerTask(
            id = UUID.randomUUID(),
            name = task.awsLambda.name,
            jsonPayload = task.jsonPayload,
            status = LambdaStatus.NEW,
            scheduledAt = LocalDateTime.now().plus(task.initialDelay),
            repetitions = task.repetitions,
            repeatEvery = task.repeatEvery
        )
        schedule(schedulerTask)
    }

    fun schedule(task: ScheduleLambdaFunctionTask) {
        val schedulerTask = SchedulerTask(
            id = UUID.randomUUID(),
            name = task.name,
            jsonPayload = task.jsonPayload,
            status = LambdaStatus.NEW,
            scheduledAt = LocalDateTime.now().plus(task.initialDelay),
            repetitions = task.repetitions,
            repeatEvery = task.repeatEvery
        )
        schedule(schedulerTask)
    }

    private fun schedule(task: SchedulerTask) {
        lambdaRepository.add(task)
    }

    private fun listen() {
        while (true) {
            val next = lambdaRepository.next()
            if (next != null) {
                awsLambdaExecutor.execute(next)
                val repetitionsRemaining = if (next.repetitions == -1) -1 else next.repetitions - 1
                if (next.repeatEvery != null && repetitionsRemaining != 0) {
                    val recurrentTask = SchedulerTask(
                        id = UUID.randomUUID(),
                        name = next.name,
                        jsonPayload = next.jsonPayload,
                        status = LambdaStatus.NEW,
                        scheduledAt = LocalDateTime.now().plus(next.repeatEvery),
                        repetitions = repetitionsRemaining,
                        repeatEvery = next.repeatEvery
                    )
                    schedule(recurrentTask)
                }
            } else {
                Thread.sleep(100)
            }
        }
    }


    override fun close() {
        executor.shutdown()
    }
}