package com.example.demo

import org.projects.scheduler.AwsLambda
import org.projects.scheduler.AwsLambdaScheduler
import org.projects.scheduler.CreateAndScheduleLambdaFunctionTask
import org.projects.scheduler.SchedulerTask
import org.projects.scheduler.TaskRepositoryInMemory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import software.amazon.awssdk.services.lambda.model.PackageType
import software.amazon.awssdk.services.lambda.model.Runtime
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.encoding.Base64


@RestController
@RequestMapping
class PayrollController(
    private val awsLambdaScheduler: AwsLambdaScheduler,
    private val taskRepository: TaskRepositoryInMemory,
) {

    @PostMapping("/schedule")
    fun schedule(@RequestBody scheduleRequest: ScheduleRequest) {
        val task = CreateAndScheduleLambdaFunctionTask(
            jsonPayload = scheduleRequest.jsonPayload,
            repeatEvery = Duration.ZERO,
            initialDelay = Duration.ZERO,
            totalRunsNumber = 1,
            awsLambda = AwsLambda(
                name = scheduleRequest.name,
                runtime = Runtime.NODEJS20_X,
                packageType = PackageType.ZIP,
                packageFileBase64 = getLambdaCode(),
            )
        )
        awsLambdaScheduler.schedule(task)
    }

    @GetMapping("/list")
    fun list(): ResponseEntity<List<SchedulerTask>> {
        return ResponseEntity.ok(taskRepository.list())
    }

    @GetMapping("result/{name}")
    fun result(@PathVariable name: String): ResponseEntity<List<SchedulerTask>> {
        val result = taskRepository.findByName(name)
        return ResponseEntity.ok(result)
    }

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

data class ScheduleRequest(
    val name: String,
    val jsonPayload: String,
)