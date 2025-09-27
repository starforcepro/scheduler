package org.projects.scheduler

import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue

class AwsLambdaRepository {
    private val executionQueue = PriorityBlockingQueue<SchedulerTask>(1000, compareBy { it.scheduledAt })
    private val lambdas = ConcurrentHashMap<UUID, SchedulerTask>()

    fun add(task: SchedulerTask) {
        executionQueue.offer(task)
        lambdas[task.id] = task
    }

    fun next(): SchedulerTask? {
        val next = executionQueue.poll() ?: return null
        if (next.scheduledAt.isBefore(LocalDateTime.now())) {
            return next
        } else {
            executionQueue.offer(next)
        }
        return null
    }

    fun update(task: SchedulerTask) {
        lambdas[task.id] = task
    }

    fun find(id: UUID): SchedulerTask? {
        return lambdas[id]
    }

    fun list(): List<SchedulerTask> {
        return lambdas.values.toList()
    }

    fun findByName(name: String): List<SchedulerTask> {
        return lambdas.values.filter { it.name == name }
    }
}