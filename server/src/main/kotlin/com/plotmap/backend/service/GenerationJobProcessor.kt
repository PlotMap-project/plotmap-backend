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
    private val aiProcessingService: AiProcessingService,
    private val neo4jSyncService: Neo4jSyncService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // Точка входа — async, без транзакции
    @Async("aiGenerationExecutor")
    fun process(jobId: UUID) {
        try {
            log.info("Start processing job {}", jobId)

            // Шаг 1. Загрузить job и текст — в своей транзакции
            val (projectId, text, mode) = loadJobData(jobId)

            // Шаг 2. AI-генерация — без транзакции, может быть долгой
            val result = aiGraphService.generateGraph(text)

            // Шаг 3. Сохранить результат — в своей транзакции
            saveResult(jobId, projectId, result, mode)

            log.info("Job {} completed successfully", jobId)

        } catch (e: Exception) {
            log.error("Job {} failed: {}", jobId, e.message, e)
            markFailed(jobId, e.message ?: "Unknown error")
        }
    }

    // Транзакция 1: загрузить данные и сменить статус на PROCESSING
    @Transactional
    fun loadJobData(jobId: UUID): JobData {
        val job = generationJobRepository.findById(jobId)
            .orElseThrow { IllegalArgumentException("Job $jobId not found") }

        val chapterId = job.chapterId
            ?: throw IllegalStateException("Job $jobId has no chapterId")

        val chapter = projectChapterRepository.findById(chapterId)
            .orElseThrow { IllegalStateException("Chapter $chapterId not found") }

        job.status = JobStatus.PROCESSING
        job.updatedAt = Instant.now()
        generationJobRepository.save(job)

        // После return этого метода транзакция коммитится
        // и PROCESSING сразу виден снаружи
        return JobData(
            projectId = job.projectId,
            text = chapter.text,
            mode = job.mode
        )
    }

    // Транзакция 2: сохранить результат и сменить статус на COMPLETED
    @Transactional
    fun saveResult(
        jobId: UUID,
        projectId: UUID,
        result: com.plotmap.backend.dto.ai.AiGraphResponse,
        mode: com.plotmap.backend.model.enum.GenerationMode
    ) {
        when (mode) {
            com.plotmap.backend.model.enum.GenerationMode.INITIAL_GENERATION ->
                aiProcessingService.saveInitialGeneration(projectId, result)
            com.plotmap.backend.model.enum.GenerationMode.APPEND_CHAPTER ->
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

    // Транзакция 3: сохранить ошибку
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

// Вспомогательный data class — добавь в конец файла или рядом
data class JobData(
    val projectId: UUID,
    val text: String,
    val mode: com.plotmap.backend.model.enum.GenerationMode
)
