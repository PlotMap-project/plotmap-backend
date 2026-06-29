package com.plotmap.backend.service

import com.plotmap.backend.dto.ai.AiGraphResponse
import com.plotmap.backend.model.enum.GenerationMode
import com.plotmap.backend.model.enum.JobStatus
import com.plotmap.backend.repository.jpa.GenerationJobRepository
import com.plotmap.backend.repository.jpa.ProjectChapterRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.UUID

@Service
class GenerationJobProcessor(
    private val generationJobRepository: GenerationJobRepository,
    private val projectChapterRepository: ProjectChapterRepository,
    private val aiGraphService: AiGraphService,
    private val aiProcessingService: AiProcessingService,
    private val neo4jSyncService: Neo4jSyncService,
    private val transactionTemplate: TransactionTemplate
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async("aiGenerationExecutor")
    fun process(jobId: UUID) {
        try {
            log.info("Start processing job {}", jobId)

            val jobData = transactionTemplate.execute {
                loadJobData(jobId)
            } ?: throw IllegalStateException("Failed to load job data for $jobId")

            val result = aiGraphService.generateGraph(jobData.text)

            transactionTemplate.execute {
                saveResult(jobId, jobData.projectId, result, jobData.mode)
            }

            log.info("Job {} completed", jobId)

        } catch (e: Exception) {
            log.error("Job {} failed: {}", jobId, e.message, e)
            try {
                transactionTemplate.execute {
                    markFailed(jobId, e.message ?: "Unknown error")
                }
            } catch (inner: Exception) {
                log.error("Failed to mark job {} as failed: {}", jobId, inner.message)
            }
        }
    }

    private fun loadJobData(jobId: UUID): JobData {
        val job = generationJobRepository.findById(jobId)
            .orElseThrow { IllegalArgumentException("Job $jobId not found") }

        val text = when (job.mode) {
            GenerationMode.REGENERATE_ALL -> {
                val allChapters = projectChapterRepository
                    .findAllByProjectIdOrderByChapterOrderAsc(job.projectId)

                if (allChapters.isEmpty()) {
                    throw IllegalStateException(
                        "Project ${job.projectId} has no chapters to regenerate"
                    )
                }

                log.info(
                    "REGENERATE_ALL: combining {} chapters for project {}",
                    allChapters.size, job.projectId
                )

                allChapters.joinToString(separator = "\n\n") { chapter ->
                    val title = chapter.title ?: "Chapter ${chapter.chapterOrder}"
                    "$title.\n\n${chapter.text}"
                }
            }

            else -> {
                val chapterId = job.chapterId
                    ?: throw IllegalStateException("Job $jobId has no chapterId")

                val chapter = projectChapterRepository.findById(chapterId)
                    .orElseThrow { IllegalStateException("Chapter $chapterId not found") }

                chapter.text
            }
        }

        job.status = JobStatus.PROCESSING
        job.updatedAt = Instant.now()
        generationJobRepository.save(job)

        return JobData(
            projectId = job.projectId,
            text = text,
            mode = job.mode
        )
    }

    private fun saveResult(
        jobId: UUID,
        projectId: UUID,
        result: AiGraphResponse,
        mode: GenerationMode
    ) {
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

        job.status = JobStatus.COMPLETED
        job.updatedAt = Instant.now()
        job.completedAt = Instant.now()
        generationJobRepository.save(job)
    }

    private fun markFailed(jobId: UUID, errorMessage: String) {
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
