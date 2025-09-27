package org.projects.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.lambda.LambdaClient
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.Executors

class AwsLambdaSchedulerCoroutine(
    val lambdaRepository: AwsLambdaRepository,
    val awsLambdaExecutor: AwsLambdaExecutor,
) : AutoCloseable {

    val lambdaClient: LambdaClient = LambdaClient.builder()
        .credentialsProvider(DefaultCredentialsProvider.create())
        .region(Region.EU_NORTH_1)
        .build()
    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    val channel = Channel<SchedulerTask>()

    init {
        CoroutineScope(executor.asCoroutineDispatcher()).launch { listen() }
    }

    suspend fun schedule(awsLambda: SchedulerTask) {
        lambdaRepository.add(awsLambda)
        channel.send(awsLambda)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun next(): SchedulerTask {
        var awsLambda = lambdaRepository.next()
        while (true) {
            val nextScheduledTime = awsLambda?.scheduledAt ?: LocalDateTime.now().plusSeconds(1)
            val untilNextExecution = Duration.between(LocalDateTime.now(), nextScheduledTime)
            if (awsLambda != null && (untilNextExecution.isNegative || untilNextExecution.isZero)) return awsLambda
            val result = select {
                channel.onReceive {
                    if (awsLambda == null || it.scheduledAt.isBefore(awsLambda?.scheduledAt)) {
                        return@onReceive it
                    }
                    return@onReceive awsLambda
                }
                onTimeout(untilNextExecution.toMillis()) {
                    return@onTimeout awsLambda
                }
            }
            if (result != null && result.scheduledAt.isBefore(LocalDateTime.now())) {
                return result
            }
            awsLambda = result
        }
    }

    private suspend fun listen() {
        while (true) {
            awsLambdaExecutor.execute(next())
        }
    }

    override fun close() {
        executor.shutdown()
    }
}