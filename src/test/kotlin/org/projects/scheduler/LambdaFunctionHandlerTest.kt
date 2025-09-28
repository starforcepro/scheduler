package org.projects.scheduler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.lambda.model.PackageType
import software.amazon.awssdk.services.lambda.model.Runtime
import java.util.*

class LambdaFunctionHandlerTest : TestBase() {
    private val lambdaFunctionHandler = LambdaFunctionHandler(lambdaClient)

    @Test
    fun deleteLambdaFunction() {
        val name = UUID.randomUUID().toString()
        val lambda = AwsLambda(
            name,
            Runtime.NODEJS20_X,
            packageType = PackageType.ZIP,
            packageFileBase64 = getLambdaCode(),
        )
        lambdaFunctionHandler.create(lambda)

        lambdaFunctionHandler.remove(name)

        val result = lambdaFunctionHandler.get(name)

        assertThat(result).isNull()
    }

    @Test
    fun getLambdaFunction() {
        val name = UUID.randomUUID().toString()
        val lambda = AwsLambda(
            name,
            Runtime.NODEJS20_X,
            packageType = PackageType.ZIP,
            packageFileBase64 = getLambdaCode(),
        )
        lambdaFunctionHandler.create(lambda)

        val result = lambdaFunctionHandler.get(name)

        assertThat(result).isNotNull
        lambdaFunctionHandler.remove(name)
    }
}