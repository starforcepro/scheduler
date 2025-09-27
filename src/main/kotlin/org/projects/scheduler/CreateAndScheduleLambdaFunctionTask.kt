package org.projects.scheduler

import software.amazon.awssdk.services.lambda.model.PackageType
import software.amazon.awssdk.services.lambda.model.Runtime
import java.time.Duration

data class CreateAndScheduleLambdaFunctionTask(
    val jsonPayload: String,
    val repeatEvery: Duration? = null,
    val initialDelay: Duration = Duration.ZERO,
    val repetitions: Int = -1,
    val awsLambda: AwsLambda,
)

data class AwsLambda(
    val name: String,
    val runtime: Runtime,
    val packageType: PackageType,
    val packageFileBase64: String,
    val handler: String = "index.handler",
)