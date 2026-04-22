package com.plotmap.backend.service

import com.plotmap.backend.dto.request.CreateProjectRequest
import com.plotmap.backend.dto.request.GenerateProjectRequest
import com.plotmap.backend.dto.response.ProjectDetailResponse
import com.plotmap.backend.dto.response.ProjectResponse
import com.plotmap.backend.entity.Project
import com.plotmap.backend.entity.UserToProject
import com.plotmap.backend.entity.ProjectType
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
        return projectRepository.findAllByUserId(userId).map { it.toResponse() }
    }

    fun getProjectById(userId: UUID, projectId: UUID): ProjectDetailResponse {
        val project = projectRepository.findById(projectId)
            .orElseThrow { ProjectNotFoundException("Project $projectId not found") }

        // Пока генерируем сами а не через ии
        val sourceText = project.sourceText

        val graph = if (sourceText != null) {
            graphGenerationService.generateGraph(
                projectId = project.id,
                request = GenerateProjectRequest(
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
            events = graph?.events ?: emptyList(),
            connections = graph?.connections ?: emptyList(),
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
        val saved = projectRepository.save(project)

        userToProjectRepository.save(
            UserToProject(
                idUser = userId,
                idProject = saved.id
            )
        )

        return saved.toResponse()
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
        val saved = projectRepository.save(project)

        userToProjectRepository.save(
            UserToProject(
                idUser = userId,
                idProject = saved.id
            )
        )

        val graph = graphGenerationService.generateGraph(
            projectId = saved.id,
            request = request
        )

        return ProjectDetailResponse(
            id = saved.id.toString(),
            title = saved.title,
            type = saved.type.name,
            description = saved.description,
            sourceText = saved.sourceText,
            events = graph.events,
            connections = graph.connections,
            createdAt = saved.createdAt
        )
    }

    private fun Project.toResponse() = ProjectResponse(
        id = this.id.toString(),
        title = this.title,
        type = this.type.name,
        description = this.description,
        createdAt = this.createdAt
    )
}
