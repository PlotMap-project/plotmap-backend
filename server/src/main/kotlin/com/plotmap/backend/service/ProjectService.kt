package com.plotmap.backend.service

import com.plotmap.backend.config.YandexGptProperties
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
import java.util.UUID
import java.time.Instant

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
        val edges = eventEdgeRepository.findAllByIdProject(projectId)
        val characters = characterRepository.findAllByProjectId(projectId)
        val storyArcs = storyArcRepository.findAllByProjectId(projectId)
        val tags = tagRepository.findAllByProjectId(projectId)

        val eventToCharacters = eventToCharacterRepository.findAllByIdProject(projectId)
        val storyArcToEvents = storyArcToEventRepository.findAllByIdProject(projectId)
        val eventToTags = eventToTagRepository.findAllByIdProject(projectId)

        val characterIdsByEvent = eventToCharacters
            .groupBy { it.idEvent }
            .mapValues { (_, list) -> list.map { it.idCharacter.toString() } }

        val arcIdsByEvent = storyArcToEvents
            .groupBy { it.idEvent }
            .mapValues { (_, list) -> list.map { it.idArc.toString() } }

        val tagIdsByEvent = eventToTags
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

        val connectionDtos = edges.map { edge ->
            ConnectionDto(
                id = edge.id.toString(),
                sourceEventId = edge.sourceEventId.toString(),
                targetEventId = edge.targetEventId.toString(),
                type = edge.type.name,
                description = edge.description
            )
        }

        val characterDtos = characters.map { char ->
            CharacterDto(
                id = char.id.toString(),
                name = char.name,
                description = char.description,
                role = char.role.name,
                color = char.color
            )
        }

        val storyArcDtos = storyArcs.map { arc ->
            StoryArcDto(
                id = arc.id.toString(),
                title = arc.title,
                description = arc.description,
                color = arc.color
            )
        }

        val tagDtos = tags.map { tag ->
            TagDto(
                id = tag.id.toString(),
                name = tag.name,
                color = tag.color
            )
        }

        return ProjectDetailResponse(
            id = project.id.toString(),
            title = project.title,
            type = project.type.name,
            description = project.description,
            events = eventDtos,
            connections = connectionDtos,
            characters = characterDtos,
            storyArcs = storyArcDtos,
            tags = tagDtos,
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

    @Transactional
    fun updateProject(userId: UUID, projectId: UUID, request: UpdateProjectRequest): ProjectResponse {
        ensureUserHasAccessToProject(userId, projectId)

        val project = projectRepository.findById(projectId)
            .orElseThrow { ProjectNotFoundException("Project $projectId not found") }

        request.title?.let {
            require(it.isNotBlank()) { "Title must not be blank" }
            project.title = it.trim()
        }

        request.description?.let {
            project.description = it.trim()
        }

        project.updatedAt = Instant.now()

        val saved = projectRepository.save(project)
        return saved.toResponse()
    }

    @Transactional
    fun deleteProject(userId: UUID, projectId: UUID) {
        ensureUserHasAccessToProject(userId, projectId)

        projectRepository.findById(projectId)
            .orElseThrow { ProjectNotFoundException("Project $projectId not found") }

        projectRepository.deleteById(projectId)
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
