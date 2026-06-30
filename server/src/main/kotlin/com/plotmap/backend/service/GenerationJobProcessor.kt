package com.plotmap.backend.service

import com.plotmap.backend.dto.ai.AiGraphResponse
import com.plotmap.backend.model.enum.GenerationMode
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
    private val aiProcessingService: AiProcessingService,
    private val neo4jSyncService: Neo4jSyncService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async("aiGenerationExecutor")
    fun process(jobId: UUID) {
        try {
            log.info("Processing job {}", jobId)

            val (projectId, text, mode) = loadJobData(jobId)
            val result = aiGraphService.generateGraph(text)
            saveResult(jobId, projectId, result, mode)

            log.info("Job {} completed successfully", jobId)

        } catch (e: Exception) {
            log.error("Job {} failed: {}", jobId, e.message, e)
            markFailed(jobId, "Generation failed. Please try again.")
        }
    }

    @Transactional
    fun loadJobData(jobId: UUID): JobData {
        val job = generationJobRepository.findById(jobId)
            .orElseThrow { IllegalArgumentException("Job $jobId not found") }

        val text = when (job.mode) {
            GenerationMode.REGENERATE_ALL -> {
                val chapters = projectChapterRepository
                    .findAllByProjectIdOrderByChapterOrderAsc(job.projectId)

                if (chapters.isEmpty()) {
                    throw IllegalStateException(
                        "No chapters found for project ${job.projectId}"
                    )
                }

                log.info(
                    "REGENERATE_ALL: combining {} chapters for project {}",
                    chapters.size, job.projectId
                )

                chapters.joinToString(separator = "\n\n") { chapter ->
                    val title = chapter.title ?: "Глава ${chapter.chapterOrder}"
                    "$title.\n\n${chapter.text}"
                }
            }

            else -> {
                val chapterId = job.chapterId
                    ?: throw IllegalStateException("Job $jobId has no chapterId")

                projectChapterRepository.findById(chapterId)
                    .orElseThrow { IllegalStateException("Chapter $chapterId not found") }
                    .text
            }
        }

        job.status = JobStatus.PROCESSING
        job.updatedAt = Instant.now()
        generationJobRepository.save(job)

        return JobData(projectId = job.projectId, text = text, mode = job.mode)
    }

    @Transactional
    fun saveResult(jobId: UUID, projectId: UUID, result: AiGraphResponse, mode: GenerationMode) {
        when (mode) {
            GenerationMode.INITIAL_GENERATION,
            GenerationMode.REGENERATE_ALL ->
                aiProcessingService.saveInitialGeneration(projectId, result)

            GenerationMode.APPEND_CHAPTER ->
                aiProcessingService.appendChapterGeneration(projectId, result)
        }

        neo4jSyncService.syncProject(projectId)

        val job = generationJobRepository.findById(jobId)
            .orElseThrow { IllegalArgumentException("Job $jobId not found") }

        val now = Instant.now()
        job.status = JobStatus.COMPLETED
        job.updatedAt = now
        job.completedAt = now
        generationJobRepository.save(job)
    }

    @Transactional
    fun markFailed(jobId: UUID, errorMessage: String) {
        generationJobRepository.findById(jobId).ifPresent { job ->
            job.status = JobStatus.FAILED
            job.errorMessage = errorMessage
            job.updatedAt = Instant.now()
            generationJobRepository.save(job)
        }
    }
}

data class JobData(
    val projectId: UUID,
    val text: String,
    val mode: GenerationMode
)
