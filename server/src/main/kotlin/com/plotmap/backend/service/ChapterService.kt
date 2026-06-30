package com.plotmap.backend.service

import com.plotmap.backend.dto.request.AddChapterRequest
import com.plotmap.backend.dto.request.UpdateChapterRequest
import com.plotmap.backend.dto.response.AddChapterResponse
import com.plotmap.backend.dto.response.ChapterDetailDto
import com.plotmap.backend.dto.response.ChapterDto
import com.plotmap.backend.dto.response.JobStatusResponse
import com.plotmap.backend.exception.ProjectNotFoundException
import com.plotmap.backend.model.entity.GenerationJob
import com.plotmap.backend.model.entity.ProjectChapter
import com.plotmap.backend.model.enum.GenerationMode
import com.plotmap.backend.model.enum.JobStatus
import com.plotmap.backend.model.enum.ProjectType
import com.plotmap.backend.repository.jpa.CharacterRepository
import com.plotmap.backend.repository.jpa.EventEdgeRepository
import com.plotmap.backend.repository.jpa.EventRepository
import com.plotmap.backend.repository.jpa.EventToCharacterRepository
import com.plotmap.backend.repository.jpa.EventToTagRepository
import com.plotmap.backend.repository.jpa.GenerationJobRepository
import com.plotmap.backend.repository.jpa.ProjectChapterRepository
import com.plotmap.backend.repository.jpa.ProjectRepository
import com.plotmap.backend.repository.jpa.StoryArcRepository
import com.plotmap.backend.repository.jpa.StoryArcToEventRepository
import com.plotmap.backend.repository.jpa.UserToProjectRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class ChapterService(
    private val projectRepository: ProjectRepository,
    private val userToProjectRepository: UserToProjectRepository,
    private val projectChapterRepository: ProjectChapterRepository,
    private val generationJobRepository: GenerationJobRepository,
    private val generationJobProcessor: GenerationJobProcessor,
    private val eventRepository: EventRepository,
    private val eventEdgeRepository: EventEdgeRepository,
    private val characterRepository: CharacterRepository,
    private val eventToCharacterRepository: EventToCharacterRepository,
    private val storyArcRepository: StoryArcRepository,
    private val storyArcToEventRepository: StoryArcToEventRepository,
    private val eventToTagRepository: EventToTagRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun getChapters(userId: UUID, projectId: UUID): List<ChapterDto> {
        ensureAccess(userId, projectId)
        return projectChapterRepository
            .findAllByProjectIdOrderByChapterOrderAsc(projectId)
            .map { it.toDto() }
    }

    fun getChapterById(userId: UUID, projectId: UUID, chapterId: UUID): ChapterDetailDto {
        ensureAccess(userId, projectId)

        val chapter = projectChapterRepository.findById(chapterId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found") }

        if (chapter.projectId != projectId) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found")
        }

        return chapter.toDetailDto()
    }

    @Transactional
    fun addChapter(userId: UUID, projectId: UUID, request: AddChapterRequest): AddChapterResponse {
        ensureAccess(userId, projectId)
        ensureAiProject(projectId)

        val nextOrder = projectChapterRepository.countByProjectId(projectId) + 1

        val chapter = projectChapterRepository.save(
            ProjectChapter(
                projectId = projectId,
                chapterOrder = nextOrder,
                title = request.title?.trim(),
                text = request.text
            )
        )

        val job = scheduleJob(projectId, chapter.id, GenerationMode.APPEND_CHAPTER)

        return AddChapterResponse(chapter = chapter.toDto(), job = job.toResponse())
    }

    @Transactional
    fun updateChapter(
        userId: UUID,
        projectId: UUID,
        chapterId: UUID,
        request: UpdateChapterRequest
    ): AddChapterResponse {
        ensureAccess(userId, projectId)
        ensureAiProject(projectId)

        val chapter = projectChapterRepository.findById(chapterId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found") }

        if (chapter.projectId != projectId) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found")
        }

        request.text?.let {
            require(it.isNotBlank()) { "Chapter text must not be empty" }
            chapter.text = it
        }
        request.title?.let { chapter.title = it.trim() }
        chapter.updatedAt = Instant.now()
        projectChapterRepository.save(chapter)

        log.info("Updated chapter {}, scheduling REGENERATE_ALL for project {}", chapterId, projectId)
        clearProjectGraph(projectId)

        val job = scheduleJob(projectId, chapter.id, GenerationMode.REGENERATE_ALL)

        return AddChapterResponse(chapter = chapter.toDto(), job = job.toResponse())
    }

    private fun scheduleJob(
        projectId: UUID,
        chapterId: UUID,
        mode: GenerationMode
    ): GenerationJob {
        val job = generationJobRepository.save(
            GenerationJob(
                projectId = projectId,
                chapterId = chapterId,
                mode = mode,
                status = JobStatus.PENDING
            )
        )

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    generationJobProcessor.process(job.id)
                }
            }
        )

        return job
    }

    private fun clearProjectGraph(projectId: UUID) {
        log.info("Clearing graph for project {}", projectId)
        eventToTagRepository.deleteAllByIdProject(projectId)
        eventToCharacterRepository.deleteAllByIdProject(projectId)
        storyArcToEventRepository.deleteAllByIdProject(projectId)
        eventEdgeRepository.deleteAllByIdProject(projectId)
        eventRepository.deleteAllByProjectId(projectId)
        storyArcRepository.deleteAllByProjectId(projectId)
        characterRepository.deleteAllByProjectId(projectId)
        log.info("Graph cleared for project {}", projectId)
    }

    private fun ensureAccess(userId: UUID, projectId: UUID) {
        if (!userToProjectRepository.existsByIdUserAndIdProject(userId, projectId)) {
            throw ProjectNotFoundException("Project not found")
        }
    }

    private fun ensureAiProject(projectId: UUID) {
        val project = projectRepository.findById(projectId)
            .orElseThrow { ProjectNotFoundException("Project not found") }
        if (project.type != ProjectType.AI_GENERATED) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "This operation is only available for AI-generated projects"
            )
        }
    }

    private fun GenerationJob.toResponse() = JobStatusResponse(
        jobId = id.toString(),
        projectId = projectId.toString(),
        status = status.name,
        errorMessage = errorMessage,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun ProjectChapter.toDto() = ChapterDto(
        id = id.toString(),
        chapterOrder = chapterOrder,
        title = title,
        createdAt = createdAt
    )

    private fun ProjectChapter.toDetailDto() = ChapterDetailDto(
        id = id.toString(),
        chapterOrder = chapterOrder,
        title = title,
        text = text,
        createdAt = createdAt
    )
}
