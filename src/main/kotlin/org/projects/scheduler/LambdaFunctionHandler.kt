package org.projects.scheduler

import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.*
import java.time.Instant
import kotlin.io.encoding.Base64

class LambdaFunctionHandler(
    val lambdaClient: LambdaClient,
) {
    fun invoke(task: SchedulerTask): InvokeResult {
        val invokeRequest = InvokeRequest.builder()
            .functionName(task.name)
            .payload(SdkBytes.fromUtf8String(task.jsonPayload))
            .build()
        val response = try {
            lambdaClient.invoke(invokeRequest)
        } catch (e: Exception) {
            return InvokeResult(
                result = "Failed to invoke function",
                status = LambdaStatus.FAILED,
                error = e.message ?: e.javaClass.simpleName
            )
        }
        return InvokeResult(
            result = response.payload().asUtf8String(),
            status = if (response.functionError() == null) LambdaStatus.COMPLETED else LambdaStatus.FAILED
        )
    }

    fun create(awsLambda: AwsLambda) {
        val role = System.getenv("AWS_LAMBDA_ROLE_ARN")
            ?: throw IllegalArgumentException("AWS_LAMBDA_ROLE_ARN not set")
        val createRequest = CreateFunctionRequest.builder()
            .functionName(awsLambda.name)
            .runtime(awsLambda.runtime)
            .handler(awsLambda.handler)
            .role(role)
            .packageType(awsLambda.packageType)
            .architectures(Architecture.ARM64)
            .code { it.zipFile(SdkBytes.fromByteArray(Base64.decode(awsLambda.packageFileBase64))) }
            .build()

        val response = lambdaClient.createFunction(createRequest)
        if (!response.sdkHttpResponse().isSuccessful) throw RuntimeException("Failed to create function")
        waitForLambdaActivation(awsLambda.name)
    }

    fun remove(name: String) {
        val request = DeleteFunctionRequest.builder().functionName(name).build()
        lambdaClient.deleteFunction(request)
    }

    fun get(name: String): FunctionConfiguration? {
        val request = GetFunctionRequest.builder().functionName(name).build()
        val response = try {
            lambdaClient.getFunction(request)
        } catch (_: ResourceNotFoundException) {
            return null
        }
        if (response.sdkHttpResponse().isSuccessful) return response.configuration()
        return null
    }

    private fun waitForLambdaActivation(name: String) {
        val waitTimeMilliseconds = 60000L
        val waitUntilTime = Instant.now().plusMillis(waitTimeMilliseconds)
        while (Instant.now().isBefore(waitUntilTime)) {
            val function = get(name)
            if (function != null && function.state() == State.ACTIVE) return
            Thread.sleep(100)
        }
        throw IllegalStateException("Function $name never became active")
    }
}

data class InvokeResult(
    val result: String,
    val status: LambdaStatus,
    val error: String? = null
)