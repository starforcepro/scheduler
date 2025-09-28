package org.projects.scheduler

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

@ExtendWith(MockKExtension::class)
class TaskExecutorTest {

    @MockK(relaxed = true)
    private lateinit var handler: LambdaFunctionHandler

    private val repository = TaskRepositoryInMemory()

    @Test
    fun updatesTaskWithInvokeResultAfterExecution() {
        val task = SchedulerTask(
            id = UUID.randomUUID(),
            name = UUID.randomUUID().toString(),
            jsonPayload = "",
            status = LambdaStatus.SCHEDULED,
            scheduledAt = LocalDateTime.now(),
        )
        val resultText = "result"
        every { handler.invoke(task) } returns InvokeResult(resultText)
        val executor = TaskExecutor(repository, handler)

        executor.execute(task)

        await.timeout(1, TimeUnit.SECONDS).untilAsserted {
            repository.findByName(task.name).singleOrNull {
                it.status == LambdaStatus.COMPLETED && it.result == resultText
            }
        }
    }

    @Test
    fun updatesTaskWithFailureWhenInvokeThrowsException() {
        val task = SchedulerTask(
            id = UUID.randomUUID(),
            name = UUID.randomUUID().toString(),
            jsonPayload = "",
            status = LambdaStatus.SCHEDULED,
            scheduledAt = LocalDateTime.now(),
        )
        val errorMessage = "error message"
        every { handler.invoke(any()) } throws Exception(errorMessage)
        val executor = TaskExecutor(repository, handler)

        executor.execute(task)

        await.timeout(1, TimeUnit.SECONDS).untilAsserted {
            repository.findByName(task.name).singleOrNull {
                it.status == LambdaStatus.FAILED && it.result == "Failed to invoke function : $errorMessage"
            }
        }
    }

}
