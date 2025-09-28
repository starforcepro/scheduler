package org.projects.scheduler

import java.time.LocalDateTime

class TaskExecutor(
    val taskRepository: TaskRepository,
    val lambdaFunctionHandler: LambdaFunctionHandler,
) {

    fun execute(task: SchedulerTask) {
        val runningTask = taskRepository.update(task.copy(status = LambdaStatus.RUNNING, startedAt = LocalDateTime.now()))
        val invokeResult = try {
            lambdaFunctionHandler.invoke(runningTask)
        } catch (e: Exception) {
            taskRepository.update(
                runningTask.copy(
                    status = LambdaStatus.FAILED,
                    result = "Failed to invoke function : ${e.message ?: e.javaClass.simpleName}",
                    executedAt = LocalDateTime.now()
                )
            )
            return
        }

        val status = if (invokeResult.error != null) LambdaStatus.FAILED else LambdaStatus.COMPLETED
        taskRepository.update(
            runningTask.copy(
                status = status,
                result = invokeResult.result,
                executedAt = LocalDateTime.now()
            )
        )
    }
}
