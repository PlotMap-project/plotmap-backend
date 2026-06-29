package com.plotmap.backend.repository.jpa

import com.plotmap.backend.model.entity.EventEdge
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EventEdgeRepository : JpaRepository<EventEdge, UUID> {

    fun findAllByProjectId(projectId: UUID): List<EventEdge>

    fun findByIdAndProjectId(id: UUID, projectId: UUID): EventEdge?

    fun deleteAllByProjectId(projectId: UUID)
}
