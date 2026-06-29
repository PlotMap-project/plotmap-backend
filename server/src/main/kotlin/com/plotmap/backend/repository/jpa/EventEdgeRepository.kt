package com.plotmap.backend.repository.jpa

import com.plotmap.backend.model.entity.EventEdge
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EventEdgeRepository : JpaRepository<EventEdge, UUID> {

    fun findAllByIdProject(idProject: UUID): List<EventEdge>

    fun findByIdAndIdProject(id: UUID, idProject: UUID): EventEdge?

    fun deleteAllByIdProject(idProject: UUID)

}
