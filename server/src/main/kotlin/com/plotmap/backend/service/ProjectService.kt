package com.plotmap.backend.service

import com.plotmap.backend.dto.request.CreateProjectRequest
import com.plotmap.backend.dto.request.GenerateProjectRequest
import com.plotmap.backend.dto.response.ProjectDetailResponse
import com.plotmap.backend.dto.response.ProjectResponse
import com.plotmap.backend.entity.Project
import com.plotmap.backend.entity.ProjectType
import com.plotmap.backend.entity.UserToProject
import com.plotmap.backend.exception.ProjectNotFoundException
import com.plotmap.backend.repository.ProjectRepository
import com.plotmap.backend.repository.UserToProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val userToProjectRepository: UserToProjectRepository,
    private val graphGenerationService: GraphGenerationService
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

        val sourceText = project.sourceText

        val generatedGraph = if (sourceText != null) {
            graphGenerationService.generateProject(
                GenerateProjectRequest(
                    name = project.title,
                    description = project.description,
                    text = sourceText
                )
            )
        } else {
            null
        }

        return ProjectDetailResponse(
            id = project.id.toString(),
            title = project.title,
            type = project.type.name,
            description = project.description,
            sourceText = project.sourceText,
            events = generatedGraph?.events ?: emptyList(),
            connections = generatedGraph?.connections ?: emptyList(),
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
    ): ProjectDetailResponse {
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

        val generatedProject = graphGenerationService.generateProject(request)

        return ProjectDetailResponse(
            id = savedProject.id.toString(),
            title = savedProject.title,
            type = savedProject.type.name,
            description = savedProject.description,
            sourceText = savedProject.sourceText,
            events = generatedProject.events,
            connections = generatedProject.connections,
            createdAt = savedProject.createdAt
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
