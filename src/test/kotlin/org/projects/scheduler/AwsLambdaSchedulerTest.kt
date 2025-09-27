package org.projects.scheduler

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.lambda.model.PackageType
import software.amazon.awssdk.services.lambda.model.Runtime
import java.time.Duration
import java.util.*

class AwsLambdaSchedulerTest : TestBase() {
    private val repository = AwsLambdaRepository()
    val lambdaFunctionHandler = LambdaFunctionHandler(lambdaClient)

    fun configureScheduler(): AwsLambdaScheduler {
        val executor = AwsLambdaExecutor(lambdaClient, repository, lambdaFunctionHandler)
        return AwsLambdaScheduler(repository, executor, lambdaFunctionHandler)
    }

    @AfterEach
    fun cleanup() {
        repository.list().forEach { lambdaFunctionHandler.remove(it.name) }
    }

    @Test
    fun creates_and_schedules_lambda_function_task() {
        val scheduler = configureScheduler()
        val name = UUID.randomUUID().toString()
        val task = CreateAndScheduleLambdaFunctionTask(
            jsonPayload = "{\"firstKey\": \"firstValue\"}",
            initialDelay = Duration.ZERO,
            repeatEvery = null,
            repetitions = 0,
            awsLambda = AwsLambda(
                name,
                Runtime.NODEJS20_X,
                packageType = PackageType.ZIP,
                packageFileBase64 = getLambdaCode(),
            )
        )
        scheduler.schedule(task)
        var completedTask: SchedulerTask? = null
        await.timeout(Duration.ofSeconds(1)).until {
            completedTask = repository.findByName(name).firstOrNull { it.status == LambdaStatus.COMPLETED }
            completedTask != null
        }
        assertThat(completedTask?.result).isNotNull
        completedTask?.result?.contains("firstValue")
        completedTask?.result?.contains("firstKey")
    }

    @Test
    fun schedules_existing_lambda_function_task() {
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
            repetitions = 0,
        )
        scheduler.schedule(task)
        var completedTask: SchedulerTask? = null
        await.timeout(Duration.ofSeconds(1)).until {
            completedTask = repository.findByName(name).firstOrNull { it.status == LambdaStatus.COMPLETED }
            completedTask != null
        }
        assertThat(completedTask?.result).isNotNull
        completedTask?.result?.contains("firstValue")
        completedTask?.result?.contains("firstKey")
    }

    @Test
    fun single_task_is_executed_once() {

    }

    @Test
    fun recurring_task_with_finite_repetitions_executes_expected_times() {


    }

    @Test
    fun recurring_task_with_infinite_repetitions_executes_multiple_times() {


    }
}
