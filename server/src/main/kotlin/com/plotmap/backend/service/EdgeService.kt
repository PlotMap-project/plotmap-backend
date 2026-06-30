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
    fun createEdge(userId: UUID, projectId: UUID, request: CreateEdgeRequest): ConnectionDto {
        ensureAccess(userId, projectId)
        ensureManualProject(projectId)

        val sourceEventId = UUID.fromString(request.sourceEventId)
        val targetEventId = UUID.fromString(request.targetEventId)

        if (sourceEventId == targetEventId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Self-loop is not allowed")
        }

        eventRepository.findByIdAndProjectId(sourceEventId, projectId)
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Source event not found in project"
            )

        eventRepository.findByIdAndProjectId(targetEventId, projectId)
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Target event not found in project"
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
        ensureAccess(userId, projectId)
        ensureManualProject(projectId)

        val edge = eventEdgeRepository.findByIdAndIdProject(edgeId, projectId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Edge not found")

        request.type?.let { edge.type = parseConnectionType(it) }
        request.description?.let { edge.description = it.trim() }
        edge.updatedAt = Instant.now()

        return eventEdgeRepository.save(edge).toDto()
    }

    @Transactional
    fun deleteEdge(userId: UUID, projectId: UUID, edgeId: UUID) {
        ensureAccess(userId, projectId)
        ensureManualProject(projectId)

        val edge = eventEdgeRepository.findByIdAndIdProject(edgeId, projectId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Edge not found")

        eventEdgeRepository.delete(edge)
    }

    private fun ensureAccess(userId: UUID, projectId: UUID) {
        if (!userToProjectRepository.existsByIdUserAndIdProject(userId, projectId)) {
            throw ProjectNotFoundException("Project not found")
        }
    }

    private fun ensureManualProject(projectId: UUID) {
        val project = projectRepository.findById(projectId)
            .orElseThrow { ProjectNotFoundException("Project not found") }
        if (project.type != ProjectType.MANUAL) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "This operation is only available for MANUAL projects"
            )
        }
    }

    private fun parseConnectionType(value: String): ConnectionType {
        return try {
            ConnectionType.valueOf(value.trim())
        } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unknown connection type: $value"
            )
        }
    }

    private fun EventEdge.toDto() = ConnectionDto(
        id = id.toString(),
        sourceEventId = sourceEventId.toString(),
        targetEventId = targetEventId.toString(),
        type = type.name,
        description = description
    )
}
