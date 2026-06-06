package com.plotmap.backend.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GenerationJobProcessor(
    private val jobStore: JobStore,
    private val aiGraphService: AiGraphService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    fun process(jobId: UUID) {
        val job = jobStore.get(jobId)?: throw IllegalArgumentException("Job $jobId not found")
        try {
            log.info("Start processing job {}", jobId)
            jobStore.markProcessing(jobId)
            val result = aiGraphService.generateGraph(job.sourceText)
            jobStore.markCompleted(jobId, result)
            log.info("Job {} completed successfully", jobId)

        } catch (e: Exception) {
            log.error("Job {} failed: {}", jobId, e.message, e)
            jobStore.markFailed(jobId, e.message ?: "Unknown error")
        }
    }
}
