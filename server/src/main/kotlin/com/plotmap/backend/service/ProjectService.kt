package com.plotmap.backend.service

import com.plotmap.backend.dto.request.CreateProjectRequest
import com.plotmap.backend.dto.request.GenerateProjectRequest
import com.plotmap.backend.dto.request.UpdateProjectRequest
import com.plotmap.backend.dto.response.CharacterDto
import com.plotmap.backend.dto.response.ConnectionDto
import com.plotmap.backend.dto.response.EventDto
import com.plotmap.backend.dto.response.JobStatusResponse
import com.plotmap.backend.dto.response.ProjectDetailResponse
import com.plotmap.backend.dto.response.ProjectResponse
import com.plotmap.backend.dto.response.StoryArcDto
import com.plotmap.backend.dto.response.TagDto
import com.plotmap.backend.exception.ProjectNotFoundException
import com.plotmap.backend.model.entity.GenerationJob
import com.plotmap.backend.model.entity.Project
import com.plotmap.backend.model.entity.ProjectChapter
import com.plotmap.backend.model.entity.UserToProject
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
import com.plotmap.backend.repository.jpa.TagRepository
import com.plotmap.backend.repository.jpa.UserToProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.util.UUID

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val userToProjectRepository: UserToProjectRepository,
    private val projectChapterRepository: ProjectChapterRepository,
    private val generationJobRepository: GenerationJobRepository,
    private val eventRepository: EventRepository,
    private val eventEdgeRepository: EventEdgeRepository,
    private val characterRepository: CharacterRepository,
    private val eventToCharacterRepository: EventToCharacterRepository,
    private val storyArcRepository: StoryArcRepository,
    private val storyArcToEventRepository: StoryArcToEventRepository,
    private val tagRepository: TagRepository,
    private val eventToTagRepository: EventToTagRepository,
    private val generationJobProcessor: GenerationJobProcessor
) {

    fun getProjectsByUserId(userId: UUID): List<ProjectResponse> {
        return projectRepository.findAllByUserId(userId).map { it.toResponse() }
    }

    fun getProjectById(userId: UUID, projectId: UUID): ProjectDetailResponse {
        ensureAccess(userId, projectId)

        val project = projectRepository.findById(projectId)
            .orElseThrow { ProjectNotFoundException("Project not found") }

        val events = eventRepository
            .findAllByProjectIdOrderByLevelAscOrderInLevelAsc(projectId)
        val edges = eventEdgeRepository.findAllByIdProject(projectId)
        val characters = characterRepository.findAllByProjectId(projectId)
        val storyArcs = storyArcRepository.findAllByProjectId(projectId)
        val tags = tagRepository.findAllByProjectId(projectId)

        val characterIdsByEvent = eventToCharacterRepository
            .findAllByIdProject(projectId)
            .groupBy { it.idEvent }
            .mapValues { (_, list) -> list.map { it.idCharacter.toString() } }

        val arcIdsByEvent = storyArcToEventRepository
            .findAllByIdProject(projectId)
            .groupBy { it.idEvent }
            .mapValues { (_, list) -> list.map { it.idArc.toString() } }

        val tagIdsByEvent = eventToTagRepository
            .findAllByIdProject(projectId)
            .groupBy { it.idEvent }
            .mapValues { (_, list) -> list.map { it.idTag.toString() } }

        val eventDtos = events.map { event ->
            EventDto(
                id = event.id.toString(),
                title = event.title,
                description = event.description,
                suggestedSystemRole = event.suggestedSystemRole?.name,
                impactLevel = event.impactLevel,
                status = event.status.name,
                userNotes = event.userNotes,
                level = event.level,
                orderInLevel = event.orderInLevel,
                customPositionX = event.customPositionX,
                customPositionY = event.customPositionY,
                color = event.color,
                source = event.source.name,
                sourceContext = event.sourceContext,
                characterIds = characterIdsByEvent[event.id] ?: emptyList(),
                storyArcIds = arcIdsByEvent[event.id] ?: emptyList(),
                tagIds = tagIdsByEvent[event.id] ?: emptyList(),
                createdAt = event.createdAt
            )
        }

        return ProjectDetailResponse(
            id = project.id.toString(),
            title = project.title,
            type = project.type.name,
            description = project.description,
            events = eventDtos,
            connections = edges.map { edge ->
                ConnectionDto(
                    id = edge.id.toString(),
                    sourceEventId = edge.sourceEventId.toString(),
                    targetEventId = edge.targetEventId.toString(),
                    type = edge.type.name,
                    description = edge.description
                )
            },
            characters = characters.map { char ->
                CharacterDto(
                    id = char.id.toString(),
                    name = char.name,
                    description = char.description,
                    role = char.role.name,
                    color = char.color
                )
            },
            storyArcs = storyArcs.map { arc ->
                StoryArcDto(
                    id = arc.id.toString(),
                    title = arc.title,
                    description = arc.description,
                    color = arc.color
                )
            },
            tags = tags.map { tag ->
                TagDto(
                    id = tag.id.toString(),
                    name = tag.name,
                    color = tag.color
                )
            },
            createdAt = project.createdAt
        )
    }

    @Transactional
    fun createProject(userId: UUID, request: CreateProjectRequest): ProjectResponse {
        require(request.title.isNotBlank()) { "Title must not be blank" }

        val project = projectRepository.save(
            Project(
                title = request.title.trim(),
                type = ProjectType.MANUAL,
                description = request.description.trim()
            )
        )

        userToProjectRepository.save(
            UserToProject(idUser = userId, idProject = project.id)
        )

        return project.toResponse()
    }

    @Transactional
    fun createProjectWithGeneration(
        userId: UUID,
        request: GenerateProjectRequest
    ): JobStatusResponse {
        require(request.name.isNotBlank()) { "Project name must not be blank" }
        require(request.text.isNotBlank()) { "Text must not be empty" }

        val project = projectRepository.save(
            Project(
                title = request.name.trim(),
                type = ProjectType.AI_GENERATED,
                description = request.description.trim()
            )
        )

        userToProjectRepository.save(
            UserToProject(idUser = userId, idProject = project.id)
        )

        val chapter = projectChapterRepository.save(
            ProjectChapter(
                projectId = project.id,
                chapterOrder = 1,
                title = "Глава 1",
                text = request.text
            )
        )

        val job = generationJobRepository.save(
            GenerationJob(
                projectId = project.id,
                chapterId = chapter.id,
                mode = GenerationMode.INITIAL_GENERATION,
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

        return job.toResponse()
    }

    @Transactional
    fun updateProject(
        userId: UUID,
        projectId: UUID,
        request: UpdateProjectRequest
    ): ProjectResponse {
        ensureAccess(userId, projectId)

        val project = projectRepository.findById(projectId)
            .orElseThrow { ProjectNotFoundException("Project not found") }

        request.title?.let {
            require(it.isNotBlank()) { "Title must not be blank" }
            project.title = it.trim()
        }
        request.description?.let { project.description = it.trim() }
        project.updatedAt = Instant.now()

        return projectRepository.save(project).toResponse()
    }

    @Transactional
    fun deleteProject(userId: UUID, projectId: UUID) {
        ensureAccess(userId, projectId)

        val project = projectRepository.findById(projectId)
            .orElseThrow { ProjectNotFoundException("Project not found") }

        projectRepository.delete(project)
    }

    private fun ensureAccess(userId: UUID, projectId: UUID) {
        if (!userToProjectRepository.existsByIdUserAndIdProject(userId, projectId)) {
            throw ProjectNotFoundException("Project not found")
        }
    }

    private fun Project.toResponse() = ProjectResponse(
        id = id.toString(),
        title = title,
        type = type.name,
        description = description,
        createdAt = createdAt
    )

    private fun GenerationJob.toResponse() = JobStatusResponse(
        jobId = id.toString(),
        projectId = projectId.toString(),
        status = status.name,
        errorMessage = errorMessage,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
