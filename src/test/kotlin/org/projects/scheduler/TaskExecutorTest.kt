package org.projects.scheduler

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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

    private lateinit var executor: TaskExecutor

    @BeforeEach
    fun setUp() {
        executor = TaskExecutor(repository, handler)
    }

    @AfterEach
    fun tearDown() {
        executor.close()
    }

    @Test
    fun updatesTaskWithInvokeResultAfterExecution() {
        val task = SchedulerTask(
            id = UUID.randomUUID(),
            name = UUID.randomUUID().toString(),
            jsonPayload = "",
            status = LambdaStatus.SCHEDULED,
            scheduledAt = LocalDateTime.now(),
            totalRunsNumber = 1,
        )
        val resultText = "result"
        every { handler.invoke(task) } returns InvokeResult(resultText, LambdaStatus.COMPLETED)
        executor.execute(task)

        await.timeout(1, TimeUnit.SECONDS).untilAsserted {
            repository.findByName(task.name).singleOrNull {
                it.status == LambdaStatus.COMPLETED && it.result == resultText
            }
        }
    }

}