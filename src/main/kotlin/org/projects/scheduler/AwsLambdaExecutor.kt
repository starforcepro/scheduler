package org.projects.scheduler

import software.amazon.awssdk.services.lambda.LambdaClient
import java.time.LocalDateTime
import java.util.concurrent.Executors

class AwsLambdaExecutor(
    val lambdaClient: LambdaClient,
    val lambdaRepository: AwsLambdaRepository,
    val lambdaFunctionHandler: LambdaFunctionHandler,
) : AutoCloseable {
    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    fun execute(awsLambda: SchedulerTask) {
        executor.submit { invokeLambda(awsLambda) }
    }

    private fun invokeLambda(task: SchedulerTask) {
        lambdaRepository.update(task.copy(status = LambdaStatus.RUNNING))
        val result = lambdaFunctionHandler.invoke(task)
        lambdaRepository.update(
            task.copy(
                status = result.status,
                result = result.result,
                executedAt = LocalDateTime.now()
            )
        )
    }

    override fun close() {
        lambdaClient.close()
        executor.shutdown()
    }
}
