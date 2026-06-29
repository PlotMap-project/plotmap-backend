package com.plotmap.backend.repository.jpa

import com.plotmap.backend.model.entity.Event
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EventRepository : JpaRepository<Event, UUID> {

    fun findAllByProjectIdOrderByLevelAscOrderInLevelAsc(projectId: UUID): List<Event>
    fun deleteAllByProjectId(projectId: UUID)
    fun findByIdAndProjectId(id: UUID, projectId: UUID): Event?
}
