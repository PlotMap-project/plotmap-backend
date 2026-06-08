package com.plotmap.backend.service

import com.plotmap.backend.config.YandexGptProperties
import com.plotmap.backend.dto.request.CreateProjectRequest
import com.plotmap.backend.dto.request.GenerateProjectRequest
import com.plotmap.backend.dto.response.JobStatusResponse
import com.plotmap.backend.dto.response.ProjectDetailResponse
import com.plotmap.backend.dto.response.ProjectResponse
import com.plotmap.backend.entity.Project
import com.plotmap.backend.entity.ProjectType
import com.plotmap.backend.entity.UserToProject
import com.plotmap.backend.exception.ProjectNotFoundException
import com.plotmap.backend.model.enum.GenerationMode
import com.plotmap.backend.repository.jpa.ProjectRepository
import com.plotmap.backend.repository.jpa.UserToProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val userToProjectRepository: UserToProjectRepository,
    private val jobStore: JobStore,
    private val generationJobProcessor: GenerationJobProcessor,
    private val yandexGptProperties: YandexGptProperties
) {

    fun getProjectsByUserId(userId: UUID): List<ProjectResponse> {
        return projectRepository.findAllByUserId(userId).map { project ->
            project.toResponse()
        }
    }

    fun getProjectById(userId: UUID, projectId: UUID): ProjectDetailResponse {
        ensureUserHasAccessToProject(userId, projectId)

        val project = projectRepository.findById(projectId)
            .orElseThrow { ProjectNotFoundException("Project $projectId not found") }

        return ProjectDetailResponse(
            id = project.id.toString(),
            title = project.title,
            type = project.type.name,
            description = project.description,
            sourceText = project.sourceText,
            events = emptyList(),
            connections = emptyList(),
            createdAt = project.createdAt
        )
    }

    @Transactional
    fun createProject(userId: UUID, request: CreateProjectRequest): ProjectResponse {
        val project = Project(
            title = request.title,
            type = ProjectType.user_created,
            description = request.description
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
        require(request.text.isNotBlank()) { "Text must not be empty" }

        if (request.text.length > yandexGptProperties.maxTextLength) {
            throw IllegalArgumentException(
                "Text is too long: ${request.text.length} characters. " +
                        "Maximum allowed: ${yandexGptProperties.maxTextLength}"
            )
        }

        val project = Project(
            title = request.name,
            type = ProjectType.ai_generated,
            description = request.description,
            sourceText = request.text
        )

        val savedProject = projectRepository.save(project)

        userToProjectRepository.save(
            UserToProject(
                idUser = userId,
                idProject = savedProject.id
            )
        )

        val job = jobStore.create(
            projectId = savedProject.id,
            userId = userId,
            sourceText = request.text,
            mode = GenerationMode.FULL
        )

        generationJobProcessor.process(job.id)

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
