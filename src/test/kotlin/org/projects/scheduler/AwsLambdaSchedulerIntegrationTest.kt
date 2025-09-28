package org.projects.scheduler

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.lambda.model.PackageType
import software.amazon.awssdk.services.lambda.model.Runtime
import java.time.Duration
import java.util.*

class AwsLambdaSchedulerIntegrationTest : TestBase() {
    private val repository = TaskRepositoryInMemory()
    private val lambdaFunctionHandler = LambdaFunctionHandler(lambdaClient)

    fun configureScheduler(): AwsLambdaScheduler {
        val executor = TaskExecutor(repository, lambdaFunctionHandler)
        return AwsLambdaScheduler(repository, executor, lambdaFunctionHandler)
    }

    @AfterEach
    fun cleanup() {
        repository.list().forEach { lambdaFunctionHandler.remove(it.name) }
    }

    @Test
    fun createsAndSchedulesLambdaFunctionTask() {
        val scheduler = configureScheduler()
        val name = UUID.randomUUID().toString()
        val task = CreateAndScheduleLambdaFunctionTask(
            jsonPayload = "{\"firstKey\": \"firstValue\"}",
            initialDelay = Duration.ZERO,
            repeatEvery = null,
            totalRunsNumber = 1,
            awsLambda = AwsLambda(
                name,
                Runtime.NODEJS20_X,
                packageType = PackageType.ZIP,
                packageFileBase64 = getLambdaCode(),
            )
        )
        scheduler.schedule(task)
        var completedTask: SchedulerTask? = null
        await.timeout(Duration.ofSeconds(5)).until {
            completedTask = repository.findByName(name).firstOrNull { it.status == LambdaStatus.COMPLETED }
            completedTask != null
        }
        assertThat(completedTask?.result).isNotNull
        completedTask?.result?.contains("firstValue")
        completedTask?.result?.contains("firstKey")
    }

    @Test
    fun schedulesExistingLambdaFunctionTask() {
        val scheduler = configureScheduler()
        val name = UUID.randomUUID().toString()
        val awsLambda = AwsLambda(
            name,
            Runtime.NODEJS20_X,
            packageType = PackageType.ZIP,
            packageFileBase64 = getLambdaCode(),
        )
        lambdaFunctionHandler.create(awsLambda)
        val task = ScheduleLambdaFunctionTask(
            name = name,
            jsonPayload = "{\"firstKey\": \"firstValue\"}",
            initialDelay = Duration.ZERO,
            totalRunsNumber = 1,
        )
        scheduler.schedule(task)
        var completedTask: SchedulerTask? = null
        await.timeout(Duration.ofSeconds(5)).until {
            completedTask = repository.findByName(name).firstOrNull { it.status == LambdaStatus.COMPLETED }
            completedTask != null
        }
        assertThat(completedTask?.result).isNotNull
        completedTask?.result?.contains("firstValue")
        completedTask?.result?.contains("firstKey")
    }
}
