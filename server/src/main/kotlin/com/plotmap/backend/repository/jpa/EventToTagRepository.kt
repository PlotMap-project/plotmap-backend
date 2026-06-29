package com.plotmap.backend.repository.jpa

import com.plotmap.backend.model.entity.EventToTag
import com.plotmap.backend.model.entity.EventToTagId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EventToTagRepository : JpaRepository<EventToTag, EventToTagId> {

    fun findAllByEventId(eventId: UUID): List<EventToTag>

    fun findAllByProjectId(projectId: UUID): List<EventToTag>

    fun deleteAllByProjectIdAndEventId(projectId: UUID, eventId: UUID)

    fun deleteByProjectIdAndEventIdAndTagId(projectId: UUID, eventId: UUID, tagId: UUID)

    fun deleteAllByProjectId(projectId: UUID)
}
