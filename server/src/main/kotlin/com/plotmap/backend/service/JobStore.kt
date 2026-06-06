package com.plotmap.backend.service

import com.plotmap.backend.dto.ai.AiGraphResponse
import com.plotmap.backend.model.entity.GenerationJob
import com.plotmap.backend.model.enum.GenerationMode
import com.plotmap.backend.model.enum.JobStatus
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class JobStore {

    private val jobs = ConcurrentHashMap<UUID, GenerationJob>()
    private val results = ConcurrentHashMap<UUID, AiGraphResponse>()

    fun create(
        projectId: UUID,
        userId: UUID,
        sourceText: String,
        mode: GenerationMode = GenerationMode.FULL
    ): GenerationJob {
        val job = GenerationJob(
            projectId = projectId,
            userId = userId,
            mode = mode,
            sourceText = sourceText
        )
        jobs[job.id] = job
        return job
    }

    fun get(jobId: UUID): GenerationJob? = jobs[jobId]

    fun getResult(jobId: UUID): AiGraphResponse? = results[jobId]

    fun markProcessing(jobId: UUID) {
        jobs.computeIfPresent(jobId) { _, oldJob ->
            oldJob.copy(
                status = JobStatus.PROCESSING,
                updatedAt = Instant.now()
            )
        }
    }

    fun markCompleted(jobId: UUID, result: AiGraphResponse) {
        results[jobId] = result

        jobs.computeIfPresent(jobId) { _, oldJob ->
            oldJob.copy(
                status = JobStatus.COMPLETED,
                updatedAt = Instant.now()
            )
        }
    }

    fun markFailed(jobId: UUID, errorMessage: String) {
        jobs.computeIfPresent(jobId) { _, oldJob ->
            oldJob.copy(
                status = JobStatus.FAILED,
                errorMessage = errorMessage,
                updatedAt = Instant.now()
            )
        }
    }
}
