package com.plotmap.backend.service

import com.plotmap.backend.dto.request.CreateEventRequest
import com.plotmap.backend.dto.request.UpdateEventRequest
import com.plotmap.backend.dto.response.EventDto
import com.plotmap.backend.exception.ProjectNotFoundException
import com.plotmap.backend.model.entity.Event
import com.plotmap.backend.model.entity.EventToCharacter
import com.plotmap.backend.model.entity.EventToTag
import com.plotmap.backend.model.entity.StoryArcToEvent
import com.plotmap.backend.model.enum.EventSource
import com.plotmap.backend.model.enum.EventStatus
import com.plotmap.backend.model.enum.ProjectType
import com.plotmap.backend.model.enum.SystemEventRole
import com.plotmap.backend.repository.jpa.CharacterRepository
import com.plotmap.backend.repository.jpa.EventRepository
import com.plotmap.backend.repository.jpa.EventToCharacterRepository
import com.plotmap.backend.repository.jpa.EventToTagRepository
import com.plotmap.backend.repository.jpa.ProjectRepository
import com.plotmap.backend.repository.jpa.StoryArcRepository
import com.plotmap.backend.repository.jpa.StoryArcToEventRepository
import com.plotmap.backend.repository.jpa.TagRepository
import com.plotmap.backend.repository.jpa.UserToProjectRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class EventService(
    private val projectRepository: ProjectRepository,
    private val userToProjectRepository: UserToProjectRepository,
    private val eventRepository: EventRepository,
    private val characterRepository: CharacterRepository,
    private val storyArcRepository: StoryArcRepository,
    private val tagRepository: TagRepository,
    private val eventToCharacterRepository: EventToCharacterRepository,
    private val storyArcToEventRepository: StoryArcToEventRepository,
    private val eventToTagRepository: EventToTagRepository
) {

    @Transactional
    fun createEvent(
        userId: UUID,
        projectId: UUID,
        request: CreateEventRequest
    ): EventDto {
        ensureUserHasAccessToProject(userId, projectId)
        val projectType = getProjectType(projectId)

        if (projectType != ProjectType.MANUAL) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Events can be created manually only in MANUAL projects"
            )
        }

        require(request.title.isNotBlank()) { "Title must not be blank" }

        val event = eventRepository.save(
            Event(
                projectId = projectId,
                title = request.title.trim(),
                description = request.description.trim(),
                suggestedSystemRole = parseSystemRole(request.suggestedSystemRole),
                impactLevel = request.impactLevel.coerceIn(1, 10),
                status = parseEventStatus(request.status),
                userNotes = request.userNotes.trim(),
                level = request.level.coerceAtLeast(0),
                orderInLevel = request.orderInLevel.coerceAtLeast(0),
                customPositionX = request.customPositionX,
                customPositionY = request.customPositionY,
                color = normalizeColor(request.color),
                source = EventSource.USER_CREATED,
                sourceContext = ""
            )
        )

        replaceCharacters(
            projectId = projectId,
            eventId = event.id,
            characterIds = request.characterIds
        )

        replaceStoryArcs(
            projectId = projectId,
            eventId = event.id,
            storyArcIds = request.storyArcIds
        )

        replaceTags(
            projectId = projectId,
            eventId = event.id,
            tagIds = request.tagIds
        )

        return mapEventToDto(eventRepository.findByIdAndProjectId(event.id, projectId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found after creation"))
    }

    @Transactional
    fun updateEvent(
        userId: UUID,
        projectId: UUID,
        eventId: UUID,
        request: UpdateEventRequest
    ): EventDto {
        ensureUserHasAccessToProject(userId, projectId)
        val projectType = getProjectType(projectId)

        val event = eventRepository.findByIdAndProjectId(eventId, projectId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Event $eventId not found")

        if (projectType == ProjectType.AI_GENERATED) {
            validateAiEventUpdateRequest(request)

            request.userNotes?.let { event.userNotes = it.trim() }
            request.color?.let { event.color = normalizeColor(it) }

            event.updatedAt = Instant.now()
            val saved = eventRepository.save(event)
            return mapEventToDto(saved)
        }

        request.title?.let {
            require(it.isNotBlank()) { "Title must not be blank" }
            event.title = it.trim()
        }

        request.description?.let {
            event.description = it.trim()
        }

        request.suggestedSystemRole?.let {
            event.suggestedSystemRole = parseSystemRole(it)
        }

        request.impactLevel?.let {
            event.impactLevel = it.coerceIn(1, 10)
        }

        request.status?.let {
            event.status = parseEventStatus(it)
        }

        request.userNotes?.let {
            event.userNotes = it.trim()
        }

        request.level?.let {
            event.level = it.coerceAtLeast(0)
        }

        request.orderInLevel?.let {
            event.orderInLevel = it.coerceAtLeast(0)
        }

        request.customPositionX?.let {
            event.customPositionX = it
        }

        request.customPositionY?.let {
            event.customPositionY = it
        }

        request.color?.let {
            event.color = normalizeColor(it)
        }

        event.updatedAt = Instant.now()
        val savedEvent = eventRepository.save(event)

        request.characterIds?.let {
            replaceCharacters(projectId, eventId, it)
        }

        request.storyArcIds?.let {
            replaceStoryArcs(projectId, eventId, it)
        }

        request.tagIds?.let {
            replaceTags(projectId, eventId, it)
        }

        return mapEventToDto(savedEvent)
    }

    @Transactional
    fun deleteEvent(
        userId: UUID,
        projectId: UUID,
        eventId: UUID
    ) {
        ensureUserHasAccessToProject(userId, projectId)
        val projectType = getProjectType(projectId)

        if (projectType != ProjectType.MANUAL) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Events can be deleted manually only in MANUAL projects"
            )
        }

        val event = eventRepository.findByIdAndProjectId(eventId, projectId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Event $eventId not found")

        eventToCharacterRepository.deleteAllByIdProjectAndIdEvent(projectId, eventId)
        storyArcToEventRepository.deleteAllByIdProjectAndIdEvent(projectId, eventId)
        eventToTagRepository.deleteAllByIdProjectAndIdEvent(projectId, eventId)

        eventRepository.delete(event)
    }

    private fun ensureUserHasAccessToProject(userId: UUID, projectId: UUID) {
        val exists = userToProjectRepository.existsByIdUserAndIdProject(userId, projectId)
        if (!exists) {
            throw ProjectNotFoundException("Project $projectId not found")
        }
    }

    private fun getProjectType(projectId: UUID): ProjectType {
        return projectRepository.findById(projectId)
            .orElseThrow { ProjectNotFoundException("Project $projectId not found") }
            .type
    }

    private fun validateAiEventUpdateRequest(request: UpdateEventRequest) {
        val hasForbiddenChanges =
            request.title != null ||
                    request.description != null ||
                    request.suggestedSystemRole != null ||
                    request.impactLevel != null ||
                    request.status != null ||
                    request.level != null ||
                    request.orderInLevel != null ||
                    request.customPositionX != null ||
                    request.customPositionY != null ||
                    request.characterIds != null ||
                    request.storyArcIds != null ||
                    request.tagIds != null

        if (hasForbiddenChanges) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Only color and userNotes can be updated for AI-generated events"
            )
        }
    }

    private fun replaceCharacters(
        projectId: UUID,
        eventId: UUID,
        characterIds: List<String>
    ) {
        val parsedIds = characterIds
            .map { UUID.fromString(it) }
            .distinct()

        parsedIds.forEach { characterId ->
            val exists = characterRepository.existsByIdAndProjectId(characterId, projectId)
            if (!exists) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Character $characterId does not belong to project $projectId"
                )
            }
        }

        eventToCharacterRepository.deleteAllByIdProjectAndIdEvent(projectId, eventId)

        val links = parsedIds.map { characterId ->
            EventToCharacter(
                idProject = projectId,
                idEvent = eventId,
                idCharacter = characterId
            )
        }

        eventToCharacterRepository.saveAll(links)
    }

    private fun replaceStoryArcs(
        projectId: UUID,
        eventId: UUID,
        storyArcIds: List<String>
    ) {
        val parsedIds = storyArcIds
            .map { UUID.fromString(it) }
            .distinct()

        parsedIds.forEach { arcId ->
            val exists = storyArcRepository.existsByIdAndProjectId(arcId, projectId)
            if (!exists) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Story arc $arcId does not belong to project $projectId"
                )
            }
        }

        storyArcToEventRepository.deleteAllByIdProjectAndIdEvent(projectId, eventId)

        val links = parsedIds.map { arcId ->
            StoryArcToEvent(
                idProject = projectId,
                idArc = arcId,
                idEvent = eventId
            )
        }

        storyArcToEventRepository.saveAll(links)
    }

    private fun replaceTags(
        projectId: UUID,
        eventId: UUID,
        tagIds: List<String>
    ) {
        val parsedIds = tagIds
            .map { UUID.fromString(it) }
            .distinct()

        parsedIds.forEach { tagId ->
            val exists = tagRepository.existsByIdAndProjectId(tagId, projectId)
            if (!exists) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tag $tagId does not belong to project $projectId"
                )
            }
        }

        eventToTagRepository.deleteAllByIdProjectAndIdEvent(projectId, eventId)

        val links = parsedIds.map { tagId ->
            EventToTag(
                idProject = projectId,
                idEvent = eventId,
                idTag = tagId
            )
        }

        eventToTagRepository.saveAll(links)
    }

    private fun mapEventToDto(event: Event): EventDto {
        val characterIds = eventToCharacterRepository.findAllByIdEvent(event.id)
            .map { it.idCharacter.toString() }

        val storyArcIds = storyArcToEventRepository.findAllByIdEvent(event.id)
            .map { it.idArc.toString() }

        val tagIds = eventToTagRepository.findAllByIdEvent(event.id)
            .map { it.idTag.toString() }

        return EventDto(
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
            characterIds = characterIds,
            storyArcIds = storyArcIds,
            tagIds = tagIds,
            createdAt = event.createdAt
        )
    }

    private fun parseSystemRole(value: String): SystemEventRole {
        return try {
            SystemEventRole.valueOf(value.trim())
        } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unknown suggestedSystemRole: $value"
            )
        }
    }

    private fun parseEventStatus(value: String): EventStatus {
        return try {
            EventStatus.valueOf(value.trim())
        } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unknown event status: $value"
            )
        }
    }

    private fun normalizeColor(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val trimmed = value.trim()

        if (!Regex("^#[0-9A-Fa-f]{6}$").matches(trimmed)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid color format: $value"
            )
        }

        return trimmed
    }
}
