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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
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
        ensureUserHasAccessToProject(userId, projectId)

        return projectChapterRepository
            .findAllByProjectIdOrderByChapterOrderAsc(projectId)
            .map { it.toDto() }
    }

    fun getChapterById(userId: UUID, projectId: UUID, chapterId: UUID): ChapterDetailDto {
        ensureUserHasAccessToProject(userId, projectId)

        val chapter = projectChapterRepository.findById(chapterId)
            .orElseThrow { ProjectNotFoundException("Chapter $chapterId not found") }

        if (chapter.projectId != projectId) {
            throw ProjectNotFoundException("Chapter $chapterId not found in project $projectId")
        }

        return chapter.toDetailDto()
    }

    @Transactional
    fun addChapter(userId: UUID, projectId: UUID, request: AddChapterRequest): AddChapterResponse {
        ensureUserHasAccessToProject(userId, projectId)

        require(request.text.isNotBlank()) { "Chapter text must not be empty" }

        val project = projectRepository.findById(projectId)
            .orElseThrow { ProjectNotFoundException("Project $projectId not found") }

        require(project.type == ProjectType.AI_GENERATED) {
            "Cannot add chapters to a MANUAL project"
        }

        val nextOrder = projectChapterRepository.countByProjectId(projectId) + 1

        val chapter = projectChapterRepository.save(
            ProjectChapter(
                projectId = projectId,
                chapterOrder = nextOrder,
                title = request.title?.trim(),
                text = request.text
            )
        )

        val job = generationJobRepository.save(
            GenerationJob(
                projectId = projectId,
                chapterId = chapter.id,
                mode = GenerationMode.APPEND_CHAPTER,
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

        return AddChapterResponse(
            chapter = chapter.toDto(),
            job = jobToResponse(job)
        )
    }

    @Transactional
    fun updateChapter(
        userId: UUID,
        projectId: UUID,
        chapterId: UUID,
        request: UpdateChapterRequest
    ): AddChapterResponse {
        ensureUserHasAccessToProject(userId, projectId)

        require(request.text.isNotBlank()) { "Chapter text must not be empty" }

        val project = projectRepository.findById(projectId)
            .orElseThrow { ProjectNotFoundException("Project $projectId not found") }

        require(project.type == ProjectType.AI_GENERATED) {
            "Cannot update chapters in a MANUAL project"
        }

        val chapter = projectChapterRepository.findById(chapterId)
            .orElseThrow { ProjectNotFoundException("Chapter $chapterId not found") }

        if (chapter.projectId != projectId) {
            throw ProjectNotFoundException("Chapter $chapterId not found in project $projectId")
        }

        chapter.text = request.text
        request.title?.let { chapter.title = it.trim() }
        projectChapterRepository.save(chapter)

        log.info("Updated chapter {} in project {}", chapterId, projectId)
        clearProjectGraph(projectId)

        val job = generationJobRepository.save(
            GenerationJob(
                projectId = projectId,
                chapterId = chapter.id,
                mode = GenerationMode.REGENERATE_ALL,
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

        return AddChapterResponse(
            chapter = chapter.toDto(),
            job = jobToResponse(job)
        )
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

    private fun jobToResponse(job: GenerationJob): JobStatusResponse {
        return JobStatusResponse(
            jobId = job.id.toString(),
            projectId = job.projectId.toString(),
            status = job.status.name,
            errorMessage = job.errorMessage,
            result = null,
            createdAt = job.createdAt,
            updatedAt = job.updatedAt
        )
    }

    private fun ensureUserHasAccessToProject(userId: UUID, projectId: UUID) {
        val exists = userToProjectRepository.existsByIdUserAndIdProject(userId, projectId)
        if (!exists) {
            throw ProjectNotFoundException("Project not found")
        }
    }

    private fun ProjectChapter.toDto(): ChapterDto {
        return ChapterDto(
            id = this.id.toString(),
            chapterOrder = this.chapterOrder,
            title = this.title,
            createdAt = this.createdAt
        )
    }

    private fun ProjectChapter.toDetailDto(): ChapterDetailDto {
        return ChapterDetailDto(
            id = this.id.toString(),
            chapterOrder = this.chapterOrder,
            title = this.title,
            text = this.text,
            createdAt = this.createdAt
        )
    }
}
