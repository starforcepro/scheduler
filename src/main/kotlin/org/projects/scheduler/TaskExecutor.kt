package org.projects.scheduler

import java.time.LocalDateTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TaskExecutor(
    val taskRepository: TaskRepository,
    val lambdaFunctionHandler: LambdaFunctionHandler,
    val executor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()
) : AutoCloseable {

    fun execute(task: SchedulerTask) {
        executor.submit { invokeLambda(task) }
    }

    private fun invokeLambda(task: SchedulerTask) {
        taskRepository.update(task.copy(status = LambdaStatus.RUNNING))
        val result = lambdaFunctionHandler.invoke(task)
        taskRepository.update(
            task.copy(
                status = result.status,
                result = result.result,
                executedAt = LocalDateTime.now()
            )
        )
    }

    override fun close() {
        executor.shutdownNow()
        executor.awaitTermination(10, TimeUnit.SECONDS)
        taskRepository.close()
    }
}
