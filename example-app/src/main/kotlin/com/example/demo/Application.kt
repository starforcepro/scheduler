package com.example.demo

import org.projects.scheduler.AwsLambdaScheduler
import org.projects.scheduler.LambdaFunctionHandler
import org.projects.scheduler.TaskExecutor
import org.projects.scheduler.TaskRepository
import org.projects.scheduler.TaskRepositoryInMemory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.lambda.LambdaClient

@SpringBootApplication
class SampleAppApplication {

    @Bean
    fun taskRepository(): TaskRepositoryInMemory = TaskRepositoryInMemory()

    @Bean
    fun lambdaFunctionHandler(lambdaClient: LambdaClient): LambdaFunctionHandler {
        return LambdaFunctionHandler(lambdaClient)
    }

    @Bean
    fun taskExecutor(taskRepository: TaskRepositoryInMemory, lambdaFunctionHandler: LambdaFunctionHandler): TaskExecutor {
        return TaskExecutor(taskRepository, lambdaFunctionHandler)
    }

    @Bean
    fun scheduler(
        taskRepository: TaskRepository,
        taskExecutor: TaskExecutor,
        lambdaFunctionHandler: LambdaFunctionHandler
    ): AwsLambdaScheduler {
        return AwsLambdaScheduler(
            taskRepository,
            taskExecutor,
            lambdaFunctionHandler,
        )
    }
}

@Configuration
class AwsLambdaConfig {

    @Bean
    fun awsCredentialsProvider(): AwsCredentialsProvider? {
        return DefaultCredentialsProvider.create()
    }

    @Bean
    fun lambdaClient(awsCredentialsProvider: AwsCredentialsProvider?): LambdaClient? {
        return LambdaClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .build()
    }
}

fun main(args: Array<String>) {
    runApplication<SampleAppApplication>(*args)
}