package org.projects.scheduler

import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

@ExtendWith(MockKExtension::class)
class AwsLambdaSchedulerTest {

    @MockK(relaxed = true)
    lateinit var executor: TaskExecutor

    @MockK(relaxed = true)
    lateinit var handler: LambdaFunctionHandler

    lateinit var scheduler: AwsLambdaScheduler

    val repository: TaskRepositoryInMemory = TaskRepositoryInMemory()

    @BeforeEach
    fun setUp() {
        scheduler = AwsLambdaScheduler(repository, executor, handler)
    }

    @AfterEach
    fun tearDown() {
        scheduler.close()
    }

    @Test
    fun executesTaskOnlyOnceWhenTotalRunsIsOne() {
        val name = UUID.randomUUID().toString()
        val task = ScheduleLambdaFunctionTask(
            name = name,
            jsonPayload = "",
            initialDelay = Duration.ZERO,
            repeatEvery = null,
            totalRunsNumber = 1,
        )

        scheduler.schedule(task)

        await.timeout(1, TimeUnit.SECONDS).untilAsserted {
            verify(exactly = 1) { executor.execute(match { it.name == name }) }
        }

    }

    @Test
    fun executesTaskExactlyTwiceWhenTotalRunsIsTwo() {
        val name = UUID.randomUUID().toString()
        val task = ScheduleLambdaFunctionTask(
            name = name,
            jsonPayload = "",
            initialDelay = Duration.ZERO,
            repeatEvery = Duration.ofMillis(1),
            totalRunsNumber = 2,
        )

        scheduler.schedule(task)
        await.timeout(1, TimeUnit.SECONDS).untilAsserted {
            verify(exactly = 2) { executor.execute(match { it.name == name }) }
        }
    }

    @Test
    fun executesTaskAtLeastTwiceWhenTotalRunsIsNegative() {
        val name = UUID.randomUUID().toString()
        val task = ScheduleLambdaFunctionTask(
            name = name,
            jsonPayload = "",
            initialDelay = Duration.ZERO,
            repeatEvery = Duration.ofMillis(1),
            totalRunsNumber = -1,
        )

        scheduler.schedule(task)
        await.timeout(1, TimeUnit.SECONDS).untilAsserted {
            verify(atLeast = 2) { executor.execute(match { it.name == name }) }
        }
    }

    @Test
    fun doesNotRepeatExecutionsWhenRepeatIntervalIsNotSet() {
        val name = UUID.randomUUID().toString()
        val task = ScheduleLambdaFunctionTask(
            name = name,
            jsonPayload = "",
            initialDelay = Duration.ZERO,
            repeatEvery = null,
            totalRunsNumber = 2,
        )

        scheduler.schedule(task)
        await.timeout(1, TimeUnit.SECONDS).untilAsserted {
            verify(exactly = 1) { executor.execute(match { it.name == name }) }
        }

    }

    @Test
    fun doesNotExecuteTasksWhenClosed() {
        val name = UUID.randomUUID().toString()
        val task = ScheduleLambdaFunctionTask(
            name = name,
            jsonPayload = "",
            initialDelay = Duration.ZERO,
            repeatEvery = null,
            totalRunsNumber = 2,
        )
        scheduler.close()
        scheduler.schedule(task)
        await.timeout(1, TimeUnit.SECONDS).untilAsserted {
            assertThat(repository.findByName(name).singleOrNull()).isNotNull
            verify(exactly = 0) { executor.execute(any()) }
        }
    }

    @Test
    fun doesNotExecuteTasksWhenRequestedRunsIsZero() {
        val name = UUID.randomUUID().toString()
        val task = ScheduleLambdaFunctionTask(
            name = name,
            jsonPayload = "",
            initialDelay = Duration.ZERO,
            repeatEvery = null,
            totalRunsNumber = 0,
        )
        scheduler.schedule(task)
        await.timeout(1, TimeUnit.SECONDS)
        verify(exactly = 0) { executor.execute(any()) }
    }

    @Test
    fun repeatsExecutionsNeverExceedingRepeatInterval() {
        val name = UUID.randomUUID().toString()
        val task = ScheduleLambdaFunctionTask(
            name = name,
            jsonPayload = "",
            initialDelay = Duration.ZERO,
            repeatEvery = Duration.ofSeconds(1),
            totalRunsNumber = 2,
        )
        scheduler.schedule(task)
        await.timeout(2, TimeUnit.SECONDS).untilAsserted {
            verify(exactly = 2) { executor.execute(match { it.name == name }) }
        }
    }

    @Test
    fun repeatsExecutionsNotMoreFrequentThanRepeatInterval() {
        val name = UUID.randomUUID().toString()
        val task = ScheduleLambdaFunctionTask(
            name = name,
            jsonPayload = "",
            initialDelay = Duration.ZERO,
            repeatEvery = Duration.ofSeconds(1),
            totalRunsNumber = 2,
        )
        scheduler.schedule(task)
        await.timeout(900, TimeUnit.MILLISECONDS)
        verify(exactly = 1) { executor.execute(match { it.name == name }) }
    }

    @Test
    fun executesTaskNoEarlierThanInitialDelay() {
        val name = UUID.randomUUID().toString()
        val task = ScheduleLambdaFunctionTask(
            name = name,
            jsonPayload = "",
            initialDelay = Duration.ofSeconds(1),
            repeatEvery = null,
            totalRunsNumber = 1,
        )
        scheduler.schedule(task)
        await.timeout(900, TimeUnit.MILLISECONDS)
        verify(exactly = 0) { executor.execute(match { it.name == name }) }
    }

    @Test
    fun stopsExecutionsAfterCancellation() {
        val name = UUID.randomUUID().toString()
        val task = ScheduleLambdaFunctionTask(
            name = name,
            jsonPayload = "",
            initialDelay = Duration.ZERO,
            repeatEvery = Duration.ofMillis(200),
            totalRunsNumber = -1,
        )

        scheduler.schedule(task)

        await.timeout(1, TimeUnit.SECONDS).untilAsserted {
            verify(exactly = 1) { executor.execute(match { it.name == name }) }
        }

        scheduler.cancel(name)

        await.timeout(1, TimeUnit.SECONDS)
        verify(exactly = 1) { executor.execute(match { it.name == name }) }
    }
}
