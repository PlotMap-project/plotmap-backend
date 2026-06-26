package com.plotmap.backend.service

import com.plotmap.backend.dto.request.CreateStoryArcRequest
import com.plotmap.backend.dto.request.UpdateStoryArcRequest
import com.plotmap.backend.dto.response.StoryArcDto
import com.plotmap.backend.exception.ProjectNotFoundException
import com.plotmap.backend.model.entity.StoryArc
import com.plotmap.backend.repository.jpa.StoryArcRepository
import com.plotmap.backend.repository.jpa.UserToProjectRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class StoryArcService(
    private val userToProjectRepository: UserToProjectRepository,
    private val storyArcRepository: StoryArcRepository
) {

    @Transactional
    fun createStoryArc(
        userId: UUID,
        projectId: UUID,
        request: CreateStoryArcRequest
    ): StoryArcDto {
        ensureUserHasAccessToProject(userId, projectId)
        require(request.title.isNotBlank()) { "Title must not be blank" }

        val arc = storyArcRepository.save(
            StoryArc(
                projectId = projectId,
                title = request.title.trim(),
                description = request.description.trim(),
                color = normalizeColor(request.color)
            )
        )

        return arc.toDto()
    }

    @Transactional
    fun updateStoryArc(
        userId: UUID,
        projectId: UUID,
        arcId: UUID,
        request: UpdateStoryArcRequest
    ): StoryArcDto {
        ensureUserHasAccessToProject(userId, projectId)

        val arc = storyArcRepository.findByIdAndProjectId(arcId, projectId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Story arc $arcId not found")

        request.title?.let {
            require(it.isNotBlank()) { "Title must not be blank" }
            arc.title = it.trim()
        }

        request.description?.let {
            arc.description = it.trim()
        }

        request.color?.let {
            arc.color = normalizeColor(it)
        }

        arc.updatedAt = Instant.now()
        val saved = storyArcRepository.save(arc)
        return saved.toDto()
    }

    @Transactional
    fun deleteStoryArc(userId: UUID, projectId: UUID, arcId: UUID) {
        ensureUserHasAccessToProject(userId, projectId)

        val arc = storyArcRepository.findByIdAndProjectId(arcId, projectId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Story arc $arcId not found")

        storyArcRepository.delete(arc)
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

    private fun StoryArc.toDto(): StoryArcDto {
        return StoryArcDto(
            id = this.id.toString(),
            title = this.title,
            description = this.description,
            color = this.color
        )
    }
}
