package com.plotmap.backend.service

import com.plotmap.backend.config.YandexGptProperties
import com.plotmap.backend.dto.request.CreateProjectRequest
import com.plotmap.backend.dto.request.GenerateProjectRequest
import com.plotmap.backend.dto.response.GeneratedConnectionDto
import com.plotmap.backend.dto.response.GeneratedEventDto
import com.plotmap.backend.dto.response.JobStatusResponse
import com.plotmap.backend.dto.response.ProjectDetailResponse
import com.plotmap.backend.dto.response.ProjectResponse
import com.plotmap.backend.exception.ProjectNotFoundException
import com.plotmap.backend.model.entity.GenerationJob
import com.plotmap.backend.model.entity.Project
import com.plotmap.backend.model.entity.ProjectChapter
import com.plotmap.backend.model.entity.UserToProject
import com.plotmap.backend.model.enum.GenerationMode
import com.plotmap.backend.model.enum.JobStatus
import com.plotmap.backend.model.enum.ProjectType
import com.plotmap.backend.repository.jpa.EventEdgeRepository
import com.plotmap.backend.repository.jpa.EventRepository
import com.plotmap.backend.repository.jpa.GenerationJobRepository
import com.plotmap.backend.repository.jpa.ProjectChapterRepository
import com.plotmap.backend.repository.jpa.ProjectRepository
import com.plotmap.backend.repository.jpa.UserToProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.UUID

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val userToProjectRepository: UserToProjectRepository,
    private val projectChapterRepository: ProjectChapterRepository,
    private val generationJobRepository: GenerationJobRepository,
    private val eventRepository: EventRepository,
    private val eventEdgeRepository: EventEdgeRepository,
    private val generationJobProcessor: GenerationJobProcessor,
    private val yandexGptProperties: YandexGptProperties
) {

    fun getProjectsByUserId(userId: UUID): List<ProjectResponse> {
        return projectRepository.findAllByUserId(userId).map { it.toResponse() }
    }

    fun getProjectById(userId: UUID, projectId: UUID): ProjectDetailResponse {
        ensureUserHasAccessToProject(userId, projectId)

        val project = projectRepository.findById(projectId)
            .orElseThrow { ProjectNotFoundException("Project $projectId not found") }

        val events = eventRepository.findAllByProjectIdOrderByLevelAscOrderInLevelAsc(projectId)
            .map {
                GeneratedEventDto(
                    id = it.id.toString(),
                    title = it.title,
                    description = it.description,
                    impactLevel = it.impactLevel,
                    level = it.level,
                    orderInLevel = it.orderInLevel
                )
            }

        val connections = eventEdgeRepository.findAllByIdProject(projectId)
            .map {
                GeneratedConnectionDto(
                    sourceEventId = it.sourceEventId.toString(),
                    targetEventId = it.targetEventId.toString(),
                    type = it.type.name
                )
            }

        return ProjectDetailResponse(
            id = project.id.toString(),
            title = project.title,
            type = project.type.name,
            description = project.description,
            events = events,
            connections = connections,
            createdAt = project.createdAt
        )
    }

    @Transactional
    fun createProject(userId: UUID, request: CreateProjectRequest): ProjectResponse {
        require(request.title.isNotBlank()) { "Title must not be blank" }

        val project = Project(
            title = request.title.trim(),
            type = ProjectType.MANUAL,
            description = request.description.trim()
        )

        val savedProject = projectRepository.save(project)

        userToProjectRepository.save(
            UserToProject(
                idUser = userId,
                idProject = savedProject.id
            )
        )

        return savedProject.toResponse()
    }

    @Transactional
    fun createProjectWithGeneration(
        userId: UUID,
        request: GenerateProjectRequest
    ): JobStatusResponse {
        require(request.name.isNotBlank()) { "Project name must not be blank" }
        require(request.text.isNotBlank()) { "Text must not be empty" }

        if (request.text.length > yandexGptProperties.maxTextLength) {
            throw IllegalArgumentException(
                "Text is too long: ${request.text.length} characters. " +
                        "Maximum allowed: ${yandexGptProperties.maxTextLength}"
            )
        }

        val project = Project(
            title = request.name.trim(),
            type = ProjectType.AI_GENERATED,
            description = request.description.trim()
        )

        val savedProject = projectRepository.save(project)

        userToProjectRepository.save(
            UserToProject(
                idUser = userId,
                idProject = savedProject.id
            )
        )

        val chapter = projectChapterRepository.save(
            ProjectChapter(
                projectId = savedProject.id,
                chapterOrder = 1,
                title = "Chapter 1",
                text = request.text
            )
        )

        val job = generationJobRepository.save(
            GenerationJob(
                projectId = savedProject.id,
                chapterId = chapter.id,
                mode = GenerationMode.INITIAL_GENERATION,
                status = JobStatus.PENDING
            )
        )

        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                generationJobProcessor.process(job.id)
            }
        })

        return JobStatusResponse(
            jobId = job.id.toString(),
            projectId = savedProject.id.toString(),
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
            throw ProjectNotFoundException("Project $projectId not found")
        }
    }

    private fun Project.toResponse(): ProjectResponse {
        return ProjectResponse(
            id = this.id.toString(),
            title = this.title,
            type = this.type.name,
            description = this.description,
            createdAt = this.createdAt
        )
    }
}
