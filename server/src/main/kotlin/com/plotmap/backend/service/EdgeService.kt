package com.plotmap.backend.service

import com.plotmap.backend.dto.request.CreateEdgeRequest
import com.plotmap.backend.dto.request.UpdateEdgeRequest
import com.plotmap.backend.dto.response.ConnectionDto
import com.plotmap.backend.exception.ProjectNotFoundException
import com.plotmap.backend.model.entity.EventEdge
import com.plotmap.backend.model.enum.ConnectionType
import com.plotmap.backend.model.enum.ProjectType
import com.plotmap.backend.repository.jpa.EventEdgeRepository
import com.plotmap.backend.repository.jpa.EventRepository
import com.plotmap.backend.repository.jpa.ProjectRepository
import com.plotmap.backend.repository.jpa.UserToProjectRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class EdgeService(
    private val projectRepository: ProjectRepository,
    private val userToProjectRepository: UserToProjectRepository,
    private val eventRepository: EventRepository,
    private val eventEdgeRepository: EventEdgeRepository
) {

    @Transactional
    fun createEdge(
        userId: UUID,
        projectId: UUID,
        request: CreateEdgeRequest
    ): ConnectionDto {
        ensureUserHasAccessToProject(userId, projectId)
        ensureManualProject(projectId)

        val sourceEventId = UUID.fromString(request.sourceEventId)
        val targetEventId = UUID.fromString(request.targetEventId)

        if (sourceEventId == targetEventId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Self-loop is not allowed")
        }

        val sourceExists = eventRepository.findByIdAndProjectId(sourceEventId, projectId)
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Source event $sourceEventId not found in project"
            )

        val targetExists = eventRepository.findByIdAndProjectId(targetEventId, projectId)
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Target event $targetEventId not found in project"
            )

        val edge = eventEdgeRepository.save(
            EventEdge(
                idProject = projectId,
                sourceEventId = sourceEventId,
                targetEventId = targetEventId,
                type = parseConnectionType(request.type),
                description = request.description.trim()
            )
        )

        return edge.toDto()
    }

    @Transactional
    fun updateEdge(
        userId: UUID,
        projectId: UUID,
        edgeId: UUID,
        request: UpdateEdgeRequest
    ): ConnectionDto {
        ensureUserHasAccessToProject(userId, projectId)
        ensureManualProject(projectId)

        val edge = eventEdgeRepository.findByIdAndIdProject(edgeId, projectId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Edge $edgeId not found")

        request.type?.let {
            edge.type = parseConnectionType(it)
        }

        request.description?.let {
            edge.description = it.trim()
        }

        edge.updatedAt = Instant.now()
        val saved = eventEdgeRepository.save(edge)
        return saved.toDto()
    }

    @Transactional
    fun deleteEdge(userId: UUID, projectId: UUID, edgeId: UUID) {
        ensureUserHasAccessToProject(userId, projectId)
        ensureManualProject(projectId)

        val edge = eventEdgeRepository.findByIdAndIdProject(edgeId, projectId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Edge $edgeId not found")

        eventEdgeRepository.delete(edge)
    }

    private fun ensureUserHasAccessToProject(userId: UUID, projectId: UUID) {
        if (!userToProjectRepository.existsByIdUserAndIdProject(userId, projectId)) {
            throw ProjectNotFoundException("Project $projectId not found")
        }
    }

    private fun ensureManualProject(projectId: UUID) {
        val project = projectRepository.findById(projectId)
            .orElseThrow { ProjectNotFoundException("Project $projectId not found") }

        if (project.type != ProjectType.MANUAL) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Edges can only be managed in MANUAL projects"
            )
        }
    }

    private fun parseConnectionType(value: String): ConnectionType {
        return try {
            ConnectionType.valueOf(value.trim())
        } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Unknown connection type: $value"
            )
        }
    }

    private fun EventEdge.toDto(): ConnectionDto {
        return ConnectionDto(
            id = this.id.toString(),
            sourceEventId = this.sourceEventId.toString(),
            targetEventId = this.targetEventId.toString(),
            type = this.type.name,
            description = this.description
        )
    }
}
