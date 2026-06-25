package com.plotmap.backend.service

import com.plotmap.backend.dto.request.CreateTagRequest
import com.plotmap.backend.dto.response.TagDto
import com.plotmap.backend.exception.ProjectNotFoundException
import com.plotmap.backend.model.entity.EventToTag
import com.plotmap.backend.model.entity.Tag
import com.plotmap.backend.repository.jpa.EventRepository
import com.plotmap.backend.repository.jpa.EventToTagRepository
import com.plotmap.backend.repository.jpa.TagRepository
import com.plotmap.backend.repository.jpa.UserToProjectRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class TagService(
    private val userToProjectRepository: UserToProjectRepository,
    private val tagRepository: TagRepository,
    private val eventRepository: EventRepository,
    private val eventToTagRepository: EventToTagRepository
) {

    @Transactional
    fun createTag(
        userId: UUID,
        projectId: UUID,
        request: CreateTagRequest
    ): TagDto {
        ensureUserHasAccessToProject(userId, projectId)
        require(request.name.isNotBlank()) { "Tag name must not be blank" }

        val tag = tagRepository.save(
            Tag(
                projectId = projectId,
                name = request.name.trim(),
                color = normalizeColor(request.color)
            )
        )

        return tag.toDto()
    }

    @Transactional
    fun deleteTag(userId: UUID, projectId: UUID, tagId: UUID) {
        ensureUserHasAccessToProject(userId, projectId)

        val tag = tagRepository.findByIdAndProjectId(tagId, projectId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Tag $tagId not found")

        tagRepository.delete(tag)
    }

    @Transactional
    fun assignTagToEvent(
        userId: UUID,
        projectId: UUID,
        eventId: UUID,
        tagId: UUID
    ) {
        ensureUserHasAccessToProject(userId, projectId)

        val event = eventRepository.findByIdAndProjectId(eventId, projectId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Event $eventId not found")

        val tag = tagRepository.findByIdAndProjectId(tagId, projectId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Tag $tagId not found")

        val alreadyExists = eventToTagRepository.findAllByIdEvent(eventId)
            .any { it.idTag == tagId }

        if (!alreadyExists) {
            eventToTagRepository.save(
                EventToTag(
                    idProject = projectId,
                    idEvent = eventId,
                    idTag = tagId
                )
            )
        }
    }

    @Transactional
    fun unassignTagFromEvent(
        userId: UUID,
        projectId: UUID,
        eventId: UUID,
        tagId: UUID
    ) {
        ensureUserHasAccessToProject(userId, projectId)

        eventToTagRepository.deleteByIdProjectAndIdEventAndIdTag(projectId, eventId, tagId)
    }

    private fun ensureUserHasAccessToProject(userId: UUID, projectId: UUID) {
        if (!userToProjectRepository.existsByIdUserAndIdProject(userId, projectId)) {
            throw ProjectNotFoundException("Project $projectId not found")
        }
    }

    private fun normalizeColor(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val trimmed = value.trim()
        if (!Regex("^#[0-9A-Fa-f]{6}$").matches(trimmed)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid color format: $value"
            )
        }
        return trimmed
    }

    private fun Tag.toDto(): TagDto {
        return TagDto(
            id = this.id.toString(),
            name = this.name,
            color = this.color
        )
    }
}
