package org.projects.scheduler

import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.DelayQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private const val POLLING_DELAY = 100L

class TaskRepositoryInMemory : AutoCloseable, TaskRepository {
    private val executionQueue = DelayQueue<SchedulerTask>()
    private val idToTaskMap = ConcurrentHashMap<UUID, SchedulerTask>()
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    init {
        scheduler.scheduleAtFixedRate({ evictExpired() }, 1, 1, TimeUnit.MINUTES)
    }

    private fun evictExpired() {
        idToTaskMap.values.removeIf {
            val isExpired = it.executedAt?.plusHours(1)?.isBefore(LocalDateTime.now()) ?: false
            it.isTerminal() && isExpired
        }
    }

    override fun add(task: SchedulerTask) {
        executionQueue.offer(task)
        idToTaskMap[task.id] = task
    }

    override fun next(): SchedulerTask? {
        return executionQueue.poll(POLLING_DELAY, TimeUnit.MILLISECONDS)
    }

    override fun update(task: SchedulerTask) {
        idToTaskMap[task.id] = task
    }

    override fun find(id: UUID): SchedulerTask? {
        return idToTaskMap[id]
    }

    override fun list(): List<SchedulerTask> {
        return idToTaskMap.values.toList()
    }

    override fun findByName(name: String): List<SchedulerTask> {
        return idToTaskMap.values.filter { it.name == name }
    }

    override fun close() {
        scheduler.shutdownNow()
        scheduler.awaitTermination(1, TimeUnit.SECONDS)
    }
}

interface TaskRepository {
    fun add(task: SchedulerTask)
    fun next(): SchedulerTask?
    fun update(task: SchedulerTask)
    fun find(id: UUID): SchedulerTask?
    fun list(): List<SchedulerTask>
    fun findByName(name: String): List<SchedulerTask>
}
