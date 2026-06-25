package com.plotmap.backend.service

import com.plotmap.backend.model.enum.JobStatus
import com.plotmap.backend.repository.jpa.GenerationJobRepository
import com.plotmap.backend.repository.jpa.ProjectChapterRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class GenerationJobProcessor(
    private val generationJobRepository: GenerationJobRepository,
    private val projectChapterRepository: ProjectChapterRepository,
    private val aiGraphService: AiGraphService,
    private val aiProcessingService: AiProcessingService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @Transactional
    fun process(jobId: UUID) {
        val job = generationJobRepository.findById(jobId)
            .orElseThrow { IllegalArgumentException("Job $jobId not found") }

        try {
            log.info("Start processing job {}", jobId)

            job.status = JobStatus.PROCESSING
            job.updatedAt = Instant.now()
            generationJobRepository.save(job)

            val chapterId = job.chapterId
                ?: throw IllegalStateException("Job $jobId has no chapterId")

            val chapter = projectChapterRepository.findById(chapterId)
                .orElseThrow { IllegalStateException("Chapter $chapterId not found") }

            val result = aiGraphService.generateGraph(chapter.text)

            when (job.mode) {
                com.plotmap.backend.model.enum.GenerationMode.INITIAL_GENERATION ->
                    aiProcessingService.saveInitialGeneration(job.projectId, result)

                com.plotmap.backend.model.enum.GenerationMode.APPEND_CHAPTER ->
                    aiProcessingService.appendChapterGeneration(job.projectId, result)
            }

            job.status = JobStatus.COMPLETED
            job.updatedAt = Instant.now()
            job.completedAt = Instant.now()
            generationJobRepository.save(job)

            log.info("Job {} completed successfully", jobId)

        } catch (e: Exception) {
            log.error("Job {} failed: {}", jobId, e.message, e)

            job.status = JobStatus.FAILED
            job.errorMessage = e.message ?: "Unknown error"
            job.updatedAt = Instant.now()
            generationJobRepository.save(job)
        }
    }
}
