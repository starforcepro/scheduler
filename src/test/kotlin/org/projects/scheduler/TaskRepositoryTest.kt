package org.projects.scheduler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class TaskRepositoryTest {

    private lateinit var repository: TaskRepository

    @BeforeEach
    fun setUp() {
        repository = TaskRepository()
    }

    @AfterEach
    fun tearDown() {
        repository.close()
    }

    @Test
    fun nextReturnsReadyTasksInScheduledOrder() {
        val latest = SchedulerTask(
            id = UUID.randomUUID(),
            name = UUID.randomUUID().toString(),
            jsonPayload = "",
            status = LambdaStatus.SCHEDULED,
            scheduledAt = LocalDateTime.now(),
            totalRunsNumber = 1,
        )
        val earliest = SchedulerTask(
            id = UUID.randomUUID(),
            name = UUID.randomUUID().toString(),
            jsonPayload = "",
            status = LambdaStatus.SCHEDULED,
            scheduledAt = LocalDateTime.now().minusMinutes(1),
            totalRunsNumber = 1,
        )

        repository.add(latest)
        repository.add(earliest)

        val first = repository.next()
        val second = repository.next()
        assertThat(first?.id).isEqualTo(earliest.id)
        assertThat(second?.id).isEqualTo(latest.id)
    }

    @Test
    fun nextReturnsNullWhenNoDueTasks() {
        val futureTask = SchedulerTask(
            id = UUID.randomUUID(),
            name = UUID.randomUUID().toString(),
            jsonPayload = "",
            status = LambdaStatus.SCHEDULED,
            scheduledAt = LocalDateTime.now().plusMinutes(1),
            totalRunsNumber = 1,
        )

        repository.add(futureTask)

        val task = repository.next()
        assertThat(task).isNull()
        assertThat(repository.find(futureTask.id)).isNotNull
    }

    @Test
    fun updatePersistsChanges() {
        val id = UUID.randomUUID()
        val originalTask = SchedulerTask(
            id = id,
            name = UUID.randomUUID().toString(),
            jsonPayload = "",
            status = LambdaStatus.SCHEDULED,
            scheduledAt = LocalDateTime.now().minusSeconds(1),
            totalRunsNumber = 1,
        )
        repository.add(originalTask)

        repository.update(
            originalTask.copy(
                status = LambdaStatus.COMPLETED,
                executedAt = LocalDateTime.now(),
                result = "ok"
            )
        )

        val updatedTask = repository.find(id)
        assertThat(updatedTask?.status).isEqualTo(LambdaStatus.COMPLETED)
        assertThat(updatedTask?.result).isEqualTo("ok")
        assertThat(updatedTask?.executedAt).isNotNull
    }

    @Test
    fun addStoresTask() {
        val task = SchedulerTask(
            id = UUID.randomUUID(),
            name = UUID.randomUUID().toString(),
            jsonPayload = "",
            status = LambdaStatus.SCHEDULED,
            scheduledAt = LocalDateTime.now(),
            totalRunsNumber = 1,
        )

        repository.add(task)

        val storedTask = repository.find(task.id)
        assertThat(storedTask).isEqualTo(task)
    }

    @Test
    fun listReturnsAllTasks() {
        val first = SchedulerTask(
            id = UUID.randomUUID(),
            name = UUID.randomUUID().toString(),
            jsonPayload = "",
            status = LambdaStatus.SCHEDULED,
            scheduledAt = LocalDateTime.now(),
            totalRunsNumber = 1,
        )
        val second = SchedulerTask(
            id = UUID.randomUUID(),
            name = UUID.randomUUID().toString(),
            jsonPayload = "",
            status = LambdaStatus.SCHEDULED,
            scheduledAt = LocalDateTime.now(),
            totalRunsNumber = 1,
        )

        repository.add(first)
        repository.add(second)

        val all = repository.list()
        assertThat(all).containsExactlyInAnyOrder(first, second)
    }

    @Test
    fun findByNameReturnsOnlyMatching() {
        val name = UUID.randomUUID().toString()
        val namedOneWay = SchedulerTask(
            id = UUID.randomUUID(),
            name = name,
            jsonPayload = "",
            status = LambdaStatus.SCHEDULED,
            scheduledAt = LocalDateTime.now(),
            totalRunsNumber = 1,
        )
        val namedOtherWay = SchedulerTask(
            id = UUID.randomUUID(),
            name = UUID.randomUUID().toString(),
            jsonPayload = "",
            status = LambdaStatus.SCHEDULED,
            scheduledAt = LocalDateTime.now(),
            totalRunsNumber = 1,
        )

        repository.add(namedOneWay)
        repository.add(namedOtherWay)

        val result = repository.findByName(name)
        assertThat(result.size).isEqualTo(1)
        assertThat(repository.list()).containsExactlyInAnyOrder(namedOneWay, namedOtherWay)
    }
}
