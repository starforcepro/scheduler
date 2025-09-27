package org.projects.scheduler

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.lambda.LambdaClient
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.encoding.Base64

open class TestBase {
    val lambdaClient: LambdaClient = LambdaClient.builder()
        .credentialsProvider(DefaultCredentialsProvider.create())
        .region(Region.EU_NORTH_1)
        .build()

    fun getLambdaCode(): String {
        val lambdaCode = """
        exports.handler = async (event, context) => {
          console.log('Received event:', JSON.stringify(event, null, 2));

          const response = {
            statusCode: 200,
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({ message: 'Hello from Lambda!', input: event }),
          };

          return response;
        };
    """.trimIndent()

        ByteArrayOutputStream().use { byteStream ->
            ZipOutputStream(byteStream).use { zipStream ->
                val entry = ZipEntry("index.js")
                zipStream.putNextEntry(entry)
                zipStream.write(lambdaCode.toByteArray())
                zipStream.closeEntry()
            }
            return Base64.encode(byteStream.toByteArray())
        }
    }
}